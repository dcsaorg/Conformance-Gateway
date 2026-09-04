# Project Documentation

This folder intentionally stays minimal.

## What to use

- Confluence sync configuration: `scripts/src/confluence/confluence-config.json`
- Local sync script: `scripts/src/confluence/sync-confluence.py`
- Generated Confluence docs: `specifications/confluence-sync/`
- Setup details: `specifications/SETUP.md`

## Local sync

Set environment variables in your local `.env` (gitignored), then run:

```bash
python3 scripts/src/confluence/sync-confluence.py
```

## Notes

- `.env` is local only and should not be committed.
- GitHub Actions uses repository secrets, not local `.env`.
- Deprecated `specifications/domains/` is removed and ignored.

