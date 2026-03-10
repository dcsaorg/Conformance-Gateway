/**
 * Script: query-dynamodb
 *
 * Queries the Conformance DynamoDB table by partition key (PK).
 * Optionally filters by a sort-key prefix (SK begins_with).
 *
 * The table schema (from CDK):
 *   Partition key : PK  (String)
 *   Sort key      : SK  (String)
 *
 * Usage:
 *   npm run query-dynamodb -- <PK> [SK_PREFIX]
 *
 * Examples:
 *   npm run query-dynamodb -- sandbox#abc123
 *   npm run query-dynamodb -- sandbox#abc123 session#
 */

import { queryByPkAndSkPrefix } from '../aws/dynamodb';

async function main(): Promise<void> {
  const pk = process.argv[2];
  if (!pk) {
    console.error(
      'Error: PK argument is required.\n' +
        'Example: npm run query-dynamodb -- sandbox#abc123',
    );
    process.exit(1);
  }

  const skPrefix = process.argv[3];

  console.log(`Querying PK="${pk}"${skPrefix ? ` SK begins_with "${skPrefix}"` : ''}...\n`);

  const items = await queryByPkAndSkPrefix(pk, skPrefix);

  if (items.length === 0) {
    console.log('No items found.');
    return;
  }

  console.log(`Found ${items.length} item(s):\n`);
  for (const item of items) {
    console.log(JSON.stringify(item, null, 2));
    console.log('---');
  }
}

main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

