# 🔐 Automated Confluence Sync - Setup Guide

This document explains how to configure automated Confluence syncing to keep specifications current.

## ⚡ Quick Start

1. Generate Confluence API token (5 min)
2. Store in GitHub Secrets (2 min)
3. GitHub Actions will auto-sync weekly

## 📋 Step-by-Step Setup

### Step 1: Generate Confluence API Token

**Location**: Atlassian Account Settings

1. Go to: https://id.atlassian.com/manage-profile/security/api-tokens
2. Click **"Create API token"**
3. Label it: `DCSA-Conformance-Gateway-Sync`
4. **Copy the token immediately** (you can only see it once!)

**Example token format**:

```
ATAT3xFfGH4o0L8mN9pQ2rStUvWxYzAbCdEfGhIjKlMnOpQrStUvWxYzAbCd1234
```

⚠️ **Security Note**: This token has read-only access to Confluence. Never commit it to git or share publicly.

---

### Step 2: Store in GitHub Repository Secrets

**Requires**: Admin access to https://github.com/dcsaorg/Conformance-Gateway

1. Go to: **GitHub Repo** → **Settings** → **Secrets and variables** → **Actions**
2. Click **"New repository secret"**
3. Create **3 secrets**:

| Secret Name            | Value                          |
|------------------------|--------------------------------|
| `CONFLUENCE_USERNAME`  | `pedro.carvalho@dcsa.org`      |
| `CONFLUENCE_API_TOKEN` | `[Your API token from Step 1]` |
| `CONFLUENCE_BASE_URL`  | `https://dcsa.atlassian.net`   |

**Example Screenshot Path**: Settings → Secrets and variables → Actions → New secret

---

### Step 3: Verify GitHub Actions Workflow

The workflow file is created at: `.github/workflows/confluence-sync.yml`

**Automated Schedule**:

- Runs every **Monday at 02:00 UTC** (configurable)
- Exports Confluence pages → Markdown
- Commits changes to `confluence-sync/` folder
- Creates PR if you prefer manual review

**Check Status**:

1. Go to: **GitHub Repo** → **Actions**
2. Find: "Confluence Sync" workflow
3. View latest run logs

---

## 🛠️ Manual Sync (Optional)

If you want to sync outside the scheduled run:

### Local Sync via Python Script

**Prerequisites**:

```bash
python3 -m pip install -r scripts/src/confluence/requirements.txt
```

**Setup**:

1. Create `.env` file in project root:

```bash
CONFLUENCE_USERNAME=pedro.carvalho@dcsa.org
CONFLUENCE_API_TOKEN=your_token_here
CONFLUENCE_BASE_URL=https://dcsa.atlassian.net
CONFLUENCE_SPACE_KEY=SD
```

2. Run local converter self-test and then sync:

```bash
python3 scripts/src/confluence/sync-confluence.py --self-test
python3 scripts/src/confluence/sync-confluence.py
```

**Output**:

- Markdown: `specifications/confluence-sync/<standard>/<version>/<page>.md`
- Excel attachments: `specifications/confluence-sync/<standard>/<version>/excel/*.xlsx`

---

## 🔍 Configuration Details

### Confluence Space & Pages

**Current Space**: `SD` (Standards Development)

- Base URL: https://dcsa.atlassian.net/wiki/spaces/SD/

**Pages Being Synced**:
See `scripts/src/confluence/confluence-config.json`.

**Recommended config shape**:

```json
{
  "output_dir": "specifications/confluence-sync",
  "standards": {
    "booking": {
      "enabled": true,
      "versions": {
        "2.x": {
          "enabled": true,
          "pages": [
            {
              "id": "1601798776",
              "name": "Booking Conformance Scenarios",
              "slug": "conformance-scenarios",
              "enabled": true,
              "download_excel_attachments": true
            }
          ]
        }
      }
    }
  }
}
```

You can provide `id` as a numeric page id or a full URL containing `/pages/{id}`.

**To Add More Pages**:

