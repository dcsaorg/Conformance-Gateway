/**
 * Script: get-sandbox-metadata
 *
 * Queries the Conformance DynamoDB table for the config item of a sandbox
 * and returns the JSON-parsed object stored in the "value" attribute.
 *
 * The table schema (from CDK):
 *   Partition key : PK  (String)
 *   Sort key      : SK  (String)
 *
 * Usage:
 *   npm run get-sandbox-metadata -- <sandboxId>
 *
 * Example:
 *   npm run get-sandbox-metadata -- abc123
 */

import { queryByPkAndSkPrefix } from '../aws/dynamodb';

export interface SandboxMetadata {
  id: string;
  name: string;
  standardName: string;
  standardVersion: string;
  scenarioSuite: string;
  isDefault: boolean;
}

export async function getSandboxMetadata(sandboxId: string): Promise<SandboxMetadata | null> {
  const items = await queryByPkAndSkPrefix(`sandbox#${sandboxId}`, 'config');
  if (items.length === 0) return null;
  const value = items[0]['value'];
  if (value == null) return null;
  const full = JSON.parse(value as string) as Record<string, unknown>;
  return {
    id: full['id'] as string,
    name: full['name'] as string,
    standardName: (full['standard'] as Record<string, unknown>)['name'] as string,
    standardVersion: (full['standard'] as Record<string, unknown>)['version'] as string,
    scenarioSuite: full['scenarioSuite'] as string,
    isDefault: Boolean((full['orchestrator'] as Record<string, unknown>)['active']),
  };
}

async function main(): Promise<void> {
  const sandboxId = process.argv[2];
  if (!sandboxId) {
    console.error(
      'Error: sandboxId argument is required.\n' +
        'Example: npm run get-sandbox-metadata -- abc123',
    );
    process.exit(1);
  }

  const metadata = await getSandboxMetadata(sandboxId);

  if (metadata === null) {
    console.log('No metadata found.');
    return;
  }

  console.log(JSON.stringify(metadata, null, 2));
}

if (require.main === module) main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

