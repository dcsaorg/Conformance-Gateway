import {
  DynamoDBClient,
  QueryCommand,
  QueryCommandInput,
  ScanCommand,
  ScanCommandInput,
} from '@aws-sdk/client-dynamodb';
import { marshall, unmarshall } from '@aws-sdk/util-dynamodb';
import { config } from '../config';

const client = new DynamoDBClient({ region: config.region });

/**
 * Scan the entire Conformance DynamoDB table.
 *
 * ⚠️  Use sparingly — a full scan reads every item and consumes capacity.
 *
 * @param filterExpression   Optional DynamoDB FilterExpression.
 * @param expressionValues   Values for the filter expression (plain JS object — will be marshalled).
 * @param limit              Cap the total number of items returned (undefined = no cap).
 */
export async function scanTable(
  filterExpression?: string,
  expressionValues?: Record<string, unknown>,
  limit?: number,
): Promise<Record<string, unknown>[]> {
  const items: Record<string, unknown>[] = [];
  let lastKey: Record<string, unknown> | undefined;

  do {
    const input: ScanCommandInput = {
      TableName: config.dynamoDbTableName,
      FilterExpression: filterExpression,
      ExpressionAttributeValues: expressionValues
        ? marshall(expressionValues)
        : undefined,
      ExclusiveStartKey: lastKey ? marshall(lastKey) : undefined,
    };

    const response = await client.send(new ScanCommand(input));
    for (const raw of response.Items ?? []) {
      items.push(unmarshall(raw));
      if (limit !== undefined && items.length >= limit) return items;
    }

    lastKey =
      response.LastEvaluatedKey
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
  const items: Record<string, unknown>[] = [];
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
      items.push(unmarshall(raw));
    }

    lastKey =
      response.LastEvaluatedKey
        ? (unmarshall(response.LastEvaluatedKey) as Record<string, unknown>)
        : undefined;
  } while (lastKey);

  return items;
}

/**
 * Query the Conformance DynamoDB table by partition key (PK) with a sort-key range.
 *
 * Returns items where PK matches and SK is between skMin and skMax (inclusive).
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
  const items: Record<string, unknown>[] = [];
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
      items.push(unmarshall(raw));
    }

    lastKey =
      response.LastEvaluatedKey
        ? (unmarshall(response.LastEvaluatedKey) as Record<string, unknown>)
        : undefined;
  } while (lastKey);

  return items;
}
