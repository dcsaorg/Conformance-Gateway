import {
  DynamoDBClient,
  QueryCommand,
  QueryCommandInput,
} from '@aws-sdk/client-dynamodb';
import { marshall, unmarshall } from '@aws-sdk/util-dynamodb';
import { config } from '../config';

const client = new DynamoDBClient({ region: config.region });

// ─── Chunked-value support ────────────────────────────────────────────────────
//
// The Java ConformancePersistenceProvider splits large item values into chunks:
//
//   PK=itemPK / SK=itemSK
//     value = "DCSA_CONFORMANCE_CHUNKED_VALUE#<CHUNK_UUID>"          ← redirect
//
//   PK=itemPK / SK=chunk#itemSK#<CHUNK_UUID>#00000000
//     value = <first 64 KiB of the original value>
//
//   PK=itemPK / SK=chunk#itemSK#<CHUNK_UUID>#00000001
//     value = <next 64 KiB of the original value>
//   …
//
// When we read an item whose "value" attribute starts with that indicator we
// must fetch all chunk items and concatenate them to recover the full value.

// ─── Value encoding ───────────────────────────────────────────────────────────
//
// The Java layer stores the DynamoDB "value" attribute as JsonNode.toString(),
// which is the JSON encoding of the value.  That means a plain string value is
// stored with surrounding double-quotes, e.g. the redirect marker is stored as:
//   "DCSA_CONFORMANCE_CHUNKED_VALUE#<UUID>"   ← note the literal quote characters
// and a JSON object is stored as a JSON string like:
//   "{\"foo\":\"bar\"}"
//
// We must JSON.parse the raw attribute string to recover the actual value.

/** Parse the raw DynamoDB "value" attribute string into its actual JS value. */
function parseItemValue(raw: Record<string, unknown>): Record<string, unknown> {
  if (typeof raw['value'] !== 'string') return raw;
  try {
    return { ...raw, value: JSON.parse(raw['value'] as string) };
  } catch {
    return raw;
  }
}

const DCSA_CONFORMANCE_CHUNKED_VALUE = 'DCSA_CONFORMANCE_CHUNKED_VALUE';

/** Return true when the item's "value" field is a chunked-value redirect. */
function isChunkedValueRedirect(item: Record<string, unknown>): boolean {
  if (typeof item['value'] !== 'string') return false;
  try {
    const decoded = JSON.parse(item['value'] as string);
    return typeof decoded === 'string' && decoded.startsWith(DCSA_CONFORMANCE_CHUNKED_VALUE + '#');
  } catch {
    return false;
  }
}

/**
 * Extract the chunk UUID from a redirect item's raw (JSON-encoded) value string.
 * e.g. raw value `"\"DCSA_CONFORMANCE_CHUNKED_VALUE#abc-123\""` → "abc-123"
 */
function extractChunkUuid(rawRedirectValue: string): string {
  const decoded = JSON.parse(rawRedirectValue) as string;
  return decoded.slice(DCSA_CONFORMANCE_CHUNKED_VALUE.length + 1);
}

/**
 * Build the sort-key prefix used for the chunk items of a given original SK
 * and chunk UUID: `chunk#<originalSK>#<CHUNK_UUID>#`
 */
function chunkSortKeyPrefix(originalSk: string, chunkUuid: string): string {
  return `chunk#${originalSk}#${chunkUuid}#`;
}

/**
 * Fetch all chunk items for one redirect item and return a copy of that item
 * with "value" replaced by the fully reassembled string.
 *
 * Chunk items are already returned in sorted order by DynamoDB (SK is
 * lexicographically ordered, and the 8-digit zero-padded index ensures correct
 * ordering), so we just concatenate in the order they arrive.
 */
