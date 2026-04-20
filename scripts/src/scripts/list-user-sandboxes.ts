/**
 * Script: list-user-sandboxes
 *
 * Queries the Conformance DynamoDB table for all sandboxes belonging to a user.
 *
 * The table schema (from CDK):
 *   Partition key : PK  (String)
 *   Sort key      : SK  (String)
 *
 * Usage:
 *   npm run list-user-sandboxes -- <username>
 *
 * Example:
 *   npm run list-user-sandboxes -- alice@example.com
 */

import { queryByPkAndSkPrefix } from '../aws/dynamodb';
import { getSandboxMetadata, SandboxMetadata } from './get-sandbox-metadata';

export async function listUserSandboxes(username: string): Promise<(SandboxMetadata | null)[]> {
  const items = await queryByPkAndSkPrefix(`environment#${username}`, 'sandbox#');
  const sandboxes = items.map((item) => JSON.parse(item['value'] as string) as { id: string });
  return Promise.all(sandboxes.map((s) => getSandboxMetadata(s.id)));
}

async function main(): Promise<void> {
  const username = process.argv[2];
  if (!username) {
    console.error(
      'Error: username argument is required.\n' +
        'Example: npm run list-user-sandboxes -- alice@example.com',
    );
    process.exit(1);
  }

  const items = await listUserSandboxes(username);

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



