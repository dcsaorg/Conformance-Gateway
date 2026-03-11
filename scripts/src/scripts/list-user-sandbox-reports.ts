/**
 * Script: list-user-sandbox-reports
 *
 * Queries the Conformance DynamoDB table for all reports belonging to a user's sandbox.
 *
 * The table schema (from CDK):
 *   Partition key : PK  (String)
 *   Sort key      : SK  (String)
 *
 * Usage:
 *   npm run list-user-sandbox-reports -- <userId> <sandboxId>
 *
 * Example:
 *   npm run list-user-sandbox-reports -- my-user-id my-sandbox-id
 */

import { queryByPkAndSkPrefix } from '../aws/dynamodb';

export async function listUserSandboxReports(userId: string, sandboxId: string): Promise<unknown[]> {
  const items = await queryByPkAndSkPrefix(`environment#${userId}`, `report#digest#${sandboxId}#`);
  return items.map((item) => JSON.parse(item['value'] as string));
}

async function main(): Promise<void> {
  const userId = process.argv[2];
  const sandboxId = process.argv[3];

  if (!userId || !sandboxId) {
    console.error(
      'Error: userId and sandboxId arguments are required.\n' +
        'Example: npm run list-user-sandbox-reports -- alice@example.com my-sandbox-id',
    );
    process.exit(1);
  }

  const items = await listUserSandboxReports(userId, sandboxId);

  if (items.length === 0) {
    console.log('No reports found.');
    return;
  }

  console.log(`Found ${items.length} report(s):\n`);
  for (const item of items) {
    console.log(JSON.stringify(item, null, 2));
    console.log('---');
  }
}

if (require.main === module) main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

