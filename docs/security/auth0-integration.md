# Auth0 integration and rollout

## Scope and trust boundaries

Deployed interactive authentication is **Auth0-only**. The checked-in `cdk/cdk.json` contains no
tenant values: supply real settings before synthesis. Local Angular builds retain email-only
DummyAuth and may only run on a loopback hostname. No Cognito runtime login or fallback remains.

- `AuthService` isolates `Auth0Auth` and local `DummyAuth` behind `AuthInterface`.
  `getApiAuthorization()` supplies the complete API Authorization header. Auth0 uses a Bearer
  **access token**, not an ID token. Application sign-in never accepts passwords.
- The official Auth0 Angular SDK owns Authorization Code + PKCE, Universal Login, callbacks,
  token caching, and renewal. Scopes are `openid profile email offline_access` plus the configured
  API audience. Tokens stay in memory; refresh-token rotation must be enabled in Auth0.
  After a reload the SDK can restore through an Auth0 iframe if browser cookies permit; otherwise
  the user signs in again.
- Logout calls the SDK with `openUrl: false`: local Conformance state ends, central SSO remains.
  It does not revoke already-issued access tokens. No back-channel or global logout is implemented.
- CDK publishes `/auth-config.json` together with the matching Angular build. Bootstrap validates
  its deployment name and API URL before importing the application. Missing/mismatched config
  fails closed. This is public configuration, not a place for secrets.
- API Gateway's native JWT authorizer checks signature, issuer, audience, and token times.
  `WebuiAuthentication` consumes only trusted authorizer claims and additionally requires the
  configured SPA `azp`, a subject, unexpired token, `org_id` matching `org_[A-Za-z0-9]+`, and exact
  permission `standards:functionality:test-conformance`. Claims in a request body confer no access.
  No role names, per-standard permissions, or organization-based ownership are interpreted.
- `WebuiLambda` resolves the principal before dispatching **every** business operation.
  Existing sandbox access checks remain. Machine-facing sandbox keys, ALB/Global Accelerator
  routes, counterpart authentication, and IAM Admin Lambda are unchanged.
- Invalid credentials return 401; missing admission permission/org and sandbox denials return
  403. Reconciliation conflicts return 409; unavailable migration/provider/storage returns 503.
  The frontend distinguishes login, access denied, and retry/support pages and never retries a
  failed mutating operation automatically. The legacy business-error response contract remains.

## Implementation boundaries

- `ConformanceApp` selects configured deployments and checks registration isolation once.
  Each `ConformanceStack` receives an immutable, validated `AuthenticationSettings`; it does not
  re-read auth context. Both Lambda settings and public frontend config come from that value.
- `WebuiAuthentication` assembles runtime clients. `Auth0UserInfoFetcher` has one constructor with an
  injected HTTP client; production uses bounded timeouts and never follows redirects.
- `Auth0IdentityResolver` orchestrates reconciliation through `IdentityMappingRepository`.
  `DynamoIdentityMappingRepository` owns conditional persistence, while `KmsEmailHasher` owns
  email HMAC generation. Neither persistence nor hashing owns Auth0 HTTP requests.
- The repository links identities using one transaction: create the identity if absent, claim
  its environment if unowned, require an unchanged legacy lookup, and require the same active
  inventory. A conflicting request may reuse only a mapping for the **same** identity.
- `checkAccess` is a POST admission probe. HTTP 200 with `{}` means both permission validation and
  personal ownership resolution succeeded. First access may create mappings, but does not read or
  mutate business data. The guard receives original failure statuses; every later business request
  is still independently authorized. The shared handler's empty response supports local DummyAuth;
  deployed admission is enforced by the Lambda before dispatch.

## Configuration from Alan

| Deployment | Tenant | SPA origin / callback (callback adds `/`) | API identifier |
| --- | --- | --- | --- |
| DT | Staging | `https://dt.conformance-dt-1.dcsa.org` | Dedicated DT audience |
| Dev | Staging | `https://dev.conformance-development-1.dcsa.org` | Dedicated dev audience |
| Test (production) | Production | `https://test.conformance-test-1.dcsa.org` | Dedicated test audience |

Confirm these origins against `ConformanceApp` before registering applications. Supply:

1. Canonical HTTPS issuer **including trailing slash**, consistent with discovery, tokens,
   `/userinfo`, and any custom-domain configuration.
2. Three separate public SPA client IDs; no client secrets. Enable Code + PKCE and refresh tokens.
3. A distinct API audience for each environment (not the SPA client ID or `/userinfo` audience).
   Use JWT access tokens with RSA signing supported by API Gateway. Enable API RBAC and include
   the `permissions` claim; configure the exact Conformance permission and independent grants.
