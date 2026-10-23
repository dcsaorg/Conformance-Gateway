import { createHash } from 'node:crypto';
import {
  CognitoIdentityProviderClient,
  ListUsersCommand,
  DescribeUserPoolCommand,
  UserType,
} from '@aws-sdk/client-cognito-identity-provider';
import {
  AttributeValue,
  DynamoDBClient,
  DescribeTableCommand,
  GetItemCommand,
  PutItemCommand,
  QueryCommand,
  ScanCommand,
  TransactWriteItemsCommand,
} from '@aws-sdk/client-dynamodb';
import { KMSClient, GenerateMacCommand } from '@aws-sdk/client-kms';
import { STSClient, GetCallerIdentityCommand } from '@aws-sdk/client-sts';
import { marshall, unmarshall } from '@aws-sdk/util-dynamodb';

export interface Target {
  environment: 'dt' | 'dev' | 'test';
  accountId: string;
  region: string;
  table: string;
  userPoolId: string;
  keyArn: string;
}
export interface LegacyRow {
  emailKey: string;
  status: 'eligible' | 'blocked';
  environmentId?: string;
  reason?: string;
}
export interface Plan {
  target: Target;
  version: string;
  rows: LegacyRow[];
  users: number;
}
export interface ManualMapping {
  issuer: string;
  subject: string;
  environmentId: string;
  previousSubject?: string;
  previousIssuer?: string;
  expectedCurrentEnvironmentId?: string;
  ticket: string;
}

export const key = (suffix: string) => ({ PK: `auth#${suffix}`, SK: 'v1' });
export const identityKey = (issuer: string, subject: string) =>
  createHash('sha256')
    .update(issuer + '\n' + subject)
    .digest('hex');
export function normalizeEmail(email: string): string {
  const normalized = email.replace(/^[\x00-\x20]+|[\x00-\x20]+$/g, '').toLowerCase();
  if (
    !/^[\x21-\x7e]+@[\x21-\x7e]+\.[\x21-\x7e]+$/.test(normalized) ||
    normalized.indexOf('@') !== normalized.lastIndexOf('@') ||
    normalized.length > 254
  ) {
    throw new Error(
      'Source inventory contains missing or unsupported email; manual review required',
    );
  }
  return normalized;
}

export function validateTarget(target: Target): void {
  if (
    !['dt', 'dev', 'test'].includes(target.environment) ||
    target.table !== `${target.environment}Conformance` ||
    !/^\d{12}$/.test(target.accountId) ||
    !/^[a-z]+-[a-z]+-\d$/.test(target.region) ||
    !target.userPoolId.startsWith(target.region + '_') ||
    !target.keyArn.startsWith(`arn:aws:kms:${target.region}:${target.accountId}:key/`)
  ) {
    throw new Error('Invalid or inconsistent deployment target');
  }
}

export async function preparePlan(
  target: Target,
  users: UserType[],
  mac: (email: string) => Promise<string>,
): Promise<Plan> {
  validateTarget(target);
  const groups = new Map<string, UserType[]>();
  for (const user of users) {
    if (!user.Username) throw new Error('Source user has no environment ID');
    const email = normalizeEmail(user.Attributes?.find((a) => a.Name === 'email')?.Value ?? '');
    const group = groups.get(email) ?? [];
    group.push(user);
    groups.set(email, group);
  }
  const rows: LegacyRow[] = [];
  for (const [email, group] of groups) {
    const user = group[0];
    const verified = user.Attributes?.some(
      (a) => a.Name === 'email_verified' && a.Value === 'true',
    );
    const eligible =
      group.length === 1 && user.Enabled === true && verified && user.UserStatus === 'CONFIRMED';
    rows.push({
      emailKey: await mac(email),
      status: eligible ? 'eligible' : 'blocked',
      ...(eligible
        ? { environmentId: user.Username! }
        : {
            reason:
              group.length > 1
                ? 'duplicate'
                : !user.Enabled
                  ? 'disabled'
                  : 'unverified-or-unconfirmed',
          }),
    });
  }
  rows.sort((a, b) => a.emailKey.localeCompare(b.emailKey));
  if (new Set(rows.map((row) => row.emailKey)).size !== rows.length)
    throw new Error('Digest collision');
  const version = createHash('sha256')
    .update(JSON.stringify({ target, rows, users: users.length }))
    .digest('hex');
  return { target, version, rows, users: users.length };
}

