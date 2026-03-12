/**
 * Script: get-user-sandboxes
 *
 * Queries the Conformance DynamoDB table for all sandboxes belonging to a user.
 *
 * The table schema (from CDK):
 *   Partition key : PK  (String)
 *   Sort key      : SK  (String)
 *
 * Usage:
 *   npm run get-user-sandboxes -- <username>
 *
 * Example:
 *   npm run get-user-sandboxes -- alice@example.com
 */

import { queryByPkAndSkPrefix } from '../aws/dynamodb';

export async function getUserSandboxes(username: string): Promise<unknown[]> {
  const items = await queryByPkAndSkPrefix(`environment#${username}`, 'sandbox#');
  return items.map((item) => JSON.parse(item['value'] as string));
}

async function main(): Promise<void> {
  const username = process.argv[2];
  if (!username) {
    console.error(
      'Error: username argument is required.\n' +
        'Example: npm run get-user-sandboxes -- alice@example.com',
    );
    process.exit(1);
  }

  const items = await getUserSandboxes(username);

  if (items.length === 0) {
    console.log('No sandboxes found.');
    return;
  }

  console.log(`Found ${items.length} sandbox(es):\n`);
  for (const item of items) {
    console.log(JSON.stringify(item, null, 2));
    console.log('---');
  }
}

if (require.main === module) main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

