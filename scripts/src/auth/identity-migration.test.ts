import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createHmac } from 'node:crypto';
import { marshall, unmarshall } from '@aws-sdk/util-dynamodb';
import { UserType } from '@aws-sdk/client-cognito-identity-provider';
import {
  IdentityMigration,
  identityKey,
  key,
  normalizeEmail,
  preparePlan,
  Target,
  validateTarget,
} from './identity-migration';

const target: Target = {
  environment: 'dt',
  accountId: '123456789012',
  region: 'eu-north-1',
  table: 'dtConformance',
  userPoolId: 'eu-north-1_fixture',
  keyArn: 'arn:aws:kms:eu-north-1:123456789012:key/fixture',
};
const user = (id: string, email: string, enabled = true): UserType => ({
  Username: id,
  Enabled: enabled,
  UserStatus: 'CONFIRMED',
  Attributes: [
    { Name: 'email', Value: email },
    { Name: 'email_verified', Value: 'true' },
  ],
});
const mac = async (email: string) =>
  createHmac('sha256', 'test-only-key').update(email).digest('hex');

function fixture(source: UserType[] = [user('legacy', 'Alice@example.com')]) {
  const items = new Map<string, any>();
  const commands: any[] = [];
  const db = {
    send: async (command: any) => {
      commands.push(command);
      const input = command.input;
      switch (command.constructor.name) {
        case 'GetItemCommand':
          return { Item: items.get(input.Key.PK.S) };
        case 'PutItemCommand': {
          if (items.has(input.Item.PK.S))
            throw Object.assign(new Error('conflict'), { name: 'ConditionalCheckFailedException' });
          items.set(input.Item.PK.S, input.Item);
          return {};
        }
        case 'TransactWriteItemsCommand':
          return {};
        case 'ScanCommand':
          return {
            Items: [...items.values()].filter(
              (item) =>
                item.PK.S.startsWith('auth#identity#') || item.PK.S.startsWith('auth#owner#'),
            ),
          };
        default:
          throw new Error('Unexpected command: ' + command.constructor.name);
      }
    },
  };
  const migration = new IdentityMigration(
    target,
    db as any,
    {} as any,
    {} as any,
    {
      send: async () => ({
        Account: target.accountId,
        Arn: 'arn:aws:sts::123456789012:assumed-role/migration/operator',
      }),
    } as any,
  );
  migration.users = async () => source;
  migration.prepare = () => preparePlan(target, source, mac);
  return { migration, items, commands, source };
}

test('normalization agrees with Java, preserves aliases and rejects ambiguous syntax', () => {
  assert.equal(normalizeEmail(' Alice+Tag@EXAMPLE.COM '), 'alice+tag@example.com');
  for (const email of ['', 'alice', 'a@@example.com', 'a@exämple.com', 'a b@example.com'])
    assert.throws(() => normalizeEmail(email));
});

test('enabled unique users are eligible; disabled, duplicate and unverified users are blocked', async () => {
  const unverified = user('unverified', 'unverified@example.com');
  unverified.Attributes![1].Value = 'false';
  const plan = await preparePlan(
    target,
    [
      user('one', 'Alice@example.com'),
      user('disabled', 'disabled@example.com', false),
      user('duplicate1', 'duplicate@example.com'),
      user('duplicate2', ' DUPLICATE@example.com '),
      unverified,
    ],
    mac,
  );
  assert.equal(plan.rows.filter((row) => row.status === 'eligible').length, 1);
  assert.equal(plan.rows.filter((row) => row.reason === 'duplicate').length, 1);
  assert.equal(plan.rows.filter((row) => row.reason === 'disabled').length, 1);
  assert.equal(plan.rows.filter((row) => row.reason === 'unverified-or-unconfirmed').length, 1);
  assert.ok(plan.rows.filter((row) => row.status === 'blocked').every((row) => !row.environmentId));
  assert.ok(!JSON.stringify(plan).includes('example.com'));
});

test('malformed source inventory cannot produce an activated partial lookup', async () => {
  await assert.rejects(preparePlan(target, [user('one', '')], mac));
});

test('target validation prevents deployment/account mixing', () => {
  validateTarget(target);
  assert.throws(() => validateTarget({ ...target, table: 'devConformance' }));
  assert.throws(() => validateTarget({ ...target, accountId: '999999999999' }));
});

test('dry-run does not write; execution stages versioned rows then activates; retry is idempotent', async () => {
  const { migration, commands, items } = fixture();
  const plan = await migration.prepare();
  await migration.apply(plan);
  assert.ok(commands.every((c) => c.constructor.name === 'GetItemCommand'));
  assert.equal(items.size, 0);
  await migration.apply(plan, true);
  const writes = commands.filter((c) => c.constructor.name === 'PutItemCommand');
  assert.equal(writes.length, 2);
  assert.equal(writes[writes.length - 1].input.Item.PK.S, 'auth#migration');
  assert.ok(writes[0].input.Item.PK.S.startsWith(`auth#legacy#${plan.version}#`));
  await migration.apply(plan, true);
  assert.equal(commands.filter((c) => c.constructor.name === 'PutItemCommand').length, 2);
  await migration.verify(plan);
});

