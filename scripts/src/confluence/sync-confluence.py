#!/usr/bin/env python3
"""Sync Confluence pages to markdown and extract Excel attachments."""

import argparse
from collections import Counter, defaultdict
import json
import logging
import os
import re
import shutil
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from urllib.parse import unquote, urljoin

import requests
from bs4 import BeautifulSoup
from markdownify import markdownify as md

try:
    from dotenv import load_dotenv

    load_dotenv()
except ImportError:
    pass


logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


def slugify(value: str) -> str:
    slug = re.sub(r"[^a-zA-Z0-9]+", "-", value.strip().lower())
    return slug.strip("-") or "page"


def extract_page_id(value: str) -> Optional[str]:
    if not value:
        return None
    if value.isdigit():
        return value
    m = re.search(r"/pages/(\d+)", value)
    return m.group(1) if m else None


@dataclass
class PageSpec:
    standard: str
    version: str
    page_id: str
    name: str
    output_path: str
    page_slug: str
    enabled: bool
    download_excel_attachments: bool


class ConfluenceSyncConfig:
    def __init__(self, config_file: str):
        self.base_url = os.getenv("CONFLUENCE_BASE_URL", "https://dcsa.atlassian.net")
        self.username = os.getenv("CONFLUENCE_USERNAME", "")
        self.api_token = os.getenv("CONFLUENCE_API_TOKEN", "")
        self.output_dir = "specifications/confluence-sync"
        self.index_file = "INDEX.md"
        self.standard_versions: Dict[str, List[str]] = {}
        self.pages: List[PageSpec] = []
        self._load(config_file)

    def _load(self, config_file: str) -> None:
        with open(config_file, "r", encoding="utf-8") as f:
            raw = json.load(f)

        self.output_dir = raw.get("output_dir", self.output_dir)
        self.index_file = raw.get("index_file", self.index_file)

        for standard, standard_cfg in raw.get("standards", {}).items():
            std_enabled = bool(standard_cfg.get("enabled", True))
            versions = standard_cfg.get("versions", {})
            self.standard_versions[standard] = list(versions.keys())
            for version, version_cfg in versions.items():
                ver_enabled = std_enabled and bool(version_cfg.get("enabled", True))
                if not ver_enabled:
                    continue
                pages = version_cfg.get("pages", [])
                for page in pages:
                    pid = extract_page_id(str(page.get("id", "")).strip())
                    if not pid:
                        logger.warning("Skipping page with invalid id in %s/%s: %s", standard, version, page.get("id"))
                        continue
                    name = page.get("name", f"Page {pid}")
                    page_slug = slugify(page.get("slug", name))
                    output_path = page.get("output_path", f"{standard}/{version}/{page_slug}.md")
                    self.pages.append(
                        PageSpec(
                            standard=standard,
                            version=version,
                            page_id=pid,
                            name=name,
                            output_path=output_path,
                            page_slug=page_slug,
                            enabled=ver_enabled and bool(page.get("enabled", True)),
                            download_excel_attachments=bool(page.get("download_excel_attachments", True)),
                        )
                    )

        logger.info("Loaded %d page entries from %s", len(self.pages), config_file)

    def is_valid(self) -> bool:
        if not self.username or not self.api_token:
            logger.error("Missing CONFLUENCE_USERNAME or CONFLUENCE_API_TOKEN")
            return False
        if not self.pages:
            logger.error("No valid page entries configured")
            return False
        return True


class ConfluenceClient:
    def __init__(self, cfg: ConfluenceSyncConfig):
        self.cfg = cfg
        self.session = requests.Session()
        self.session.auth = (cfg.username, cfg.api_token)
        self.session.headers.update({"Accept": "application/json"})

    def get_page(self, page_id: str) -> Optional[Dict]:
        url = urljoin(self.cfg.base_url, f"/wiki/rest/api/content/{page_id}")
        params = {"expand": "body.view,version"}
        try:
            r = self.session.get(url, params=params, timeout=45)
            r.raise_for_status()
            return r.json()
        except requests.RequestException as exc:
            logger.error("Page fetch failed (%s): %s", page_id, exc)
            return None

    def list_attachments(self, page_id: str) -> List[Dict]:
        items: List[Dict] = []
        url = urljoin(self.cfg.base_url, f"/wiki/rest/api/content/{page_id}/child/attachment")
        params = {"limit": 200}
        while url:
            try:
                r = self.session.get(url, params=params, timeout=45)
                r.raise_for_status()
                payload = r.json()
                items.extend(payload.get("results", []))
                next_rel = payload.get("_links", {}).get("next")
                url = urljoin(self.cfg.base_url, next_rel) if next_rel else ""
                params = None
            except requests.RequestException as exc:
                logger.error("Attachment listing failed (%s): %s", page_id, exc)
                return items
        return items

    def download_file(self, download_path: str) -> Optional[bytes]:
        if download_path.startswith("http"):
            url = download_path
        else:
            normalized = download_path
            # Some Confluence responses return /rest/... paths that need the /wiki prefix.
            if normalized.startswith("/rest/"):
                normalized = "/wiki" + normalized
            url = urljoin(self.cfg.base_url, normalized)
        try:
            r = self.session.get(url, timeout=90)
            r.raise_for_status()
            return r.content
        except requests.RequestException as exc:
            logger.error("Attachment download failed (%s): %s", download_path, exc)
            return None


