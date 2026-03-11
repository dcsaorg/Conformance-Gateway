/**
 * Script: get-current-sandbox-session
 *
 * Queries the Conformance DynamoDB table for all sessions belonging to a sandbox.
 *
 * The table schema (from CDK):
 *   Partition key : PK  (String)
 *   Sort key      : SK  (String)
 *
 * Usage:
 *   npm run get-current-sandbox-session -- <sandboxId>
 *
 * Example:
 *   npm run get-current-sandbox-session -- abc123
 */

import { queryByPkAndSkPrefix } from '../aws/dynamodb';

export async function getCurrentSandboxSession(sandboxId: string): Promise<string | null> {
  const items = await queryByPkAndSkPrefix(`sandbox#${sandboxId}`, 'state');
  if (items.length === 0) return null;
  const value = items[0]['value'];
  if (value == null) return null;
  return JSON.parse(value as string)['currentSessionId'] ?? null;
}

async function main(): Promise<void> {
  const sandboxId = process.argv[2];
  if (!sandboxId) {
    console.error(
      'Error: sandboxId argument is required.\n' +
        'Example: npm run get-current-sandbox-session -- abc123',
    );
    process.exit(1);
  }

  const sessionId = await getCurrentSandboxSession(sandboxId);

  if (sessionId === null) {
    console.log('No current session found.');
    return;
  }

  console.log(sessionId);
}

if (require.main === module) main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