export class IdentityMigration {
  constructor(
    readonly target: Target,
    readonly db = new DynamoDBClient({ region: target.region }),
    readonly cognito = new CognitoIdentityProviderClient({ region: target.region }),
    readonly kms = new KMSClient({ region: target.region }),
    readonly sts = new STSClient({ region: target.region }),
  ) {
    validateTarget(target);
  }

  async verifyTarget(): Promise<void> {
    const table = await this.db.send(new DescribeTableCommand({ TableName: this.target.table }));
    const pool = await this.cognito.send(
      new DescribeUserPoolCommand({ UserPoolId: this.target.userPoolId }),
    );
    const prefix = `arn:aws:`;
    if (
      table.Table?.TableArn !==
        `${prefix}dynamodb:${this.target.region}:${this.target.accountId}:table/${this.target.table}` ||
      pool.UserPool?.Arn !==
        `${prefix}cognito-idp:${this.target.region}:${this.target.accountId}:userpool/${this.target.userPoolId}`
    ) {
      throw new Error('AWS account/target mismatch');
    }
  }

  async users(): Promise<UserType[]> {
    const users: UserType[] = [];
    let PaginationToken: string | undefined;
    do {
      const result = await this.cognito.send(
        new ListUsersCommand({ UserPoolId: this.target.userPoolId, PaginationToken, Limit: 60 }),
      );
      users.push(...(result.Users ?? []));
      PaginationToken = result.PaginationToken;
    } while (PaginationToken);
    return users;
  }

  async prepare(): Promise<Plan> {
    return preparePlan(this.target, await this.users(), async (email) => {
      const result = await this.kms.send(
        new GenerateMacCommand({
          KeyId: this.target.keyArn,
          MacAlgorithm: 'HMAC_SHA_256',
          Message: Buffer.from(email, 'utf8'),
        }),
      );
      if (!result.Mac) throw new Error('KMS HMAC unavailable');
      return Buffer.from(result.Mac).toString('hex');
    });
  }

  async read(suffix: string): Promise<Record<string, any> | undefined> {
    const result = await this.db.send(
      new GetItemCommand({
        TableName: this.target.table,
        Key: marshall(key(suffix)),
        ConsistentRead: true,
      }),
    );
    return result.Item ? unmarshall(result.Item) : undefined;
  }

  private async immutable(item: Record<string, any>, execute: boolean): Promise<void> {
    const suffix = item.PK.substring('auth#'.length);
    const matches = (other: Record<string, any> | undefined) =>
      other &&
      Object.keys(item).every((field) => other[field] === item[field]) &&
      Object.keys(other).length === Object.keys(item).length;
    const existing = await this.read(suffix);
    if (existing) {
      if (!matches(existing))
        throw new Error(`Mapping conflict at ${item.PK}; no overwrite permitted`);
      return;
    }
    if (!execute) return;
    try {
      await this.db.send(
        new PutItemCommand({
          TableName: this.target.table,
          Item: marshall(item),
          ConditionExpression: 'attribute_not_exists(PK)',
        }),
      );
    } catch (error: any) {
      if (error.name !== 'ConditionalCheckFailedException' || !matches(await this.read(suffix)))
        throw error;
    }
  }

  async apply(plan: Plan, execute = false): Promise<void> {
    if (JSON.stringify(plan.target) !== JSON.stringify(this.target))
      throw new Error('Plan target mismatch');
    const fresh = await this.prepare();
    if (
      fresh.version !== plan.version ||
      JSON.stringify(fresh.rows) !== JSON.stringify(plan.rows)
    ) {
      throw new Error('Source inventory or plan changed; prepare and review again');
    }
    const meta = {
      ...key('migration'),
      status: 'ready',
      version: plan.version,
      keyArn: this.target.keyArn,
      userPoolId: this.target.userPoolId,
    };
    // Check marker conflicts BEFORE staging anything. Never modify an activated inventory.
    await this.immutable(meta, false);
    for (const row of plan.rows) {
      const { emailKey, ...values } = row;
      await this.immutable({ ...key(`legacy#${plan.version}#${emailKey}`), ...values }, execute);
    }
    if (execute) {
      if ((await this.prepare()).version !== plan.version)
        throw new Error('Source changed during apply; activation refused');
      // Verify every row before activating, including retry after partial writes.
      for (const row of plan.rows) {
        const actual = await this.read(`legacy#${plan.version}#${row.emailKey}`);
        if (!actual || actual.status !== row.status || actual.environmentId !== row.environmentId)
          throw new Error('Lookup verification failed');
      }
      await this.immutable(meta, true);
    }
  }

