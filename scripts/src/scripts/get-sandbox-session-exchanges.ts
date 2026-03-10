/**
 * Script: get-sandbox-session-exchanges
 *
 * Queries the Conformance DynamoDB table for all exchanges belonging to a session
 * whose sort key falls within a given date range.
 *
 * The table schema (from CDK):
 *   Partition key : PK  (String)  — "session#<sessionId>"
 *   Sort key      : SK  (String)  — UTC timestamp (e.g. "2026-03-06T07:46:14.734397249Z")
 *
 * Usage:
 *   npm run get-sandbox-session-exchanges -- <sessionId> <dateMin> <dateMax>
 *
 * Arguments:
 *   sessionId   The session identifier.
 *   dateMin     Lower bound date (inclusive) in YYYY-MM-DD format.
 *   dateMax     Upper bound date (inclusive) in YYYY-MM-DD format.
 *
 * Example:
 *   npm run get-sandbox-session-exchanges -- abc123 2025-01-01 2025-12-31
 */

import { queryByPkAndSkRange } from '../aws/dynamodb';

export async function getSandboxSessionExchanges(
  sessionId: string,
  dateMin: string,
  dateMax: string,
): Promise<unknown[]> {
  const items = await queryByPkAndSkRange(
    `session#${sessionId}`,
    dateMin,
    dateMax,
  );
  return items.map((item) => JSON.parse(item['value'] as string));
}

async function main(): Promise<void> {
  const [, , sessionId, dateMin, dateMax] = process.argv;

  if (!sessionId || !dateMin || !dateMax) {
    console.error(
      'Error: sessionId, dateMin, and dateMax arguments are required.\n' +
        'Example: npm run get-sandbox-session-exchanges -- abc123 2025-01-01 2025-12-31',
    );
    process.exit(1);
  }

  const datePattern = /^\d{4}-\d{2}-\d{2}$/;
  if (!datePattern.test(dateMin) || !datePattern.test(dateMax)) {
    console.error('Error: dateMin and dateMax must be in YYYY-MM-DD format.');
    process.exit(1);
  }

  const items = await getSandboxSessionExchanges(sessionId, dateMin, dateMax);

  if (items.length === 0) {
    console.log('No exchanges found.');
    return;
  }

  console.log(`Found ${items.length} exchange(s):\n`);
  for (const item of items) {
    console.log(JSON.stringify(item, null, 2));
    console.log('---');
  }
}

main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

