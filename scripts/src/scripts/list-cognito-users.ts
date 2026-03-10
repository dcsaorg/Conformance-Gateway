/**
 * Script: list-cognito-users
 *
 * Lists all users in the configured Cognito User Pool.
 *
 * Usage:
 *   npm run list-cognito-users
 *
 * Optional environment variable:
 *   COGNITO_FILTER   A Cognito filter expression, e.g. 'email = "user@example.com"'
 *
 * Example:
 *   COGNITO_FILTER='email ^= "alice"' npm run list-cognito-users
 */

import { formatUser, listUsers } from '../aws/cognito';

export type CognitoUserEntry = { username: unknown; email: string };

export async function listCognitoUsers(
  filter?: string,
): Promise<Record<string, CognitoUserEntry[]>> {
  const users = await listUsers(filter);
  const grouped: Record<string, CognitoUserEntry[]> = {};

  for (const user of users) {
    const formatted = formatUser(user);
    const email = (formatted['Attributes'] as Record<string, string>)['email'];
    if (!email) continue;
    const domain = email.split('@')[1].split('.').reverse().join('.');
    (grouped[domain] ??= []).push({ username: formatted['Username'], email });
  }

  return Object.fromEntries(
    Object.keys(grouped)
      .sort()
      .map((domain) => [
        domain,
        grouped[domain].sort((a, b) => a.email.localeCompare(b.email)),
      ]),
  );
}

async function main(): Promise<void> {
  const filter = process.env['COGNITO_FILTER'];
  const output = await listCognitoUsers(filter);
  const count = Object.values(output).reduce((sum, users) => sum + users.length, 0);

  console.log(JSON.stringify(output, null, 2));
  console.error(`\nFetched ${count} user(s).`);
}

main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