  async inspect(): Promise<Record<string, number>> {
    const users = await this.users();
    const counts = {
      users: users.length,
      enabled: users.filter((u) => u.Enabled).length,
      sandboxes: 0,
      reports: 0,
    };
    for (const user of users)
      for (const [name, prefix] of [
        ['sandboxes', 'sandbox#'],
        ['reports', 'report#digest#'],
      ] as const) {
        let ExclusiveStartKey: Record<string, AttributeValue> | undefined;
        do {
          const result = await this.db.send(
            new QueryCommand({
              TableName: this.target.table,
              Select: 'COUNT',
              KeyConditionExpression: 'PK = :p AND begins_with(SK, :s)',
              ExpressionAttributeValues: marshall({
                ':p': `environment#${user.Username}`,
                ':s': prefix,
              }),
              ExclusiveStartKey,
            }),
          );
          counts[name] += result.Count ?? 0;
          ExclusiveStartKey = result.LastEvaluatedKey;
        } while (ExclusiveStartKey);
      }
    return counts;
  }

  async verify(plan: Plan): Promise<void> {
    if (JSON.stringify(plan.target) !== JSON.stringify(this.target))
      throw new Error('Plan target mismatch');
    const version = createHash('sha256')
      .update(JSON.stringify({ target: plan.target, rows: plan.rows, users: plan.users }))
      .digest('hex');
    if (version !== plan.version) throw new Error('Plan integrity mismatch');
    const meta = await this.read('migration');
    if (
      meta?.status !== 'ready' ||
      meta.version !== plan.version ||
      meta.keyArn !== this.target.keyArn ||
      meta.userPoolId !== this.target.userPoolId
    )
      throw new Error('Inventory not activated');
    for (const row of plan.rows) {
      const actual = await this.read(`legacy#${plan.version}#${row.emailKey}`);
      if (
        actual?.status !== row.status ||
        actual?.environmentId !== row.environmentId ||
        actual?.reason !== row.reason
      )
        throw new Error('Legacy row mismatch');
    }
    let ExclusiveStartKey: Record<string, AttributeValue> | undefined;
    do {
      // Projection excludes application data, credentials, and report bodies; scan is read-only.
      const result = await this.db.send(
        new ScanCommand({
          TableName: this.target.table,
          ConsistentRead: true,
          FilterExpression: 'begins_with(PK, :i) OR begins_with(PK, :o)',
          ExpressionAttributeValues: marshall({ ':i': 'auth#identity#', ':o': 'auth#owner#' }),
          ProjectionExpression: 'PK, SK, #s, environmentId, identity',
          ExpressionAttributeNames: { '#s': 'status' },
          ExclusiveStartKey,
        }),
      );
      for (const raw of result.Items ?? []) {
        const item = unmarshall(raw);
        if (item.SK !== 'v1') throw new Error('Unsupported identity record version');
        if (item.PK.startsWith('auth#identity#')) {
          if (item.status === 'revoked') continue;
          const owner = await this.read(`owner#${item.environmentId}`);
          if (
            item.status !== 'active' ||
            !item.environmentId ||
            owner?.status !== undefined ||
            owner?.identity !== item.PK.substring('auth#identity#'.length)
          ) {
            throw new Error('Forward/reverse identity mismatch');
          }
        } else {
          const identity = await this.read(`identity#${item.identity}`);
          const environmentId = item.PK.substring('auth#owner#'.length);
          // A corrected identity leaves its former workspace reserved, never available to claim.
          if (
            !item.identity ||
            !identity ||
            (item.status === 'reserved'
              ? identity.environmentId === environmentId
              : item.status !== undefined ||
                identity.status !== 'active' ||
                identity.environmentId !== environmentId)
          ) {
            throw new Error('Reverse/forward identity mismatch');
          }
        }
      }
      ExclusiveStartKey = result.LastEvaluatedKey;
    } while (ExclusiveStartKey);
  }

