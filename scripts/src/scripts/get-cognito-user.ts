/**
 * Script: get-cognito-user
 *
 * Retrieves a single Cognito user by username (Cognito sub UUID or email).
 *
 * Usage:
 *   npm run get-cognito-user -- <username>
 *
 * Example:
 *   npm run get-cognito-user -- alice@example.com
 */

import { getCognitoUser } from '../aws/cognito';

async function main(): Promise<void> {
  const username = process.argv[2];
  if (!username) {
    console.error(
      'Error: username argument is required.\n' +
        'Example: npm run get-cognito-user -- alice@example.com',
    );
    process.exit(1);
  }

  const result = await getCognitoUser(username);

  const attrs: Record<string, string> = {};
  for (const attr of result.UserAttributes ?? []) {
    if (attr.Name) attrs[attr.Name] = attr.Value ?? '';
  }

  console.log(
    JSON.stringify(
      {
        Username: result.Username,
        Status: result.UserStatus,
        Enabled: result.Enabled,
        Created: result.UserCreateDate?.toISOString(),
        LastModified: result.UserLastModifiedDate?.toISOString(),
        MFAOptions: result.MFAOptions,
        Attributes: attrs,
      },
      null,
      2,
    ),
  );
}

if (require.main === module) main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

