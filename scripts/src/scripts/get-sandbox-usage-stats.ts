/**
 * Script: get-sandbox-usage-stats
 *
 * Collects exchange counts per sandbox session within a date range across all
 * Cognito users and their sandboxes.
 *
 * For each Cognito user:
 *   1. Lists all sandboxes (PK=environment#<username> SK begins_with sandbox#).
 *   2. Resolves the current session id (PK=sandbox#<sandboxId> SK=state).
 *   3. Counts exchanges in the date range (PK=session#<sessionId>
 *        SK BETWEEN <dateMin> AND <dateMax> (both inclusive)).
 *
 * Output structure (domains/users/sandboxes with positive exchange counts only):
 *   {
 *     "<domain>": {
 *       "<email>": {
 *         "<sandboxName>": {
 *           "metadata": { id, name, standardName, standardVersion, scenarioSuite, isDefault },
 *           "exchanges": <exchangeCount>
 *         }
 *       }
 *     }
 *   }
 *
 * Usage:
 *   npm run get-sandbox-usage-stats -- <dateMin> <dateMax>
 *
 * Arguments:
 *   dateMin   Lower bound date (inclusive) in YYYY-MM-DD format.
 *   dateMax   Upper bound date (inclusive) in YYYY-MM-DD format.
 *
 * Example:
 *   npm run get-sandbox-usage-stats -- 2025-01-01 2025-12-31
 */

import { listCognitoUsers } from './list-cognito-users';
import { getUserSandboxes } from './get-user-sandboxes';
import { getCurrentSandboxSession } from './get-current-sandbox-session';
import { getSandboxMetadata, SandboxMetadata } from './get-sandbox-metadata';
import { queryByPkAndSkRange } from '../aws/dynamodb';

interface SandboxStats {
  metadata: SandboxMetadata | null;
  exchanges: number;
}

type UsageStats = Record<string, Record<string, Record<string, SandboxStats>>>;

async function countExchanges(
  sessionId: string,
  dateMin: string,
  dateMax: string,
): Promise<number> {
  // SK format: <UTC> (e.g. "2026-03-06T07:46:14.734397249Z")
  const items = await queryByPkAndSkRange(
    `session#${sessionId}`,
    dateMin,
    dateMax,
  );
  return items.length;
}

export async function getSandboxUsageStats(
  dateMin: string,
  dateMax: string,
): Promise<UsageStats> {
  const stats: UsageStats = {};

  const usersByDomain = await listCognitoUsers();

  const allUsers = Object.values(usersByDomain).flat();
  let remainingUsers = allUsers.length;

  for (const [domain, users] of Object.entries(usersByDomain)) {
    for (const { email, username } of users) {
      process.stderr.write(`[${remainingUsers--} user(s) remaining] Processing ${email}...\n`);
      const sandboxes = (await getUserSandboxes(username as string)) as {
        id: string;
        name: string;
      }[];

      for (const sandbox of sandboxes) {
        const sessionId = await getCurrentSandboxSession(sandbox.id);
        if (!sessionId) continue;

        const count = await countExchanges(sessionId, dateMin, dateMax);
        if (count === 0) continue;

        const metadata = await getSandboxMetadata(sandbox.id);
        process.stderr.write(`  -> ${sandbox.name} (session ${sessionId}): ${count} exchange(s)\n`);
        ((stats[domain] ??= {})[email] ??= {})[sandbox.name] = { metadata, exchanges: count };
      }
    }
  }

  return stats;
}

async function main(): Promise<void> {
  const [, , dateMin, dateMax] = process.argv;

  if (!dateMin || !dateMax) {
    console.error(
      'Error: dateMin and dateMax arguments are required.\n' +
        'Example: npm run get-sandbox-usage-stats -- 2025-01-01 2025-12-31',
    );
    process.exit(1);
  }

  const datePattern = /^\d{4}-\d{2}-\d{2}$/;
  if (!datePattern.test(dateMin) || !datePattern.test(dateMax)) {
    console.error('Error: dateMin and dateMax must be in YYYY-MM-DD format.');
    process.exit(1);
  }

  const stats = await getSandboxUsageStats(dateMin, dateMax);

  console.log(JSON.stringify(stats, null, 2));

  const summaryMap = new Map<string, number>();
  for (const [reversedDomain, userMap] of Object.entries(stats)) {
    const domain = reversedDomain.split('.').reverse().join('.');
    const allSandboxStats = Object.values(userMap).flatMap((sandboxMap) =>
      Object.values(sandboxMap),
    );
    for (const s of allSandboxStats) {
      const standard = s.metadata
        ? `${s.metadata.standardName} ${s.metadata.standardVersion}`
        : 'unknown';
      const key = `${domain}\t${standard}`;
      summaryMap.set(key, (summaryMap.get(key) ?? 0) + s.exchanges);
    }
  }
  for (const [key, totalExchanges] of summaryMap) {
    process.stderr.write(`${key}\t${totalExchanges}\n`);
  }
}

if (require.main === module) main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

