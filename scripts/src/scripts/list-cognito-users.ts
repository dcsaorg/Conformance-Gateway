/**
 * Script: list-cognito-users
 *
 * Lists all users in the configured Cognito User Pool.
 *
 * Usage:
 *   npm run list-cognito-users [-- <email-substring>]
 *
 * Optional argument:
 *   <email-substring>   When provided, only users whose email contains this
 *                       string (case-insensitive) are returned.
 *
 * Examples:
 *   npm run list-cognito-users
 *   npm run list-cognito-users -- alice
 *   npm run list-cognito-users -- @example.com
 */

import { formatUser, listUsers } from '../aws/cognito';

export type CognitoUserEntry = { username: unknown; email: string };

export async function listCognitoUsers(
  emailSubstring?: string,
): Promise<Record<string, CognitoUserEntry[]>> {
  const users = await listUsers();
  const grouped: Record<string, CognitoUserEntry[]> = {};
  const needle = emailSubstring?.toLowerCase();

  for (const user of users) {
    const formatted = formatUser(user);
    const email = (formatted['Attributes'] as Record<string, string>)['email'];
    if (!email) continue;
    if (needle && !email.toLowerCase().includes(needle)) continue;
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
  const emailSubstring = process.argv[2];
  const output = await listCognitoUsers(emailSubstring);
  const count = Object.values(output).reduce((sum, users) => sum + users.length, 0);

  console.log(JSON.stringify(output, null, 2));
  console.error(`\nFetched ${count} user(s).`);
}

if (require.main === module) main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