4. Organization-enabled central login/registration that supplies `org_id`; authorized and
   unauthorized test accounts, verified emails, and at least two test organizations.
5. Allowed callback URLs above with `/`; allowed web origins and CORS origins for the SPA origins.
   Review allowed logout URLs even though this application does not call central logout.
6. Refresh-token rotation/reuse detection, API offline access, and agreed access-token,
   refresh-token idle/absolute lifetimes. These are Auth0 settings, not invented application defaults.
7. Confirm this API access token can call `/userinfo` with matching `sub`, email, and boolean
   `email_verified`. Validate the actual Gateway serialization of `aud` and `permissions`.

In `cdk/cdk.json`, add a context object named `dtAuth`, `devAuth`, or `testAuth` with required
`issuer`, `audience`, and `clientId` strings from Alan. There is no provider setting. Configure
only DT initially; omitted environments are not synthesized or modified. Supplied but incomplete
settings, no configured environments, or reused audiences/SPA registrations fail synthesis.
Always target one named stack (`dtConformanceStack`, then dev, then test), never `--all`.
CDK derives Lambda environment variables
`AUTH_ISSUER`, `AUTH_AUDIENCE`, `AUTH_CLIENT_ID`, `IDENTITY_HMAC_KEY_ARN`, and the
public frontend config from the same settings. Do not override them independently.
Auth0 adds only its issuer to CSP connect/frame sources and allows the SDK blob worker.
Web UI API CORS remains restricted to that deployment's SPA origin.

## Stable identity and protected legacy inventory

Business records keep their existing `environment#<cognito-username>` keys. Mapping is personal:
changing the selected organization does not change the workspace.

| DynamoDB PK (`SK=v1`) | Purpose |
| --- | --- |
| `auth#identity#<SHA256(issuer + newline + sub)>` | Permanent environment ID and active/revoked status |
| `auth#owner#<environmentId>` | Reverse owner, or a reserved workspace after correction |
| `auth#legacy#<version>#<HMAC(email)>` | Eligible legacy environment or explicit blocked reason |
| `auth#migration` | Ready marker, inventory version, KMS key ARN, source pool |
| `auth#audit#<identity>#<timestamp>` | Manual correction actor, ticket, and previous mapping |

On first authorized access, the resolver requires an activated inventory, fetches `/userinfo`,
checks its subject and verified email, and looks up the normalized email. Normalization trims
ASCII whitespace and lowercases; it rejects unsupported/non-ASCII or malformed addresses.
It does not remove dots, plus tags, or otherwise fold aliases. Java and TypeScript use the same rule.

A retained, deployment-specific, non-exportable KMS HMAC-SHA256 key protects the email lookup
against offline guessing. Do not replace/delete that key or grant GenerateMac broadly. Only the
Auth0 Web UI Lambda is granted GenerateMac by this stack; migration operators need approved IAM.
Plans contain no plaintext emails but still contain sensitive identifiers and must be protected.

Unique enabled, confirmed Cognito accounts with verified email are eligible. Disabled, duplicate,
unverified or unconfirmed accounts create **blocked** entries, not empty/new workspaces.
Unsupported/missing source email aborts preparation for review. A verified Auth0 email absent
from the complete inventory gets an application-owned `auth0-<UUID>` environment ID.
An email mismatch can therefore produce a new empty workspace; support must investigate it,
not move/delete data automatically.

First linking atomically creates forward and reverse records and checks the inventory version
and legacy entry. A competing identity cannot claim an existing owner. Same-identity retries
reuse the winner; conflicts fail safely. Subsequent requests use the permanent mapping, never
email. Revoked mappings are not automatically recreated. No database copying or merging occurs.

## Operator preparation and migration commands

**The AWS commands in this section are for an approved operator, not part of local tests.**
Do not execute writes until the target, inventory, exceptions, backups, and change ticket are reviewed.
Use separate AWS profiles and target/plan files for each environment. The CLI does not load the
legacy scripts `.env`; use the standard AWS credential chain/profile.

Create a protected target JSON file outside version control with these exact fields:

| Field | Required value |
| --- | --- |
| `environment` | `dt`, `dev`, or `test` |
| `accountId` | Approved 12-digit AWS account |
| `region` | Actual deployment region (currently `eu-north-1`) |
| `table` | `dtConformance`, `devConformance`, or `testConformance`, matching environment |
| `userPoolId` | Existing Cognito pool for that deployment |
| `keyArn` | Full ARN from the stack output `<environment>IdentityHmacKeyArn` |

