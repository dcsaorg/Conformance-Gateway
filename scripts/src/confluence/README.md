# Confluence Sync Script

This script exports selected Confluence pages to markdown and downloads Excel attachments found on those pages.

## Files

- Script: `sync-confluence.py`
- Config: `confluence-config.json`
- Dependencies: `requirements.txt`
- Output: `../../../specifications/confluence-sync/`

## Quick run

```bash
python3 -m pip install -r scripts/src/confluence/requirements.txt
python3 scripts/src/confluence/sync-confluence.py --self-test
python3 scripts/src/confluence/sync-confluence.py
```

## Config model

`confluence-config.json` supports the grouped standards model:

- `standards.<standard>.versions.<version>.pages[]`

Only enabled standards/versions/pages are synced.

## Output layout

- Markdown: `specifications/confluence-sync/<standard>/<version>/<page>.md`
- Excel files: `specifications/confluence-sync/<standard>/<version>/excel/*.xlsx`

## Optional filter

```bash
python3 scripts/src/confluence/sync-confluence.py --standard booking
```

