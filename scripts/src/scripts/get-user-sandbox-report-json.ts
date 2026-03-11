/**
 * Script: get-user-sandbox-report-json
 *
 * Queries the Conformance DynamoDB table for the content items of a specific sandbox report.
 *
 * The table schema (from CDK):
 *   Partition key : PK  (String)
 *   Sort key      : SK  (String)
 *
 * Usage:
 *   npm run get-user-sandbox-report-json -- <userId> <sandboxId> <reportTimestamp>
 *
 * Example:
 *   npm run get-user-sandbox-report-json -- my-user-id my-sandbox-id 2026-03-10T16:34:11.962087591Z
 */

import { queryByPkAndSkPrefix } from '../aws/dynamodb';

export async function getUserSandboxReportJson(
  userId: string,
  sandboxId: string,
  reportTimestamp: string,
): Promise<unknown[]> {
  const items = await queryByPkAndSkPrefix(
    `environment#${userId}`,
    `report#content#${sandboxId}#${reportTimestamp}`,
  );
  return items.map((item) => JSON.parse(item['value'] as string));
}

async function main(): Promise<void> {
  const userId = process.argv[2];
  const sandboxId = process.argv[3];
  const reportTimestamp = process.argv[4];

  if (!userId || !sandboxId || !reportTimestamp) {
    console.error(
      'Error: userId, sandboxId and reportTimestamp arguments are required.\n' +
        'Example: npm run get-user-sandbox-report-json -- my-user-id my-sandbox-id 2026-03-10T16:34:11.962087591Z',
    );
    process.exit(1);
  }

  const items = await getUserSandboxReportJson(userId, sandboxId, reportTimestamp);

  if (items.length === 0) {
    console.log('No report content found.');
    return;
  }

  console.log(`Found ${items.length} report content item(s):\n`);
  for (const item of items) {
    console.log(JSON.stringify(item, null, 2));
    console.log('---');
  }
}

if (require.main === module) {
  main().catch((err) => {
    console.error('Error:', err);
    process.exit(1);
  });
}