test('source drift, plan tampering, and activated inventory replacement fail without overwriting', async () => {
  const { migration, source, items } = fixture();
  const plan = await migration.prepare();
  await assert.rejects(migration.apply({ ...plan, rows: [] }, true));
  source.push(user('two', 'bob@example.com'));
  await assert.rejects(migration.apply(plan, true));
  assert.equal(items.size, 0);
  source.pop();
  await migration.apply(plan, true);
  source.push(user('two', 'bob@example.com'));
  await assert.rejects(migration.apply(await migration.prepare(), true));
  assert.equal(items.size, 2);
});

test('verification rejects altered plans, orphaned owners and active mappings to reserved workspaces', async () => {
  const { migration, items } = fixture();
  const plan = await migration.prepare();
  await migration.apply(plan, true);
  await assert.rejects(migration.verify({ ...plan, rows: [] }), /Plan integrity/);
  const put = (suffix: string, values: Record<string, string>) =>
    items.set(`auth#${suffix}`, marshall({ ...key(suffix), ...values }));
  put('owner#legacy', { identity: 'alice' });
  await assert.rejects(migration.verify(plan), /Reverse\/forward/);
  put('identity#alice', { environmentId: 'legacy', status: 'active' });
  await migration.verify(plan);
  put('owner#legacy', { identity: 'alice', status: 'reserved' });
  await assert.rejects(migration.verify(plan), /identity mismatch/);
  put('identity#alice', { environmentId: 'corrected', status: 'active' });
  put('owner#corrected', { identity: 'alice' });
  await migration.verify(plan);
});

test('partial preparations are isolated by version and cannot be confused with an active lookup', async () => {
  const { migration, items, source } = fixture();
  const first = await migration.prepare();
  const { marshall } = await import('@aws-sdk/util-dynamodb');
  items.set(
    `auth#legacy#${first.version}#partial`,
    marshall({
      ...key(`legacy#${first.version}#partial`),
      status: 'eligible',
      environmentId: 'old',
    }),
  );
  source.push(user('two', 'bob@example.com'));
  const second = await migration.prepare();
  await migration.apply(second, true);
  assert.notEqual(first.version, second.version);
  assert.equal(unmarshall(items.get('auth#migration')).version, second.version);
});

test('manual mapping requires enabled source and expected owner and never overwrites existing identity', async () => {
  const { migration, commands } = fixture();
  const mapping = {
    issuer: 'https://tenant.example/',
    subject: 'auth0|alice',
    environmentId: 'legacy',
    ticket: 'SUPPORT-123',
  };
  await migration.manual(mapping);
  assert.ok(!commands.some((c) => c.constructor.name === 'TransactWriteItemsCommand'));
  await migration.manual(mapping, true);
  const transaction = commands.find((c) => c.constructor.name === 'TransactWriteItemsCommand').input
    .TransactItems;
  assert.equal(transaction.length, 3);
  assert.ok(
    transaction.every((item: any) => item.Put.ConditionExpression === 'attribute_not_exists(PK)'),
  );
  await assert.rejects(
    migration.manual({ ...mapping, environmentId: 'disabled-or-unknown' }, true),
  );
});

test('manual identity transfer atomically revokes old principal and retains audit evidence', async () => {
  const { migration, commands } = fixture();
  const oldId = identityKey('https://tenant.example/', 'old');
  migration.read = async (suffix) =>
    suffix === 'owner#legacy'
      ? { identity: oldId }
      : suffix === `identity#${oldId}`
        ? { status: 'active', environmentId: 'legacy' }
        : undefined;
  await migration.manual(
    {
      issuer: 'https://tenant.example/',
      subject: 'new',
      previousSubject: 'old',
      environmentId: 'legacy',
      ticket: 'SUPPORT-123',
    },
    true,
  );
  const transaction = commands.find((c) => c.constructor.name === 'TransactWriteItemsCommand').input
    .TransactItems;
  assert.equal(transaction.length, 4);
  assert.equal(transaction[1].Put.ConditionExpression, 'identity = :old');
  assert.equal(transaction[3].Update.ExpressionAttributeValues[':revoked'].S, 'revoked');
  assert.equal(transaction[2].Put.Item.ticket.S, 'SUPPORT-123');
  assert.equal(
    transaction[2].Put.Item.actor.S,
    'arn:aws:sts::123456789012:assumed-role/migration/operator',
  );
});

test('mapping corrections require an expected current environment and reserve its data', async () => {
  const { migration, commands } = fixture();
  const id = identityKey('https://tenant.example/', 'alice');
  migration.read = async (suffix) =>
    suffix === `identity#${id}`
      ? { status: 'active', environmentId: 'auth0-new' }
      : suffix === 'owner#auth0-new'
        ? { identity: id }
        : undefined;
  const mapping = {
    issuer: 'https://tenant.example/',
    subject: 'alice',
    environmentId: 'legacy',
    ticket: 'SUPPORT-456',
  };
  await assert.rejects(migration.manual(mapping, true));
  await assert.rejects(
    migration.manual({ ...mapping, expectedCurrentEnvironmentId: 'wrong' }, true),
  );
  await migration.manual({ ...mapping, expectedCurrentEnvironmentId: 'auth0-new' }, true);
  const transaction = commands.find((c) => c.constructor.name === 'TransactWriteItemsCommand').input
    .TransactItems;
  assert.equal(transaction[0].Put.ExpressionAttributeValues[':current'].S, 'auth0-new');
  assert.equal(transaction[3].Update.ExpressionAttributeValues[':reserved'].S, 'reserved');
  assert.ok(transaction.every((item: any) => !item.Delete));
});
