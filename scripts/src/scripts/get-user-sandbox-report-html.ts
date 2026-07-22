/**
 * Script: get-user-sandbox-report-html
 *
 * Queries the Conformance DynamoDB table for the content items of a specific sandbox report
 * and renders them as a self-contained HTML file, matching the Angular report.component view.
 *
 * The table schema (from CDK):
 *   Partition key : PK  (String)
 *   Sort key      : SK  (String)
 *
 * Usage:
 *   npm run get-user-sandbox-report-html -- <userId> <sandboxId> <reportTimestamp>
 *
 * Example:
 *   npm run get-user-sandbox-report-html -- my-user-id my-sandbox-id 2026-03-10T16:34:11.962087591Z
 *
 * Redirect the output to a file:
 *   npm run get-user-sandbox-report-html -- my-user-id my-sandbox-id 2026-03-10T16:34:11.962087591Z > report.html
 */

import { getUserSandboxReportJson } from './get-user-sandbox-report-json';

// ---------------------------------------------------------------------------
// Types (mirrors webui/src/app/model)
// ---------------------------------------------------------------------------

type ConformanceStatus =
  | 'CONFORMANT'
  | 'NON_CONFORMANT'
  | 'PARTIALLY_CONFORMANT'
  | 'COMPLETED_WITHOUT_TRAFFIC'
  | 'SKIPPED'
  | 'NO_TRAFFIC'
  | 'IRRELEVANT';

interface ScenarioConformanceReport {
  title: string;
  status: ConformanceStatus;
  errorMessages: string[];
  subReports: ScenarioConformanceReport[];
}

// ---------------------------------------------------------------------------
// Helpers (mirrors webui/src/app/model/conformance-status.ts)
// ---------------------------------------------------------------------------

function getConformanceStatusEmoji(status: ConformanceStatus): string {
  switch (status) {
    case 'CONFORMANT':           return '✅';
    case 'NON_CONFORMANT':       return '🚫';
    case 'PARTIALLY_CONFORMANT': return '❔';
    case 'COMPLETED_WITHOUT_TRAFFIC': return '✔️';
    case 'SKIPPED':              return '↪️';
    case 'NO_TRAFFIC':           return '❔';
    case 'IRRELEVANT':           return '➖';
  }
}

function getConformanceStatusTitle(status: ConformanceStatus): string {
  switch (status) {
    case 'CONFORMANT':           return 'Conformant';
    case 'NON_CONFORMANT':       return 'Non-conformant';
    case 'PARTIALLY_CONFORMANT': return 'Partially conformant';
    case 'COMPLETED_WITHOUT_TRAFFIC': return 'Completed without optional traffic';
    case 'SKIPPED':              return 'Skipped';
    case 'NO_TRAFFIC':           return 'No traffic';
    case 'IRRELEVANT':           return 'Irrelevant';
  }
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ---------------------------------------------------------------------------
// Recursive renderer (mirrors report.component.html logic)
// ---------------------------------------------------------------------------

function renderReport(report: ScenarioConformanceReport, unfoldedLevels: number): string {
  const emoji = getConformanceStatusEmoji(report.status);
  const title = getConformanceStatusTitle(report.status);
  const hasDetails = report.errorMessages.length > 0 || report.subReports.length > 0;

  if (!hasDetails) {
    return `
      <div style="margin-top:1em">
        <div style="margin-left:1em">
          <span class="conformanceStatus" title="${escapeHtml(title)}">${emoji}</span>
          <span>${escapeHtml(report.title)}</span>
        </div>
      </div>`;
  }

  const openAttr = unfoldedLevels > 0 ? ' open' : '';

  const sortedErrors = [...report.errorMessages].sort();
  const errorLines = sortedErrors
    .map(
      (msg) => `
          <div style="margin-left:2em">
            <span class="conformanceStatus" title="Non-conformant">🚫</span>
            <span>${escapeHtml(msg)}</span>
          </div>`,
    )
    .join('');

  const subReportLines = report.subReports
    .map((sub) => renderReport(sub, unfoldedLevels - 1))
    .join('');

  return `
      <div style="margin-top:1em">
        <details${openAttr}>
          <summary>
            <span class="conformanceStatus" title="${escapeHtml(title)}">${emoji}</span>
            <span>${escapeHtml(report.title)}</span>
          </summary>
          ${errorLines}
          <div style="margin-left:2em">
            ${subReportLines}
          </div>
        </details>
      </div>`;
}

// ---------------------------------------------------------------------------
// HTML page wrapper
// ---------------------------------------------------------------------------

function renderPage(
  userId: string,
  sandboxId: string,
  reportTimestamp: string,
  reports: ScenarioConformanceReport[],
): string {
  const bodyContent = reports.length === 0
    ? '<p>No report content found.</p>'
    : reports.map((r) => renderReport(r, 2)).join('\n');

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Report — ${escapeHtml(sandboxId)} @ ${escapeHtml(reportTimestamp)}</title>
  <style>
    body {
      font-family: sans-serif;
      margin: 1em 2em;
    }
    h1 { font-size: 1.2rem; margin-top: 1em; font-weight: bold; }
    h2 { font-size: 1.1rem; margin-top: 1em; font-weight: bold; }
    .conformanceStatus {
      display: inline-block;
      width: 2em;
    }
    details > summary {
      cursor: pointer;
      display: list-item;
      list-style-position: inside;
    }
  </style>
</head>
<body>
  <h1>Conformance Report</h1>
  <h2>User: ${escapeHtml(userId)}</h2>
  <h2>Sandbox: ${escapeHtml(sandboxId)}</h2>
  <h2>Timestamp: ${escapeHtml(reportTimestamp)}</h2>
  ${bodyContent}
</body>
</html>`;
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

async function main(): Promise<void> {
  const userId = process.argv[2];
  const sandboxId = process.argv[3];
  const reportTimestamp = process.argv[4];

  if (!userId || !sandboxId || !reportTimestamp) {
    console.error(
      'Error: userId, sandboxId and reportTimestamp arguments are required.\n' +
        'Example: npm run get-user-sandbox-report-html -- my-user-id my-sandbox-id 2026-03-10T16:34:11.962087591Z',
    );
    process.exit(1);
  }

  const items = await getUserSandboxReportJson(userId, sandboxId, reportTimestamp);
  const reports = items as ScenarioConformanceReport[];

  process.stdout.write(renderPage(userId, sandboxId, reportTimestamp, reports));
}

if (require.main === module) main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

