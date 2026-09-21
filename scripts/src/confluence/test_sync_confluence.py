import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from urllib.parse import quote


SCRIPT_PATH = Path(__file__).with_name("sync-confluence.py")
SPEC = importlib.util.spec_from_file_location("sync_confluence", SCRIPT_PATH)
sync_confluence = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = sync_confluence
SPEC.loader.exec_module(sync_confluence)


class FakeClient:
    def __init__(self, attachments, downloads):
        self.attachments = attachments
        self.downloads = downloads

    def list_attachments(self, _page_id):
        return self.attachments

    def download_file(self, path):
        return self.downloads.get(path)


class ExcelSyncTest(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp_dir.cleanup)
        self.page_spec = sync_confluence.PageSpec(
            standard="booking",
            version="2.0.4",
            page_id="123",
            name="Booking",
            output_path="booking/2.0.4/page.md",
            page_slug="page",
            enabled=True,
            download_excel_attachments=True,
        )
        cfg = SimpleNamespace(
            base_url="https://example.invalid",
            username="user",
            api_token="token",
            output_dir=self.temp_dir.name,
            index_file="INDEX.md",
            standard_versions={"booking": ["2.0.4"]},
            pages=[self.page_spec],
        )
        self.manager = sync_confluence.SyncManager(cfg)

    @property
    def excel_dir(self):
        return Path(self.temp_dir.name) / "booking" / "2.0.4" / "excel"

    @staticmethod
    def page(*names):
        links = "".join(
            f'<a href="/wiki/download/attachments/123/{quote(name)}?version=1">file</a>'
            for name in names
        )
        return {"body": {"view": {"value": links}}}

    @staticmethod
    def attachment(name, path):
        return {"title": name, "_links": {"download": path}}

    def test_downloads_every_linked_excel_and_reports_added(self):
        names = ["Carrier.xlsx", "Shipper.xls", "Validation Matrix.xlsx"]
        attachments = [self.attachment(name, f"/{index}") for index, name in enumerate(names)]
        downloads = {f"/{index}": name.encode() for index, name in enumerate(names)}
        self.manager.client = FakeClient(attachments, downloads)

        self.assertTrue(self.manager._download_excels(self.page_spec, self.page(*names)))

        self.assertEqual(set(names), {path.name for path in self.excel_dir.iterdir()})
        self.assertEqual(3, self.manager.excel_stats["added"])

    def test_overwrites_changed_file_and_leaves_unchanged_file_alone(self):
        changed = "Changed.xlsx"
        unchanged = "Unchanged.xlsx"
        self.excel_dir.mkdir(parents=True)
        (self.excel_dir / changed).write_bytes(b"old")
        (self.excel_dir / unchanged).write_bytes(b"same")
        attachments = [self.attachment(changed, "/changed"), self.attachment(unchanged, "/unchanged")]
        self.manager.client = FakeClient(attachments, {"/changed": b"new", "/unchanged": b"same"})

        self.assertTrue(self.manager._download_excels(self.page_spec, self.page(changed, unchanged)))

        self.assertEqual(b"new", (self.excel_dir / changed).read_bytes())
        self.assertEqual(1, self.manager.excel_stats["updated"])
        self.assertEqual(1, self.manager.excel_stats["unchanged"])

    def test_failed_download_preserves_previous_file_and_disables_cleanup(self):
        name = "Validations.xlsx"
        self.excel_dir.mkdir(parents=True)
        file_path = self.excel_dir / name
        file_path.write_bytes(b"previous")
        self.manager.client = FakeClient([self.attachment(name, "/failed")], {})

        self.assertFalse(self.manager._download_excels(self.page_spec, self.page(name)))
        self.manager._cleanup_excel_outputs(None)

        self.assertEqual(b"previous", file_path.read_bytes())
        self.assertEqual(1, self.manager.excel_stats["failed"])
        self.assertIn(("booking", "2.0.4"), self.manager.failed_excel_buckets)

    def test_missing_referenced_attachment_fails_without_deleting_existing_files(self):
        self.excel_dir.mkdir(parents=True)
        existing = self.excel_dir / "Previous.xlsx"
        existing.write_bytes(b"previous")
        self.manager.client = FakeClient([], {})

        # An empty successful attachment response must reveal a broken page reference.
        self.assertFalse(self.manager._download_excels(self.page_spec, self.page("Missing.xlsx")))
        self.manager._cleanup_excel_outputs(None)

        self.assertTrue(existing.exists())


if __name__ == "__main__":
    unittest.main()