The CLI verifies table/pool ARNs against the account and region. Review the pool association
against the deployed stack as well: a valid ARN alone cannot establish that operational choice.
Preparation needs Cognito ListUsers/DescribeUserPool, DynamoDB DescribeTable/GetItem/Query,
and KMS GenerateMac. Verification additionally needs Scan. Execution needs conditional PutItem,
UpdateItem and ConditionCheckItem permissions for transactional writes under `auth#*` only;
do not grant business-partition write access. Manual execution records STS GetCallerIdentity.
Scope permissions to the selected table, pool, and KMS key and use a short-lived operator role.

From repository root, set `AWS_PROFILE` to the approved deployment profile. Replace the shell
variables below with absolute protected paths; these examples intentionally contain no live values.

```sh
npm --prefix scripts ci
npm --prefix scripts run auth0-identity-migration -- --command inspect --target "$TARGET"
npm --prefix scripts run auth0-identity-migration -- --command prepare --target "$TARGET" --plan "$PLAN"
npm --prefix scripts run auth0-identity-migration -- --command apply --target "$TARGET" --plan "$PLAN"
```

`inspect` returns user/sandbox/report counts. `prepare` creates a new mode-0600 plan (refuses
overwrite), with eligible/blocked/duplicate counts and per-digest reasons. Review duplicates
and blocked users through approved Cognito access; never paste emails/tokens into logs or tickets.
The first `apply` is a dry-run: fresh-source and target checks, no database writes.

After separate approval, execute and verify:

```sh
npm --prefix scripts run auth0-identity-migration -- --command apply --target "$TARGET" --plan "$PLAN" --execute --confirm "$ACCOUNT_ID/$TABLE"
npm --prefix scripts run auth0-identity-migration -- --command verify --target "$TARGET" --plan "$PLAN"
```

Writes are immutable/versioned; the ready marker is created last after fresh-source and row
verification. Partial failures do not activate a partial lookup. Retry the same unchanged plan;
source drift requires a new reviewed plan. An activated inventory cannot be replaced by this tool.
Coordinate a **Cognito provisioning/email/status freeze** across preparation and cutover: the
inventory is a snapshot, not continuous synchronization. Subsequent Cognito disables do not
revoke an already mapped Auth0 account; manage Auth0 admission separately. Do not unfreeze
legacy provisioning without an approved process for new exceptions.

`verify` checks plan integrity, readiness, all expected lookup rows, active forward/reverse
ownership, and reserved/orphan owners. Run it during a quiet maintenance window: DynamoDB
Scan is not a multi-record snapshot and concurrent manual corrections can cause a transient failure.
Do not remove unexpected records merely to make verification pass.

### Manual exceptions

Use a protected exception JSON file with `issuer`, `subject`, `environmentId`, and a nonblank
support `ticket`. Verify ownership independently before approving any correction.

- Initial exception: target must be an enabled legacy Cognito environment and unclaimed.
- Replace a subject: additionally supply `previousSubject` and, if different, `previousIssuer`.
  The old principal must actively own the target; it is revoked atomically.
- Correct an already mapped identity to an unclaimed legacy workspace: supply
  `expectedCurrentEnvironmentId`. The former workspace becomes **reserved**, retaining all
  its data. This is not a merge. Do not combine this option with `previousSubject`.

```sh
npm --prefix scripts run auth0-identity-migration -- --command manual --target "$TARGET" --exception "$EXCEPTION"
# Approved execution only:
npm --prefix scripts run auth0-identity-migration -- --command manual --target "$TARGET" --exception "$EXCEPTION" --execute --confirm "$ACCOUNT_ID/$TABLE"
npm --prefix scripts run auth0-identity-migration -- --command verify --target "$TARGET" --plan "$PLAN"
```

The transaction checks the expected prior state and adds an audit record with operator ARN.
There is no automatic unreserve, data merge, deletion, or cleanup command. Conflicts and complex
multi-workspace recovery require separately reviewed support work.

## DT rollout gates (then dev, then test)

1. Obtain Alan's settings and acceptance accounts; record approvals and verify target account,
   pool, table, PITR/backup readiness, and application recovery procedure. Keep the staged assessment intact.
2. Configure `dtAuth`, build the DT Angular artifact and Lambda package, and review the
   `dtConformanceStack` change set. It retains existing Cognito resources through
   `LegacyCognitoResources` without connecting them to authentication, and creates the HMAC key.
   Freeze legacy identity changes before the first approved Auth0 deployment.
