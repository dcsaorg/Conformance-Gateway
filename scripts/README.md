# conformance-scripts

TypeScript console scripts for performing AWS operations against the Conformance Gateway infrastructure (Cognito & DynamoDB).

## Prerequisites

- Node.js ≥ 18
- AWS credentials configured — either via:
  - `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` environment variables, or
  - a named AWS CLI profile (`AWS_PROFILE=my-profile`), or
  - an EC2/Lambda instance role

## Setup

```bash
cd scripts
npm install
cp .env.example .env
# Edit .env and fill in the correct values for the target environment
```

## Configuration (`.env`)

| Variable               | Description                                      | Example                     |
|------------------------|--------------------------------------------------|-----------------------------|
| `AWS_REGION`           | AWS region                                       | `eu-north-1`                |
| `COGNITO_USER_POOL_ID` | Cognito User Pool ID                             | `eu-north-1_rGS9UPweZ`      |
| `DYNAMODB_TABLE_NAME`  | DynamoDB table name (prefix + "Conformance")     | `dev-Conformance`           |

Known environment values:

| Env   | `COGNITO_USER_POOL_ID`    | `DYNAMODB_TABLE_NAME` |
|-------|---------------------------|-----------------------|
| dev   | `eu-north-1_rGS9UPweZ`    | `dev-Conformance`     |
| dt    | `eu-north-1_It7UzW00Z`    | `dt-Conformance`      |
| test  | `eu-north-1_VRXXJfTsv`    | `test-Conformance`    |

## Available Scripts

### List all Cognito users

```bash
npm run list-cognito-users
```

Optional: apply a Cognito [filter expression](https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_ListUsers.html#CognitoUserPools-ListUsers-request-Filter):

```bash
COGNITO_FILTER='email ^= "alice"' npm run list-cognito-users
```

---

### Get a single Cognito user

```bash
USERNAME=alice@example.com npm run get-cognito-user
```

---

### Scan the DynamoDB table

⚠️ Reads every item — use only when you need a full table dump.

```bash
npm run scan-dynamodb
```

Optional: limit the number of items printed (default 100):

```bash
SCAN_LIMIT=20 npm run scan-dynamodb
```

---

### Query DynamoDB by partition key (PK)

```bash
PK=sandbox#abc123 npm run query-dynamodb
```

Optional: narrow by sort-key prefix:

```bash
PK=sandbox#abc123 SK_PREFIX=session# npm run query-dynamodb
```

## Adding New Scripts

1. Create `src/scripts/my-script.ts` using the helpers in `src/aws/`.
2. Add an entry to the `scripts` block in `package.json`:
   ```json
   "my-script": "ts-node src/scripts/my-script.ts"
   ```
3. Run with `npm run my-script`.