1. Find page in Confluence
2. Get page ID from URL: `...pages/704151560...` -> ID is `704151560`
3. Add to `scripts/src/confluence/confluence-config.json` under the matching standard:

```json
{
  "standards": {
    "booking": {
      "versions": {
        "latest": {
          "enabled": true,
          "pages": [
            {
              "id": "704151560",
              "name": "Booking API Specification",
              "slug": "booking-api-spec",
              "enabled": true,
              "download_excel_attachments": true
            }
          ]
        }
      }
    }
  }
}
```

4. Re-run sync or wait for weekly automated run

---

## 🚨 Troubleshooting

### Issue: "401 Unauthorized" in GitHub Actions

**Cause**: API token expired or incorrect **Fix**:

1. Generate new token (Step 1)
2. Update GitHub secret (Step 2)
3. Trigger workflow manually

### Issue: "404 Page Not Found"

**Cause**: Confluence page ID changed or was deleted **Fix**:

1. Find correct page ID in Confluence URL
2. Update `scripts/src/confluence/confluence-config.json`

### Issue: Files not appearing in `confluence-sync/`

**Cause**: Confluence API permissions **Fix**:

- Verify your Confluence account has read access to the SD space
- Check GitHub Actions logs for error messages

**View Logs**:

1. GitHub Repo → Actions → Confluence Sync (latest run)
2. Click "Run confluence-sync.py"
3. Scroll to see error output

---

## 📊 What Gets Synced

**Weekly Export Includes**:

- ✅ Confluence pages (as markdown)
- ✅ Embedded tables and code blocks
- ✅ Page hierarchy and navigation
- ✅ Links to other pages

**NOT Included**:

- ❌ Attachments (use `CONFLUENCE_SOURCES.md` links instead)
- ❌ Comments/History
- ❌ Permissions (public pages only)

---

## 🔄 Sync Workflow Overview

```
Monday 02:00 UTC
    ↓
GitHub Actions triggers
    ↓
Authenticate with Confluence API
    ↓
Fetch pages from SD space
    ↓
Convert to Markdown
    ↓
Write to specifications/confluence-sync/
    ↓
Create git commit & push
    ↓
(Optional: Create PR for review)
    ↓
Development team pulls latest
```

---

## 🔐 Token Security Checklist

- [ ] Token is 64+ characters
- [ ] Token is stored ONLY in GitHub Secrets (not in code/files)
- [ ] `.env` file is in `.gitignore` (for local testing)
- [ ] GitHub Secrets are NOT visible in logs
- [ ] Token has minimal required permissions (read-only)
- [ ] Rotate token quarterly

**If Token is Compromised**:

1. Delete token immediately: https://id.atlassian.com/manage-profile/security/api-tokens
2. Create new token
3. Update GitHub Secrets
4. No need to rotate other tokens (API token is isolated)

---

## 📞 Support

- **Confluence Issues**: Check Confluence server status
- **GitHub Actions**: See workflow logs in Actions tab
- **Python Script Errors**: Check `.env` file and Python version (3.8+)

**Questions?** Contact: [DCSA DevOps Team]

---

## ✅ Verification Checklist

After setup, verify everything works:

```bash
# 1. Check GitHub secrets exist
# Go to: GitHub -> Settings -> Secrets and variables -> Actions
# ✓ CONFLUENCE_USERNAME
# ✓ CONFLUENCE_API_TOKEN
# ✓ CONFLUENCE_BASE_URL

# 2. Check workflow file exists
ls -la .github/workflows/confluence-sync.yml

# 3. (Optional) Test local sync
cat > .env <<'EOF'
CONFLUENCE_USERNAME=your.email@dcsa.org
CONFLUENCE_API_TOKEN=your_token_here
CONFLUENCE_BASE_URL=https://dcsa.atlassian.net
CONFLUENCE_SPACE_KEY=SD
EOF
python3 scripts/src/confluence/sync-confluence.py

# 4. Check git for changes
git status
# Should show new files in specifications/confluence-sync/

# 5. Monitor workflow
# GitHub -> Actions -> Confluence Sync
# Should show successful run
```