3. The first deployment provisions the HMAC key but **does not open general access**: unmapped
   users receive 503 until inventory activation. Schedule this initial reconciliation window;
   there is no Cognito baseline/provider rollback in this revision. No migration runs automatically.
4. Capture `dtIdentityHmacKeyArn`, grant temporary operator permissions, then
   inspect/prepare/review/dry-run/apply/verify the **DT-only** inventory as above. Verify the sole JWT authorizer's issuer and
   audience, Lambda settings, runtime JSON, CSP/CORS, KMS grant, and absence of stateful-resource
   replacements/deletions. Do not deploy all stacks together.
5. Frontend, config and backend must be released together; existing tabs need reload/relogin.
   Confirm `/auth-config.json` is JSON, not SPA fallback HTML, and keep general access closed
   until the activated inventory and acceptance accounts have been verified.
6. Complete the live acceptance checks below before admitting general DT users. Observe safe
   401/403/409/503 counts and investigate conflicts without logging tokens or `/userinfo` responses.
7. Repeat independently for dev and then test; do not reuse inventories, keys, or data.

### Live acceptance still required

- Real browser Code+PKCE callback and safe deep-link restoration; refresh rotation, hard reload
  with/without third-party cookies, expired/revoked session handling, local logout with SSO retained.
- Direct API requests with missing/invalid signatures, expired/wrong issuer/audience/client tokens,
  an ID token, no org, and no permission. Expect denial without touching business data. Validate
  Gateway claim serialization and `/userinfo` compatibility with the real API token.
- Authorized legacy user retains sandboxes, configuration, scenarios and old/new reports; new
  user gets an empty personal environment; cross-user access fails; org changes preserve ownership.
- Disabled/duplicate/mismatched identities, concurrent first login, safe support correction,
  application-release recovery, unchanged machine key traffic and IAM administration.
- Passive deployed security checks per [ZAP scanning](zap-scanning.md), and browser/manual CI
  verification. Do not run generic active crawlers against the mutating Web UI POST API.

## Local validation

Use Java 25 and Node 20+. From repository root:

```sh
./mvnw -pl lambda,sandbox,cdk -am -DskipTests install
./mvnw -pl lambda,sandbox,cdk test
npm --prefix scripts test
npm --prefix webui test -- --runInBand
npm --prefix webui run build -- --configuration dt
./mvnw -pl spring-boot -am -DskipTests package
./mvnw -pl spring-boot -DskipTests package spring-boot:repackage
npm --prefix scripts run run-conformance-suite -- --standard Booking --version 2.0.0 --suite Conformance --start-command 'java -jar spring-boot/target/spring-boot-1.0.0-SNAPSHOT.jar --conformance.useDynamoDb=false --conformance.createManualTestingSandboxes=false'
git diff --check
```

The initial install prepares fresh reactor dependencies; it is not test evidence. It avoids the
existing upstream Surefire tag/engine issue when testing these modules directly. CDK unit tests
synthesize Auth0 configuration and retained legacy resources locally with asset bundling disabled and no AWS calls.
The report runner uses port **8080** (the local counterpart URL), memory-only storage, and writes
both Booking notification variants under `target/conformance-reports/`. Inspect both HTML reports.
No standard behavior was changed; these tests are regression checks of the shared Web UI boundary.
Do not execute Spring Boot `frontend/**` or `manual/**` tests as an agent; reserve those for CI/humans.
Mocks do not prove actual Auth0/Gateway cryptographic or browser behavior; the live gate remains.

## Recovery and legacy retirement

Recovery means fixing configuration or restoring a known-good **Auth0 application release**, not
switching back to Cognito. Before the first accepted Auth0 release, plan for maintenance/fail-closed
access while correcting issues. Retain all `auth#*` records and the HMAC key; do not restore an old
whole-table backup over new business data. Reserved workspaces retain data and are never merged.

Cognito remains only in operator inventory tools and `LegacyCognitoResources`. That helper keeps
the exact original construct scope/IDs and RETAIN policies so application cleanup cannot delete or
replace the source pools/clients. Keep them until inventory and manual reconciliation are signed
off. Decommission them in a separately reviewed infrastructure change, retire obsolete operator
grants, and stop legacy provisioning. Do not remove permanent identity mappings. Retire the legacy
lookup/HMAC dependency only after reconciliation closes and the first-use resolver is updated.

The local Selenium fixture is email-only. `AWSEnvironmentTest` is explicitly disabled pending an
Auth0 Universal Login fixture and staging accounts; it is not covered by local test results.
The passive authenticated ZAP plan now accepts a short-lived Auth0 access token, not username/password
secrets. See the scan guide for setup and remaining live validation.