async function resolveChunkedItem(
  item: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  const pk = item['PK'] as string;
  const sk = item['SK'] as string;
  const redirectValue = item['value'] as string;
  const chunkUuid = extractChunkUuid(redirectValue);
  const skPrefix = chunkSortKeyPrefix(sk, chunkUuid);

  const chunks = await queryRawByPkAndSkPrefix(pk, skPrefix);

  // Sort by SK to guarantee chunk order even if DynamoDB returns them
  // out of sequence (shouldn't happen, but be defensive).
  chunks.sort((a, b) => {
    const skA = a['SK'] as string;
    const skB = b['SK'] as string;
    return skA < skB ? -1 : skA > skB ? 1 : 0;
  });

  const assembledValue = chunks.map((c) => c['value'] as string).join('');

  return { ...item, value: assembledValue };
}

/**
 * Post-process a list of raw DynamoDB items: for every item whose "value" is a
 * chunked-value redirect, fetch and assemble the full value.
 *
 * Items that are themselves chunk records (SK starts with "chunk#") are
 * stripped from the result — they are implementation details and callers
 * should never see them directly.
 */
async function resolveChunkedValues(
  items: Record<string, unknown>[],
): Promise<Record<string, unknown>[]> {
  // Filter out raw chunk items (they are only meaningful as parts of a redirect).
  const visibleItems = items.filter(
    (item) => typeof item['SK'] !== 'string' || !(item['SK'] as string).startsWith('chunk#'),
  );

  return Promise.all(
    visibleItems.map((item) =>
      isChunkedValueRedirect(item) ? resolveChunkedItem(item) : Promise.resolve(item),
    ),
  );
}

// ─── Internal raw query helper (no chunk resolution) ─────────────────────────
// Used by resolveChunkedItem to fetch chunk pages without infinite recursion.

async function queryRawByPkAndSkPrefix(
  pk: string,
  skPrefix: string,
): Promise<Record<string, unknown>[]> {
  const items: Record<string, unknown>[] = [];
  let lastKey: Record<string, unknown> | undefined;

  const keyCondition = 'PK = :pk AND begins_with(SK, :skPrefix)';
  const exprValues: Record<string, unknown> = { ':pk': pk, ':skPrefix': skPrefix };

  do {
    const input: QueryCommandInput = {
      TableName: config.dynamoDbTableName,
      KeyConditionExpression: keyCondition,
      ExpressionAttributeValues: marshall(exprValues),
      ExclusiveStartKey: lastKey ? marshall(lastKey) : undefined,
    };

    const response = await client.send(new QueryCommand(input));
    for (const raw of response.Items ?? []) {
      items.push(parseItemValue(unmarshall(raw)));
    }

    lastKey = response.LastEvaluatedKey
      ? (unmarshall(response.LastEvaluatedKey) as Record<string, unknown>)
      : undefined;
  } while (lastKey);

  return items;
}


/**
 * Query the Conformance DynamoDB table by partition key (PK).
 *
 * Optionally narrow further with a sort-key condition or filter expression.
 *
 * Chunk items (SK starting with "chunk#") are filtered out automatically and
 * items whose "value" is a chunked-value redirect are transparently reassembled.
 *
 * @param pk                  Partition key value.
 * @param skPrefix            If provided, adds a `begins_with(SK, :skPrefix)` condition.
 * @param filterExpression    Additional DynamoDB FilterExpression (applied after key condition).
 * @param extraExprValues     Extra expression attribute values merged with the key condition values.
 */
export async function queryByPkAndSkPrefix(
  pk: string,
  skPrefix?: string,
  filterExpression?: string,
  extraExprValues?: Record<string, unknown>,
): Promise<Record<string, unknown>[]> {
  const rawItems: Record<string, unknown>[] = [];
  let lastKey: Record<string, unknown> | undefined;

  const keyCondition = skPrefix
    ? 'PK = :pk AND begins_with(SK, :skPrefix)'
    : 'PK = :pk';

  const exprValues: Record<string, unknown> = {
    ':pk': pk,
    ...(skPrefix ? { ':skPrefix': skPrefix } : {}),
    ...(extraExprValues ?? {}),
  };

  do {
    const input: QueryCommandInput = {
      TableName: config.dynamoDbTableName,
      KeyConditionExpression: keyCondition,
      FilterExpression: filterExpression,
      ExpressionAttributeValues: marshall(exprValues),
      ExclusiveStartKey: lastKey ? marshall(lastKey) : undefined,
    };

    const response = await client.send(new QueryCommand(input));
    for (const raw of response.Items ?? []) {
      rawItems.push(unmarshall(raw));
    }

    lastKey =
      response.LastEvaluatedKey
        ? (unmarshall(response.LastEvaluatedKey) as Record<string, unknown>)
        : undefined;
  } while (lastKey);

  return resolveChunkedValues(rawItems);
}

