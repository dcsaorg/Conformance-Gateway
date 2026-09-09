# Specifications

Practical conformance documentation lives here.

## Structure

```text
specifications/
  confluence-sync/
	<standard>/
	  <version>/
		<page>.md
		excel/
		  <page-slug>/
			*.xlsx
			*.xls
  README.md
  SETUP.md
```

## How content is produced

- Markdown scenario/validation docs are pulled from Confluence pages.
- Excel files attached to those pages are downloaded automatically.
- Runtime schemas stay in each module's `src/main/resources` folder.

## Run sync locally

```bash
python3 -m pip install -r scripts/src/confluence/requirements.txt
python3 scripts/src/confluence/sync-confluence.py --self-test
python3 scripts/src/confluence/sync-confluence.py
```

## Configure standards and versions

Edit `scripts/src/confluence/confluence-config.json` using:

- `standards.<standard>.enabled`
- `standards.<standard>.versions.<version>.enabled`
- `standards.<standard>.versions.<version>.pages[]`

Each page supports:

- `id` (numeric id or full Confluence URL)
- `name`
- `slug` (used in markdown and excel folder names)
- `enabled`
- `download_excel_attachments`

See `specifications/SETUP.md` for full setup details.