  async manual(mapping: ManualMapping, execute = false): Promise<void> {
    const caller = await this.sts.send(new GetCallerIdentityCommand({}));
    if (caller.Account !== this.target.accountId || !caller.Arn)
      throw new Error('Manual mapping caller/account mismatch');
    for (const issuer of [mapping.issuer, mapping.previousIssuer ?? mapping.issuer]) {
      const uri = new URL(issuer);
      if (uri.protocol !== 'https:' || issuer !== uri.origin + '/')
        throw new Error('Invalid issuer');
    }
    if (
      !mapping.subject ||
      !mapping.environmentId ||
      !mapping.ticket?.trim() ||
      /[\r\n]/.test(mapping.subject + (mapping.previousSubject ?? ''))
    )
      throw new Error('Incomplete manual mapping');
    const identity = identityKey(mapping.issuer, mapping.subject);
    const oldIdentity = mapping.previousSubject
      ? identityKey(mapping.previousIssuer ?? mapping.issuer, mapping.previousSubject)
      : undefined;
    if (oldIdentity === identity) throw new Error('Old and new identities must differ');
    const owner = await this.read(`owner#${mapping.environmentId}`);
    const existing = await this.read(`identity#${identity}`);
    if (
      existing?.status === 'active' &&
      existing.environmentId === mapping.environmentId &&
      owner?.identity === identity
    )
      return;
    const current = mapping.expectedCurrentEnvironmentId;
    if (
      current &&
      (oldIdentity ||
        current === mapping.environmentId ||
        existing?.status !== 'active' ||
        existing.environmentId !== current ||
        (await this.read(`owner#${current}`))?.identity !== identity)
    ) {
      throw new Error('Current environment does not match the explicitly approved correction');
    }
    if (
      (existing && !current) ||
      owner?.status === 'reserved' ||
      (owner && owner.identity !== oldIdentity) ||
      (!owner && oldIdentity)
    )
      throw new Error('Manual mapping conflict');
    if (!oldIdentity) {
      const source = (await this.users()).find((u) => u.Username === mapping.environmentId);
      if (!source?.Enabled)
        throw new Error('Manual initial mapping requires an enabled legacy user');
    } else {
      const old = await this.read(`identity#${oldIdentity}`);
      if (old?.status !== 'active' || old.environmentId !== mapping.environmentId)
        throw new Error('Previous identity mismatch');
    }
    const now = new Date().toISOString();
    const items: any[] = [
      {
        Put: {
          TableName: this.target.table,
          Item: marshall({
            ...key(`identity#${identity}`),
            environmentId: mapping.environmentId,
            status: 'active',
            createdAt: now,
          }),
          ConditionExpression: current
            ? 'environmentId = :current AND #s = :active'
            : 'attribute_not_exists(PK)',
          ...(current
            ? {
                ExpressionAttributeNames: { '#s': 'status' },
                ExpressionAttributeValues: marshall({ ':current': current, ':active': 'active' }),
              }
            : {}),
        },
      },
      {
        Put: {
          TableName: this.target.table,
          Item: marshall({ ...key(`owner#${mapping.environmentId}`), identity }),
          ConditionExpression: oldIdentity ? 'identity = :old' : 'attribute_not_exists(PK)',
          ...(oldIdentity ? { ExpressionAttributeValues: marshall({ ':old': oldIdentity }) } : {}),
        },
      },
      {
        Put: {
          TableName: this.target.table,
          Item: marshall({
            ...key(`audit#${identity}#${now}`),
            identity,
            previousIdentity: oldIdentity ?? '',
            previousEnvironmentId: current ?? '',
            environmentId: mapping.environmentId,
            ticket: mapping.ticket,
            actor: caller.Arn,
            createdAt: now,
          }),
          ConditionExpression: 'attribute_not_exists(PK)',
        },
      },
    ];
    if (current)
      items.push({
        Update: {
          TableName: this.target.table,
          Key: marshall(key(`owner#${current}`)),
          UpdateExpression: 'SET #s = :reserved',
          ConditionExpression: 'identity = :id AND attribute_not_exists(#s)',
          ExpressionAttributeNames: { '#s': 'status' },
          ExpressionAttributeValues: marshall({ ':reserved': 'reserved', ':id': identity }),
        },
      });
    if (oldIdentity)
      items.push({
        Update: {
          TableName: this.target.table,
          Key: marshall(key(`identity#${oldIdentity}`)),
          UpdateExpression: 'SET #s = :revoked',
          ConditionExpression: '#s = :active AND environmentId = :e',
          ExpressionAttributeNames: { '#s': 'status' },
          ExpressionAttributeValues: marshall({
            ':revoked': 'revoked',
            ':active': 'active',
            ':e': mapping.environmentId,
          }),
        },
      });
    if (execute) await this.db.send(new TransactWriteItemsCommand({ TransactItems: items }));
  }
}
