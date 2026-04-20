/**
 * Script: get-users-and-sandboxes
 *
 * Lists all Cognito users whose email contains the given substring, then fetches
 * the sandboxes for each user and displays a grouped domain -> user -> sandbox
 * hierarchy.
 *
 * Usage:
 *   npm run get-users-and-sandboxes -- <emailSubstring>
 *
 * Example:
 *   npm run get-users-and-sandboxes -- @example.com
 */

import { listCognitoUsers } from './list-cognito-users';
import { listUserSandboxes } from './list-user-sandboxes';

async function main(): Promise<void> {
  const emailSubstring = process.argv[2];
  if (!emailSubstring) {
    console.error(
      'Error: emailSubstring argument is required.\n' +
        'Example: npm run get-users-and-sandboxes -- @example.com',
    );
    process.exit(1);
  }

  const grouped = await listCognitoUsers(emailSubstring);

  let totalUsers = 0;
  let totalSandboxes = 0;

  for (const [domain, users] of Object.entries(grouped)) {
    console.log(`\n=== ${domain} ===`);

    for (const user of users) {
      totalUsers++;
      console.log(`\n  ${user.email} (${user.username})`);

      const sandboxes = await listUserSandboxes(user.username as string);

      if (sandboxes.length === 0) {
        console.log('    (no sandboxes)');
      } else {
        for (const sandbox of sandboxes) {
          totalSandboxes++;
          console.log(`    - ${JSON.stringify(sandbox)}`);
        }
      }
    }
  }

  console.log(`\n---`);
  console.log(`Total: ${totalUsers} user(s), ${totalSandboxes} sandbox(es).`);
}

if (require.main === module) main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

