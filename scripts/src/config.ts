import * as dotenv from 'dotenv';
import * as path from 'path';

// Load .env from the scripts/ root (one level up from src/)
dotenv.config({ path: path.resolve(__dirname, '..', '.env') });

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(
      `Missing required environment variable: ${name}\n` +
        `Copy .env.example to .env and fill in the values.`,
    );
  }
  return value;
}

export const config = {
  /** AWS region, e.g. "eu-north-1" */
  region: requireEnv('AWS_REGION'),

  /** Cognito User Pool ID, e.g. "eu-north-1_rGS9UPweZ" */
  cognitoUserPoolId: requireEnv('COGNITO_USER_POOL_ID'),

  /** DynamoDB table name, e.g. "dev-Conformance" */
  dynamoDbTableName: requireEnv('DYNAMODB_TABLE_NAME'),
};

