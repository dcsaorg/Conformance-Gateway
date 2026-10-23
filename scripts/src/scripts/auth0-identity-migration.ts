import { readFileSync, writeFileSync } from 'node:fs';
import { parseArgs } from 'node:util';
import { IdentityMigration, Target, Plan, ManualMapping } from '../auth/identity-migration';

async function main(): Promise<void> {
  const { values } = parseArgs({
    options: {
      command: { type: 'string' },
      target: { type: 'string' },
      plan: { type: 'string' },
      exception: { type: 'string' },
      execute: { type: 'boolean', default: false },
      confirm: { type: 'string' },
    },
  });
  if (!values.target || !values.command)
    throw new Error('Required: --target <file> --command inspect|prepare|apply|verify|manual');
  const target: Target = JSON.parse(readFileSync(values.target, 'utf8'));
  const migration = new IdentityMigration(target);
  if (values.execute && values.confirm !== `${target.accountId}/${target.table}`) {
    throw new Error('Execution requires --confirm <accountId>/<table>');
  }
  if (values.execute && !['apply', 'manual'].includes(values.command))
    throw new Error('Command is read-only');
  await migration.verifyTarget();
  switch (values.command) {
    case 'inspect':
      console.log(JSON.stringify(await migration.inspect(), null, 2));
      break;
    case 'prepare': {
      if (!values.plan) throw new Error('--plan required');
      const plan = await migration.prepare();
      // No plaintext email; still sensitive. Refuse overwrite and restrict local file permissions.
      writeFileSync(values.plan, JSON.stringify(plan, null, 2) + '\n', { mode: 0o600, flag: 'wx' });
      console.log(
        JSON.stringify({
          users: plan.users,
          eligible: plan.rows.filter((r) => r.status === 'eligible').length,
          blocked: plan.rows.filter((r) => r.status === 'blocked').length,
          duplicates: plan.rows.filter((r) => r.reason === 'duplicate').length,
        }),
      );
      break;
    }
    case 'apply':
    case 'verify': {
      if (!values.plan) throw new Error('--plan required');
      const plan: Plan = JSON.parse(readFileSync(values.plan, 'utf8'));
      if (values.command === 'apply') await migration.apply(plan, values.execute);
      else await migration.verify(plan);
      console.log(
        values.execute ? 'Applied and verified.' : 'Read-only checks passed; no database writes.',
      );
      break;
    }
    case 'manual': {
      if (!values.exception) throw new Error('--exception required');
      const exception: ManualMapping = JSON.parse(readFileSync(values.exception, 'utf8'));
      await migration.manual(exception, values.execute);
      console.log(
        values.execute
          ? 'Manual mapping transaction completed.'
          : 'Manual mapping dry-run passed; no writes.',
      );
      break;
    }
    default:
      throw new Error('Unknown command');
  }
}

if (require.main === module)
  main().catch((error: unknown) => {
    // AWS exceptions can contain source personal data. Keep console output credential-safe.
    console.error(
      'Migration stopped: validation, source drift, mapping conflict, or AWS access failure. No automatic overwrite or cleanup was attempted. Review the target and protected plan.',
    );
    if (error instanceof Error && error.name === 'Error') console.error(error.message);
    process.exitCode = 1;
  });
