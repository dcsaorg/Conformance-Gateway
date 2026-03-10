import {
  AdminGetUserCommand,
  AdminGetUserCommandOutput,
  CognitoIdentityProviderClient,
  ListUsersCommand,
  ListUsersCommandInput,
  ListUsersCommandOutput,
  UserType,
} from '@aws-sdk/client-cognito-identity-provider';
import { config } from '../config';

const client = new CognitoIdentityProviderClient({ region: config.region });

/**
 * List all users in the configured Cognito User Pool.
 *
 * @param filter  Optional Cognito filter expression, e.g. `email = "user@example.com"`
 */
export async function listUsers(filter?: string): Promise<UserType[]> {
  const users: UserType[] = [];
  let paginationToken: string | undefined;

  do {
    const input: ListUsersCommandInput = {
      UserPoolId: config.cognitoUserPoolId,
      Filter: filter,
      PaginationToken: paginationToken,
    };

    const response: ListUsersCommandOutput = await client.send(
      new ListUsersCommand(input),
    );

    users.push(...(response.Users ?? []));
    paginationToken = response.PaginationToken;
  } while (paginationToken);

  return users;
}

/**
 * Retrieve a single Cognito user by username.
 *
 * @param username  The Cognito username (usually the sub UUID or the email used at sign-up).
 */
export async function getCognitoUser(
  username: string,
): Promise<AdminGetUserCommandOutput> {
  return client.send(
    new AdminGetUserCommand({
      UserPoolId: config.cognitoUserPoolId,
      Username: username,
    }),
  );
}

/**
 * Format a Cognito UserType for console output.
 */
export function formatUser(user: UserType): Record<string, unknown> {
  const attrs: Record<string, string> = {};
  for (const attr of user.Attributes ?? []) {
    if (attr.Name) attrs[attr.Name] = attr.Value ?? '';
  }
  return {
    Username: user.Username,
    Status: user.UserStatus,
    Enabled: user.Enabled,
    Created: user.UserCreateDate?.toISOString(),
    LastModified: user.UserLastModifiedDate?.toISOString(),
    Attributes: attrs,
  };
}