class MarkdownConverter:
    @staticmethod
    def html_to_markdown(html: str) -> str:
        soup = BeautifulSoup(html, "html.parser")
        for tag in soup(["script", "style"]):
            tag.decompose()
        for br in soup.find_all("br"):
            br.replace_with("\n")

        text = md(str(soup), heading_style="ATX", bullets="-", strip=["span", "font"])
        text = re.sub(r"\n{3,}", "\n\n", text)
        return text.strip() + "\n"

    @staticmethod
    def to_page_markdown(page: Dict) -> str:
        title = page.get("title", "Untitled")
        page_id = page.get("id", "")
        ver = page.get("version", {}).get("number", "?")
        html = page.get("body", {}).get("view", {}).get("value", "")
        header = (
            f"# {title}\n\n"
            f"- Confluence page id: `{page_id}`\n"
            f"- Confluence version: `{ver}`\n"
            f"- Synced at: `{datetime.utcnow().isoformat()}Z`\n\n"
        )
        return header + MarkdownConverter.html_to_markdown(html)


class SyncManager:
    def __init__(self, cfg: ConfluenceSyncConfig):
        self.cfg = cfg
        self.client = ConfluenceClient(cfg)
        self.output_root = Path(cfg.output_dir)
        self.output_root.mkdir(parents=True, exist_ok=True)
        self.page_count_by_bucket = Counter(
            (p.standard, p.version) for p in self.cfg.pages if p.enabled
        )
        self.expected_excel_by_bucket = defaultdict(set)

    def ensure_standard_version_folders(self) -> None:
        for standard, versions in self.cfg.standard_versions.items():
            for version in versions:
                (self.output_root / standard / version).mkdir(parents=True, exist_ok=True)

    def sync(self, only_standard: Optional[str] = None) -> Tuple[int, int]:
        self.ensure_standard_version_folders()
        total, ok = 0, 0
        for page in self.cfg.pages:
            if only_standard and page.standard != only_standard:
                continue
            if not page.enabled:
                continue
            total += 1
            if self._sync_page(page):
                ok += 1
        self._cleanup_excel_outputs(only_standard)
        self.write_index(only_standard)
        logger.info("Sync complete: %d/%d pages", ok, total)
        return ok, total

    def _cleanup_excel_outputs(self, only_standard: Optional[str]) -> None:
        buckets = {
            (p.standard, p.version)
            for p in self.cfg.pages
            if p.enabled and (not only_standard or p.standard == only_standard)
        }
        for standard, version in buckets:
            out_dir = self.output_root / standard / version / "excel"
            if not out_dir.exists():
                continue

            expected = self.expected_excel_by_bucket.get((standard, version), set())
            for existing in out_dir.glob("*.xls*"):
                if existing.name not in expected:
                    existing.unlink(missing_ok=True)

            if not any(out_dir.iterdir()):
                out_dir.rmdir()

    def _sync_page(self, page_spec: PageSpec) -> bool:
        page = self.client.get_page(page_spec.page_id)
        if not page:
            return False

        markdown = MarkdownConverter.to_page_markdown(page)
        md_path = self.output_root / page_spec.output_path
        md_path.parent.mkdir(parents=True, exist_ok=True)
        md_path.write_text(markdown, encoding="utf-8")
        logger.info("Wrote markdown: %s", md_path)

        if page_spec.download_excel_attachments:
            self._download_excels(page_spec, page)

        return True

    @staticmethod
    def _extract_referenced_excel_names(page: Dict) -> set:
        html = page.get("body", {}).get("view", {}).get("value", "")
        if not html:
            return set()
        soup = BeautifulSoup(html, "html.parser")
        names = set()
        for anchor in soup.find_all("a", href=True):
            href = anchor.get("href", "")
            if "/download/attachments/" not in href:
                continue
            raw_name = href.rsplit("/", 1)[-1].split("?", 1)[0]
            file_name = unquote(raw_name)
            if file_name.lower().endswith((".xlsx", ".xls")):
                names.add(file_name)
        return names

    @staticmethod
    def _parse_attachment_datetime(att: Dict) -> datetime:
        when = att.get("version", {}).get("when", "")
        if when:
            try:
                return datetime.fromisoformat(when.replace("Z", "+00:00")).replace(tzinfo=None)
            except ValueError:
                pass
        name = att.get("title", "")
        m = re.search(r"(20\d{2})(\d{2})(\d{2})", name)
        if m:
            try:
                return datetime(int(m.group(1)), int(m.group(2)), int(m.group(3)))
            except ValueError:
                pass
        return datetime.min

    @staticmethod
    def _pick_latest_role_files(attachments: List[Dict]) -> List[Dict]:
        role_buckets = {"carrier": [], "shipper": []}
        for att in attachments:
            title = att.get("title", "")
            lower = title.lower()
            if "carrier" in lower:
                role_buckets["carrier"].append(att)
            elif "shipper" in lower:
                role_buckets["shipper"].append(att)

        selected = []
        for role in ("carrier", "shipper"):
            candidates = role_buckets[role]
            if not candidates:
                logger.warning("No %s excel attachment found for current page", role)
                continue
            candidates.sort(key=SyncManager._parse_attachment_datetime, reverse=True)
            selected.append(candidates[0])
        return selected

    def _download_excels(self, page_spec: PageSpec, page: Dict) -> None:
        attachments = self.client.list_attachments(page_spec.page_id)
        if not attachments:
            return

        out_dir = self.output_root / page_spec.standard / page_spec.version / "excel"

        excel_attachments = [
            att
            for att in attachments
            if att.get("title", "").lower().endswith((".xlsx", ".xls"))
        ]

        referenced_names = self._extract_referenced_excel_names(page)
        if not referenced_names:
            logger.info("No excel links referenced in page %s; skipping excel export", page_spec.page_id)
            return

        excel_attachments = [att for att in excel_attachments if att.get("title", "") in referenced_names]

        target_attachments = self._pick_latest_role_files(excel_attachments)
        if not target_attachments:
            return

        out_dir.mkdir(parents=True, exist_ok=True)

        # Backward compatibility cleanup: old layout used excel/<page-slug>/
        legacy_dir = out_dir / page_spec.page_slug
        if legacy_dir.exists() and legacy_dir.is_dir():
            shutil.rmtree(legacy_dir)

        # Backward compatibility cleanup: old layout wrote slug-prefixed files in excel/.
        for legacy_file in out_dir.glob("*.xls*"):
            if "__" in legacy_file.name:
                legacy_file.unlink(missing_ok=True)

        for att in target_attachments:
            name = att.get("title", "")
            download_path = att.get("_links", {}).get("download")
            if not download_path:
                continue
            data = self.client.download_file(download_path)
            if data is None:
                continue
            file_path = out_dir / name
            file_path.write_bytes(data)
            self.expected_excel_by_bucket[(page_spec.standard, page_spec.version)].add(name)
            logger.info("Saved Excel: %s", file_path)

    def write_index(self, only_standard: Optional[str]) -> None:
        lines = ["# Confluence Sync Index", "", f"Generated at `{datetime.utcnow().isoformat()}Z`", ""]
        grouped: Dict[Tuple[str, str], List[PageSpec]] = {}
        for page in self.cfg.pages:
            if not page.enabled:
                continue
            if only_standard and page.standard != only_standard:
                continue
            grouped.setdefault((page.standard, page.version), []).append(page)

        for (standard, version), pages in sorted(grouped.items()):
            lines.append(f"## {standard} / {version}")
            lines.append("")
            for p in pages:
                lines.append(f"- [{p.name}]({p.output_path}) (`{p.page_id}`)")
            lines.append("")

        (self.output_root / self.cfg.index_file).write_text("\n".join(lines).strip() + "\n", encoding="utf-8")


def run_self_test() -> int:
    sample = "<h1>T</h1><p>hello <strong>x</strong></p><table><tr><th>A</th></tr><tr><td>1</td></tr></table>"
    out = MarkdownConverter.html_to_markdown(sample)
    ok = "# T" in out and "**x**" in out and "| A |" in out
    print("SELF-TEST OK" if ok else "SELF-TEST FAILED")
    return 0 if ok else 1


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Confluence to markdown sync")
    p.add_argument("--config", default="scripts/src/confluence/confluence-config.json")
    p.add_argument("--self-test", action="store_true")
    p.add_argument("--standard", default="", help="Sync only one standard key (e.g. booking)")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    if args.self_test:
        return run_self_test()

    cfg = ConfluenceSyncConfig(args.config)
    if not cfg.is_valid():
        return 1

    manager = SyncManager(cfg)
    ok, total = manager.sync(args.standard or None)
    return 0 if total > 0 and ok == total else 1


if __name__ == "__main__":
    sys.exit(main())