/**
 * Count items in the Conformance DynamoDB table by partition key (PK) with a
 * sort-key range, without fetching the item data.
 *
 * Uses `Select: 'COUNT'` so DynamoDB returns only the count, avoiding the
 * transfer of potentially very large (and chunked) value attributes.
 *
 * Chunk items (SK starting with "chunk#") are excluded from the count because
 * the key condition `SK BETWEEN skMin AND skMax` will never match their
 * "chunk#…" prefix unless explicitly requested.
 *
 * @param pk      Partition key value.
 * @param skMin   Lower bound for the sort key (inclusive).
 * @param skMax   Upper bound for the sort key (inclusive).
 */
export async function countByPkAndSkRange(
  pk: string,
  skMin: string,
  skMax: string,
): Promise<number> {
  let count = 0;
  let lastKey: Record<string, unknown> | undefined;

  const keyCondition = 'PK = :pk AND SK BETWEEN :skMin AND :skMax';

  const exprValues: Record<string, unknown> = {
    ':pk': pk,
    ':skMin': skMin,
    ':skMax': skMax,
  };

  do {
    const input: QueryCommandInput = {
      TableName: config.dynamoDbTableName,
      KeyConditionExpression: keyCondition,
      ExpressionAttributeValues: marshall(exprValues),
      Select: 'COUNT',
      ExclusiveStartKey: lastKey ? marshall(lastKey) : undefined,
    };

    const response = await client.send(new QueryCommand(input));
    count += response.Count ?? 0;

    lastKey = response.LastEvaluatedKey
      ? (unmarshall(response.LastEvaluatedKey) as Record<string, unknown>)
      : undefined;
  } while (lastKey);

  return count;
}

/**
 * Query the Conformance DynamoDB table by partition key (PK) with a sort-key range.
 *
 * Returns items where PK matches and SK is between skMin and skMax (inclusive).
 *
 * Chunk items (SK starting with "chunk#") are filtered out automatically and
 * items whose "value" is a chunked-value redirect are transparently reassembled.
 *
 * @param pk                  Partition key value.
 * @param skMin               Lower bound for the sort key (inclusive).
 * @param skMax               Upper bound for the sort key (inclusive).
 * @param filterExpression    Additional DynamoDB FilterExpression (applied after key condition).
 * @param extraExprValues     Extra expression attribute values merged with the key condition values.
 */
export async function queryByPkAndSkRange(
  pk: string,
  skMin: string,
  skMax: string,
  filterExpression?: string,
  extraExprValues?: Record<string, unknown>,
): Promise<Record<string, unknown>[]> {
  const rawItems: Record<string, unknown>[] = [];
  let lastKey: Record<string, unknown> | undefined;

  const keyCondition = 'PK = :pk AND SK BETWEEN :skMin AND :skMax';

  const exprValues: Record<string, unknown> = {
    ':pk': pk,
    ':skMin': skMin,
    ':skMax': skMax,
    ...(extraExprValues ?? {}),
  };

  do {
    const input: QueryCommandInput = {
      TableName: config.dynamoDbTableName,
      KeyConditionExpression: keyCondition,
      FilterExpression: filterExpression,
      ExpressionAttributeValues: marshall(exprValues),
      ExclusiveStartKey: lastKey ? marshall(lastKey) : undefined,
    };

    const response = await client.send(new QueryCommand(input));
    for (const raw of response.Items ?? []) {
      rawItems.push(unmarshall(raw));
    }

    lastKey =
      response.LastEvaluatedKey
        ? (unmarshall(response.LastEvaluatedKey) as Record<string, unknown>)
        : undefined;
  } while (lastKey);

  return resolveChunkedValues(rawItems);
}
