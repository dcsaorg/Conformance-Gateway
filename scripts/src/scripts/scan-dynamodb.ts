/**
 * Script: scan-dynamodb
 *
 * Performs a full scan of the Conformance DynamoDB table and prints the results.
 *
 * ⚠️  A full scan reads every item in the table and consumes read capacity.
 *     Use query-dynamodb when you know the partition key.
 *
 * Usage:
 *   npm run scan-dynamodb
 *
 * Optional environment variables:
 *   SCAN_LIMIT    Maximum number of items to print (default: 100)
 *
 * Example:
 *   SCAN_LIMIT=10 npm run scan-dynamodb
 */

import { scanTable } from '../aws/dynamodb';

async function main(): Promise<void> {
  const limit = process.env['SCAN_LIMIT']
    ? parseInt(process.env['SCAN_LIMIT'], 10)
    : 100;

  console.log(
    `Scanning table (limit=${limit}) — this may take a moment for large tables...\n`,
  );

  const items = await scanTable(undefined, undefined, limit);

  if (items.length === 0) {
    console.log('No items found.');
    return;
  }

  console.log(`Retrieved ${items.length} item(s):\n`);
  for (const item of items) {
    console.log(JSON.stringify(item, null, 2));
    console.log('---');
  }
}

if (require.main === module) main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

