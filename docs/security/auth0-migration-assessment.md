# Conformance Tool: Cognito to Auth0 migration assessment

**Original assessment:** October 16, 2026  
**Decision update:** October 22, 2026  
**Purpose:** Verified baseline of the existing Cognito integration plus the subsequent Conformance-to-Auth0 migration decisions agreed with the application owner and discussed with Alan and George.  
**Scope:** Conformance-Gateway and reference-dcsa-org source inspection, read-only AWS inspection of dev/DT/test performed on October 16, and migration requirements confirmed in follow-up discussions. This is a preparation/implementation reference; it is **not** authorization to deploy, change live identity configuration, or write/migrate live application data.

## Evidence labels and boundaries

- **Confirmed — live:** Observed through read-only AWS APIs during this investigation.
- **Confirmed — source:** Present in inspected repository code; not proof that every deployed component matches the checkout.
- **Confirmed — owner:** Environment purpose or business mapping clarified by the Conformance application owner.
- **Agreed — release requirement:** Explicit decision from the October 16–22 follow-up discussion or Alan/George; requirements for this migration release, not a statement that code or tenant configuration already implements them.
- **Selected technical approach:** Agreed direction for implementation, subject to verification of actual SDK/API behavior and safe infrastructure changes.
- **Recommendation:** Proposed approach, not approved or implemented configuration.
- **Pending — Alan/live:** Configuration requested from Alan but not yet supplied/verified in the actual Auth0 tenants.
- **Unknown:** Requires additional evidence or an architectural/operational decision.

**October 16 investigation boundary:** Only the original assessment report was added to the repository. No application code, infrastructure, users, passwords, permissions, or Auth0 configuration were changed as part of that investigation. No application login, password reset, Lambda invocation, deployment, or secret retrieval was performed in that investigation. User values were processed transiently in memory to calculate aggregates; no personal user export was saved or included here. AWS account IDs and credential material are deliberately omitted. **This describes the original investigation, not all activity after October 16**: two test Cognito accounts were subsequently created manually in DT; no fresh AWS inventory has been performed for this revision.

## Executive summary

1. **Legacy data continuity is critical.** The current backend uses `cognito:username` (equal to Cognito `sub` for users observed on October 16) as the application's data `environmentId`. DynamoDB sandbox indexes and saved reports rely on `environment#<environmentId>`. **Preserve those keys**, resolving authenticated Auth0 `(iss, sub)` to the existing Conformance `environmentId`.
2. **Current Conformance authorization is individual, not organization-based.** The original source inspection found no company entity, Cognito groups, or company-owned workspaces. For this release, **sandboxes and reports remain private to their individual owners, independently of the active Auth0 Organization**. Existing sandbox/report ownership checks must remain enforced.
3. **Auth0 integration has been selected.** Implement the official Angular SPA model using Universal Login and OIDC Authorization Code with PKCE, with Auth0 **API access tokens**, validated issuer/audience/signature/expiry, mandatory `org_id`, and the `standards:functionality:test-conformance` permission. Reject unauthenticated or unauthorized API requests. No per-standard restriction is required this release.
4. **Environment mapping is confirmed by Alan.** Conformance **DT and dev use the Auth0 staging tenant** (`dcsa-staging`); Conformance **test (current production) uses the production tenant** (`dcsa-production`). Each deployment needs its own SPA registration and independently controlled API access, and retains a separate DynamoDB data/mapping namespace. Dev and DT can share Auth0 staging identities/organizations; they are **not separate Auth0 directories**.
5. **User account migration is not an Auth0 bulk import.** Users will register or sign in through central CIAM. For existing *enabled* Cognito users, prepare an environment-specific normalized-email-to-legacy-ID lookup; on first authorized Auth0 use, obtain email through `/userinfo` (selected assumption), reconcile by email, and atomically save `(iss, sub) -> environmentId`. Subsequent requests use the permanent mapping. Handle mismatches/conflicts manually; do not automatically map disabled Cognito accounts.
6. **Transition and operations.** Keep Cognito usable as a **temporary provider** behind a clear, per-deployment selection during rollout; retain its stateful AWS resources, and remove it only in a later reviewed cleanup. Preserve local `DummyAuth`, machine-facing sandbox API keys, IAM admin operations, and counterpart integrations. Implement a Conformance-specific access-denied page and local-only logout preserving central SSO.
7. **Rollout and dependencies.** Implement locally now, even though Alan has not supplied Auth0 client IDs, issuer/domain, API audiences, role assignments, and token settings. No deployment or data writes are yet authorized. **Deploy to DT first, then dev, then test/current production**, with explicit validation and rollback checkpoints. Do **not** copy production users or databases into DT.
8. **October 16 AWS inventory remains historical evidence:** 154 Cognito accounts (151 enabled, 3 disabled), 1,280 sandbox-index records, and 12,897 report-digest records were observed then. **Counts are not current** because additional DT test users were created afterward. Reference's inspected server-side integration is informative, but not an implementation blueprint for Conformance's chosen SPA pattern.

## Agreed migration baseline and pending configuration (October 22, 2026)

This section **supersedes earlier exploratory recommendations and open questions** in the original investigation. Sections A–C and H retain the original source/live findings unless explicitly noted. Do not interpret planned behavior as already deployed or verified.

### Confirmed release requirements

| Topic | Decision |
|---|---|
| Interactive login | Auth0 Universal Login; **Angular SPA, Authorization Code + PKCE** using a supported SDK; do not build a custom identity service/BFF just for migration. |
| API credentials | Auth0-issued **API access tokens**, not Auth0 ID tokens, for frontend-to-API requests. |
| API authorization | Validate token issuer, signature, expiry and deployment-specific API audience; require nonempty/trusted `org_id` and `standards:functionality:test-conformance` in `permissions`. Keep backend ownership checks. |
| Standard restrictions | **None in this release**. Any admitted user can use every supported standard. `all-standards` and per-standard permissions are not required for Conformance admission. |
| Role model | Conformance checks permissions, **not role names**. No new Conformance viewer/editor/admin role tiers or delegated administration UI. Alan/central CIAM controls memberships, roles, and permission grants. |
| Organizations | A valid single active Auth0 Organization context is required for login/authorization; a user could belong to multiple organizations but acts in one context at a time. **Organization selection does not change personal Conformance data ownership** in this release. |
| Workspaces | Sandboxes/reports are user-owned and visible regardless of the user's selected organization, provided current organization context grants Conformance access. No sharing or automatic organization remapping. Document this release-specific exception to generic CIAM organization-scoped-data guidance. |
| Identity continuity | Permanent local `(iss, sub) -> environmentId` mapping; preserve historical Cognito-derived `environmentId` and DynamoDB keys. Email is **initial reconciliation only**. |
| Registration | Users register/sign in directly through central Auth0 CIAM; do **not** migrate Cognito passwords or create bulk Auth0 accounts. Existing login/registration and DCSA staff SSO connections are centrally configured. |
| Legacy matching | Use normalized email matching, agreed with George/Alan, for eligible active users. Use `/userinfo` on first unresolved Auth0 access (subject to staging verification), compare `sub`, and require trustworthy email/verification data. Manual corrections for changed/ambiguous emails. |
| Disabled source accounts | Exclude from automatic mapping; leave their historical DynamoDB data untouched. Do not infer they are globally blocked in another Conformance environment. |
| New users | Allocate a new application-owned `environmentId` only when safely established that no eligible legacy mapping applies. Never assign someone else's historical data on lookup or transport errors. |
| Logout | End local Conformance authentication state but **preserve the shared Auth0 SSO session**, as in Reference's current behavior. Cross-application single sign-off/back-channel logout out of scope. |
| No-access UX | Add `/access-denied` (or equivalent) in Angular with a brief access-request message, no login redirect loop; no portal page needed for this release. |
| Local development | Keep `DummyAuth`; no localhost Auth0 application registration needed. |
| Temporary coexistence | Keep Cognito frontend/backend selection and AWS resources available for controlled rollout/rollback; one explicit provider per deployment, not silent fallback or acceptance of both token issuers on the same endpoint. Plan later cleanup. |
| Deployment | **DT → dev → test/current production**; do not use the personal AWS sandbox or separate Auth0 demo lab as Conformance environments. No production copy to DT. |

### Environments and registration requirements

| Conformance deployment | Existing frontend URL from inspected CDK | Auth0 tenant chosen by Alan | Separate registrations/data |
|---|---|---|---|
| `dt` | `https://dt.conformance-dt-1.dcsa.org` | Staging (`dcsa-staging`) | Own SPA client, API audience/access grants, DynamoDB and identity-mapping records |
| `dev` | `https://dev.conformance-development-1.dcsa.org` | Staging (`dcsa-staging`) | Own SPA client, API audience/access grants, DynamoDB and identity-mapping records |
| `test` (live production) | `https://test.conformance-test-1.dcsa.org` | Production (`dcsa-production`) | Own SPA client, API audience/access grants, DynamoDB and identity-mapping records |

The **root frontend URLs were proposed** as Auth0 callback and logout-return URLs in the request to Alan; the new SPA must implement callbacks that actually match those registrations. Allowed Web Origins / CORS origins use each corresponding HTTPS origin. API audiences must be distinct for deployment-level token isolation, and **separate grant/role configuration is also required** so that access to dev does not automatically grant access to DT. Separate SPA clients/audiences *alone* do not create independent user directories, organizations, or permission assignments. Request `openid profile email offline_access`, subject to the configured session/refresh policy; public SPA clients have **no client secret**.

### Trusted identity resolution, data migration, and failure behavior

```text
Auth0 access token validated for the active deployment/API
  -> authorize with org_id + standards:functionality:test-conformance
  -> local identity mapping for (issuer, subject)?
     -> YES: use existing environmentId for all sandbox/report operations
     -> NO: first-use reconciliation only
        -> call /userinfo with a suitable Auth0 access token
        -> verify response subject matches token subject
        -> require usable verified email; normalize consistently
        -> look up eligible, enabled legacy Cognito email -> environmentId
        -> unique unclaimed match: atomically persist mapping
        -> confirmed no legacy match: create new app-owned environmentId
        -> ambiguous, already claimed, disabled, unverified, provider error:
           do not link historical data; fail safely or follow manual review
```

A deterministic lookup may use a consistently normalized email or a protected keyed hash/HMAC; **do not treat a plain hash as ownership proof or store it as the permanent identity**. Conformance must not trust an email or old environment ID supplied independently by the browser. The approved reconciliation criterion is email matching; this does not authorize overwriting an existing mapping. Implement conditional/transactional writes for concurrent first logins and a small controlled exception/remapping procedure. Do not make `/userinfo` a per-request lookup after mapping exists. Test actual `email`, `email_verified`, `sub`, API access-token audience, and organization/permission claims once staging is configured.

Use the resolved `environmentId` consistently for sandbox indexes, configuration, reports, scenario operations, and deletion. Treat a missing `/userinfo` response or failed legacy-data lookup differently from a confirmed genuinely new user. Never rewrite all legacy DynamoDB ownership keys merely to match the Auth0 subject. The legacy lookup is specific to the source Cognito pool and Conformance deployment; the same Auth0 staging identity may map to *different* legacy environment IDs in dev versus DT.

### Remaining dependencies (not open product-design questions)

- **Alan is configuring**, but has not yet returned/verified, the staging/production issuer domains, three SPA client IDs, per-environment API identifiers/audiences, permitted connections/organizations, effective role-permission assignments, and token/refresh-token policies.
- **Staging integration must verify** `/userinfo` availability and `email_verified` semantics for supported customer and DCSA staff connections; no custom email claim is assumed.
- **DT rehearsal** must verify eligible legacy users see prior sandbox/report records, new users get empty workspaces, authorization is enforced for direct API calls, and users cannot see each other's records. Prepare test data in DT itself; **do not clone production user or application data to DT**.
- **Release controls:** preserve and back up source data, review CDK diffs for accidental Cognito/stateful-resource deletion, coordinate portal tile behavior outside Conformance, ensure rollback and credentials-safe logs, then authorize each environment cutover explicitly.
- **Experimental infrastructure is separate:** Alan provided a personal account `DCSA-TechTeam-PC` and access to an Auth0 development test lab in `DCSA-Auth0-Dev`. Neither is a Conformance deployment nor required for the agreed DT-first integration; the lab is not equivalent to staging.

## A. Current Conformance Tool architecture

### A1. Authentication and sessions

**Confirmed — source:** The Angular frontend uses `amazon-cognito-identity-js` (`^6.3.20` declared in `webui/package.json`). `AuthService` selects `CognitoAuth` for deployed Cognito environments and `DummyAuth` for local operation. [CG1–CG3]

| Concern | Current implementation |
|---|---|
| Login | Application-owned email/password form calls `CognitoUser.authenticateUser`; SRP is declared in infrastructure. |
| Session restoration | SDK `getCurrentUser()` and `getSession()`. No separate application session store. |
| Token refresh | Delegated to SDK session handling; no explicit application refresh scheduler. |
| Token storage | No custom SDK storage adapter is supplied. Persistence behavior depends on SDK defaults; it was not separately browser-tested. |
| Authentication state | SDK session `isValid()` and an RxJS status observable. |
| API credential | Cognito **ID token**, returned by `getUserIdToken()`. |
| Logout | SDK `signOut()`; no explicit global-sign-out or hosted logout call. |
| User email accessor | `CognitoAuth.getUserEmail()` returns `null`. |
| Additional challenges | Inspected login callbacks handle success/failure, but do not implement explicit MFA, new-password, sign-up, or password-reset workflows. |

`ApiService.call` sends the ID token in the `Authorization` header without explicitly prepending `Bearer`. API Gateway's JWT authorizer authenticates the Web UI route. Java does not independently verify that token's signature. [CG4–CG6]

**Critical claim dependency:** `WebuiLambda.handleRequest` reads:

```text
requestContext.authorizer.jwt.claims["cognito:username"]
```

It passes this value to the application as the user/environment identifier. No application authorization use of `cognito:groups`, email, company claims, or permission claims was found. API Gateway separately relies on standard JWT issuer, audience, signature, and validity checks. [CG6]

### A2. Users, identity links, and persistence

**Confirmed — source:** Cognito is the identity directory. DynamoDB stores application data and access indexes; no separate application user-profile entity was found. [CG7–CG9]

| Relationship/data | Key pattern |
|---|---|
| User's sandbox index/access record | `PK=environment#<cognito:username>`, `SK=sandbox#<sandboxId>` |
| Sandbox configuration | `PK=sandbox#<sandboxId>`, `SK=config` |
| Sandbox execution state | Sandbox state points to the current session ID. |
| Scenario/party state and recorded traffic | `session#<sessionId>` partitions |
| Saved report digest | `environment#<identity>` / `report#digest#<sandboxId>#<timestamp>` |
| Saved report content | `environment#<identity>` / `report#content#<sandboxId>#<timestamp>` |
| Administrative report jobs | `admin` / `reportJob#...` |

**Confirmed — live on October 16:** Username equals `sub` for all users inspected at that time in all three pools. This observed equality does not change the code's explicit username dependency, and must not be assumed for future/federated identities or other identity providers.

**User lifecycle:** Self-registration is disabled. Repository helpers read Cognito users using `ListUsers` and `AdminGetUser`. No application invitation/create/update/delete-user workflow or user-deletion cascade was found. The actual operator/external provisioning process remains **unknown**. Existing scripts can print personal or application data and were not used to export users. [CG5, CG10]

### A3. Authorization and meaningful access restrictions

**Confirmed — source:** `WebuiAccessChecker.getUserEnvironmentId(userId)` returns `userId`. `checkUserSandboxAccess` checks for an indexed sandbox record under that user's environment partition. [CG7]

The Web UI handler applies sandbox checks to configuration, status, scenarios, action/exchange inspection, saved reports, and deletion. Accessible sandboxes grant the same user the ability to: [CG8]

- Read and update configuration, including counterpart authentication settings.
- Inspect scenarios/status/exchanges and submit action input.
- Start/stop scenarios, complete/skip actions, notify/reset parties, and change notification suppression.
- Generate and retrieve saved reports.
- Soft-delete the sandbox.

There is no separate viewer/editor/operator permission tier. Authenticated users can retrieve the supported-standard catalog and create sandboxes; no per-user standard entitlement check was found. Version, scenario-suite, and tested-party-role validation are business validity checks, not identity entitlements.

**Frontend:** Components such as `EnvironmentComponent` check authentication and redirect to login. The route declaration has no `canActivate` guards, and no claim-driven role/permission gating was found. Backend identity and resource checks remain the security boundary. [CG11]

**Administrative operations:** `AdminLambda` exposes reporting operations, including cross-user reporting, through Lambda invocation rather than the Web UI route. CDK does not expose it as an Auth0/Cognito application-admin endpoint. Its invocation boundary is AWS IAM; actual authorized IAM operators were not enumerated. Lambda execution roles grant infrastructure access and are not end-user roles. [CG5, CG12]

**Protocol roles are not user roles:** Carrier, Shipper, Sending/Receiving eBL Platform, and similar roles describe conformance participants. “DCSA internal” sandbox labeling reflects configuration/type, not an implemented administrator entitlement. Do not translate these into Auth0 roles. [CG8, CG14]

### A4. Separate machine-facing authentication and local bypass

New sandboxes receive a generated `dcsa-conformance-api-key`. The machine-facing sandbox handler compares the configured authentication header/value independently of Cognito; this protects scenario-related API operations and live report endpoints when a header is configured. The handler permits an empty configured auth-header name, and a `dev/null` no-op response precedes the credential check. Outbound counterpart credentials are also sandbox configuration, not human identities. [CG8–CG9]

**Recommendation:** Preserve sandbox API credentials and counterpart integrations during the human-login migration. Moving these to Auth0 machine-to-machine authorization would be a separate change.

Local Spring Boot uses a fixed `spring-boot-env` identity and an unrestricted access checker. `DummyAuth` uses local authentication flags and a synthetic token. Local success therefore does not prove deployed authorization isolation. [CG3, CG13]

### A5. Companies and organizations

**Confirmed — source/live within scope:**

- No application company entity, company administrator, multi-company membership, or company-level sandbox/report enforcement was found.
- The inspected Cognito pools contain no custom attributes; existing users have only `sub`, `email`, and `email_verified` attributes populated.
- Administrative scripts group email addresses by domain and retrieve each username's sandboxes. This is a reporting view, not a company directory or membership grant. [CG10]
- No sandbox ID was indexed under multiple current users within any inspected environment.

**Unknown:** Company associations may exist in SaaStart, the central Auth0 tenant, contracts, or operational records outside these repositories. No organization membership can be reconstructed authoritatively from an email domain alone.

## B. AWS Cognito configuration and environment comparison

### B1. Environment purpose and resource identifiers

**Confirmed — owner:** There are only dev, DT, and test pools. DT is between dev and test; **test is currently production**. A separately named production environment may be introduced later. The placeholder `environment.prod.ts` is not an existing fourth migration target.

**Confirmed — live/local configuration:** All three profiles authenticated successfully after operator SSO login. Resource region is `eu-north-1`; the configured SSO region is `eu-west-1`. Profile accounts match the corresponding account references in Conformance CDK configuration; full account identifiers are omitted. [CG15]

| Environment | Profile / configured SSO role | Stack | Pool ID | Client ID | Database / Web UI API ID |
|---|---|---|---|---|---|
| dev | `cnf-dev` / `PowerUserAccess` | `devConformanceStack` | `eu-north-1_rGS9UPweZ` | `7i6h6m9q1f336lnoohl4c8dkee` | `devConformance` / `1vwiuyuu7e` |
| DT | `cnf-dt` / `PowerUserAccess` | `dtConformanceStack` | `eu-north-1_It7UzW00Z` | `7r5r2a5v06otg47vm1vddhkvle` | `dtConformance` / `82aej8wtud` |
| test (current production) | `cnf-test` / `ReadOnlyAccess` | `testConformanceStack` | `eu-north-1_VRXXJfTsv` | `4b2c2dt168j0tkf1ubg16n3g42` | `testConformance` / `hkgii1c8d1` |

All three database names have **no hyphen**, unlike the examples in `scripts/README.md`.

### B2. Common live pool configuration

No difference was found in the inspected authentication-policy fields:

| Setting | dev, DT, and test |
|---|---|
| Pool names | `devUserPool`, `dtUserPool`, `testUserPool` respectively |
| App Clients | Exactly one per pool; IDs above |
| Self-registration | Disabled; administrator-created users only |
| Sign-in | Email is a **username attribute**, not an alias; `AliasAttributes` absent |
| First authentication factor | Password |
| MFA | Off |
| Password policy | Minimum 8; uppercase, lowercase, digits required; symbols optional |
| Temporary password validity | 7 days |
| Automatic verification / recovery | Email / verified email |
| Attributes requiring verification before update | Empty list |
| Email sending account | `COGNITO_DEFAULT` |
| Hosted/custom domain | None |
| Lambda triggers | None |
| Custom schema attributes | None |
| Cognito groups / group membership | No groups, therefore no memberships |
| Federated providers | None |
| Cognito resource servers/custom scopes | None |
| Cognito Identity Pools | None returned in each inspected account/region |

The full test schema includes Cognito's standard optional attributes (for example name, phone, address, and identities); their availability does not mean they are populated. `sub` is immutable. User-level populated attributes are reported below.

### B3. App Client declarations versus operational login

**Confirmed — deployed CloudFormation template:** All three client definitions declare the same settings:

- Explicit authentication flows: `ALLOW_USER_SRP_AUTH`, `ALLOW_REFRESH_TOKEN_AUTH`.
- OAuth flows enabled: `implicit` and `code`.
- OAuth scopes: `openid`, `profile`, `email`, `phone`, `aws.cognito.signin.user.admin`.
- Callback URL: `https://example.com`.
- Supported identity provider: `COGNITO`.
- User-existence error protection: enabled.
- No explicit token-lifetime or logout-URL properties in the selected declarations.

**Interpretation:** The Angular implementation actually uses SDK/SRP, not a hosted OAuth callback. The broader OAuth configuration appears to be synthesized defaults, not the application's used login path. No pool domain is configured.

**Safety limitation:** `DescribeUserPoolClient` was deliberately not called because its response can include a client secret. The template observations do not establish absence of live client drift. Exact live token lifetimes, read/write attributes, refresh/revocation settings, logout URLs, and complete current client flags remain **unverified**. No default duration is asserted as a measured finding.

### B4. Live API enforcement and infrastructure differences

For each API listed above, both `GET /conformance/webui` and `POST /conformance/webui` require JWT authorization. The authorizer reads `$request.header.Authorization`, uses that environment's client ID as audience, and uses:

```text
https://cognito-idp.eu-north-1.amazonaws.com/<pool-id>
```

Both routes have **empty required-scope lists** in all three environments. There is no deployed route-scope entitlement gate.

Source additionally defines CloudFront/S3 for Angular, API Gateway/Lambda for the Web UI, and ALB/Global Accelerator for machine-facing requests. Test application Lambdas use the optional VPC configuration; dev/DT do not. Domains, credentials, data, and identity namespaces remain environment-specific. [CG5, CG15]

### B5. User statistics and cross-pool overlap

**Confirmed — live, fully paginated **October 16 snapshot**, not a current inventory:**

| Measure | dev | DT | test (current production) |
|---|---:|---:|---:|
| Total users | 11 | 2 | 141 |
| `CONFIRMED` | 11 | 2 | 141 |
| Enabled | 9 | 2 | 140 |
| Disabled | 2 | 0 | 1 |
| Verified email | 11 | 2 | 141 |
| Username equals `sub` | 11 | 2 | 141 |
| Duplicate normalized email sets within pool | 0 | 0 | 0 |
| Users with populated federated `identities` attribute | 0 | 0 | 0 |

Every user observed on October 16 had only `sub`, `email`, and `email_verified` populated. At that point, no observed user was in temporary-password, unconfirmed, reset-required, or external-provider status. **At least two additional DT test users were subsequently created manually; the counts/statuses here were not refreshed.** This is not evidence of recent activity or successful recent login.

The **154 accounts observed on October 16** contain **148 distinct normalized email addresses** (trimmed and case-folded). Pairwise overlap: dev/DT **1**, dev/test **5**, DT/test **1**. Pairwise counts overlap and must not be summed as distinct people. **One normalized email has conflicting enabled/disabled flags across pools.** These aggregates do not prove account ownership or establish Auth0 matches.

### B6. Known-user application data inventory

Only `SK` values for `sandbox#` and `report#digest#` records in current users' `environment#<username>` partitions were queried. No table scan, report content, sandbox configuration, or credential read was performed.

| Measure | dev | DT | test (current production) |
|---|---:|---:|---:|
| Current Cognito users checked | 11 | 2 | 141 |
| Users with sandbox-index records | 9 | 2 | 100 |
| Users without sandbox-index records | 2 | 0 | 41 |
| Sandbox-index records / distinct IDs within environment | 695 | 54 | 531 |
| Sandbox IDs indexed under multiple current users | 0 | 0 | 0 |
| Users with saved-report digests | 9 | 2 | 93 |
| Saved-report digest records | 1,712 | 249 | 10,936 |
| Disabled users with sandbox-index records | 2 | 0 | 0 |

October 16 totals: **1,280 sandbox-index records and 12,897 report digests**. These include any retained soft-deletion/index artifacts. Report contents were not checked for existence or integrity. Queries were read-only and paginated, but not an atomic point-in-time snapshot.

Database metadata reported approximately 35,218 items / 1.53 GB in dev, 5,737 / 0.316 GB in DT, and 142,040 / 7.08 GB in test. Metadata counts are approximate. Full scans were deliberately avoided; partitions owned by deleted/missing Cognito users, orphaned records, and legacy non-user environments remain outside this inventory.

## C. Existing DCSA Auth0 architecture in Reference

### C1. Scope and environment configuration

**Confirmed — source:** Current `reference-app.ts` configures public release sites and protected Snapshot sites separately. [R1]

| Site profile | Labs | Staging | Production |
|---|---|---|---|
| Reference releases | Public | Public | Public |
| Reference Snapshots | Auth0 | Auth0 | Auth0 |

All Snapshot clients use issuer `https://auth.dcsa.org/`, with different client IDs and secret references. Their domains are `snapshots-reference.labs.dcsa.org`, `snapshots-staging.reference.dcsa.org`, and `snapshots.reference.dcsa.org`; each callback is `https://<domain>/auth/callback`. This source configuration was not verified against the live Auth0 tenant or Reference AWS deployment.

### C2. Flow, sessions, and validation

The integration follows a **Regular Web Application/confidential-client pattern**, despite serving static Astro content. [R2–R6]

1. CloudFront protects content with a Lambda@Edge guard and routes non-cached `/auth/*` requests to a regional Lambda through an IAM-protected Function URL/OAC.
2. `/auth/login` creates random state and nonce, a five-minute DynamoDB transaction, and a browser-binding transaction cookie; then redirects to `/authorize` with `response_type=code` and `openid profile email`.
3. `/auth/callback` checks the browser cookie/state, consumes the one-time transaction, and exchanges the code using a server-side client secret read from Secrets Manager.
4. The callback validates the ID token and sets a host-scoped `HttpOnly; Secure; SameSite=Lax` cookie containing it. Cookie lifetime follows token expiry.
5. The edge guard validates the ID-token cookie before protected content delivery. Expiry/invalidity sends the browser through login again; existing Auth0 SSO may avoid another credential prompt.

No PKCE parameters, API `audience`, `organization` parameter, or `offline_access` request are present in the inspected login flow. There is no refresh-token handling or Conformance-like access-token API integration. Logout clears the local cookie only, deliberately preserving the central Auth0 SSO session.

JWT validation uses RS256, cached JWKS, nonempty `sub`, exact issuer, audience, expiry, and nonce at callback. The implementation uses Node crypto/custom handlers rather than an Angular Auth0 SDK. The edge guard consumes `sub`, `email`, and `name` for identified access auditing. [R4–R6]

### C3. Authorization, Organizations, and administration

**Confirmed — source:** The edge guard explicitly permits every valid Auth0 user; its role-check location is a future extension point. No implementation of George's proposed permissions, Organization selection/`org_id` enforcement, Management API user administration, SaaStart integration, or My Organization API was found. No repository-managed Auth0 Actions were found. [R5]

The access ledger records identified content requests, not a user/company membership directory. Its runbook explicitly defers organization information until an authoritative claim or directory exists. Audit-write failure blocks audited content with a generic 503; Matomo remains separate aggregate analytics. [R7]

**Organizational context supplied by the requester, not tenant-verified:** DCSA has central Auth0 tenant/Organization setup; SaaStart participates in user management; domain-related login-time logic exists; Alan owns application registrations. Those facts do not prove which Actions, connections, permissions, Organizations, or administration APIs apply to each Reference/Conformance client. My Organization API adoption is unconfirmed. The deprecated Delegated Administration Dashboard must not be introduced.

### C4. Reusable patterns and limits

**Recommendation:** Reuse central identity/SSO, per-environment registration, server-only secret handling, secure cookies where applicable, state/nonce and callback replay protection, safe return-path handling, cached/fail-closed JWT validation, credential-safe logging, and focused auth tests.

Do **not** copy Reference's static-content cookie gate as if it supplies API/resource authorization. Conformance is an interactive Angular application with a separate API and legacy data ownership. Its chosen flow must support those requirements, and its authorization must not silently broaden to every user in the central tenant.

## D. Authorization requirements and agreed mapping (updated October 22)

The tables and descriptions below describe **requirements to implement**, not behavior already deployed in Auth0. Their operational configuration still depends on Alan.

### D1. Release authorization mapping

| Requirement | Current Conformance implementation | Agreed Auth0/Conformance behavior | Responsibility |
|---|---|---|---|
| Authenticate people | Cognito pool/client + API Gateway JWT authorizer | Auth0 Universal Login, SPA Authorization Code + PKCE, API access tokens | Auth0/SDK + application integration |
| Admit to Conformance | Access effectively controlled by Cognito account membership | Valid token for deployment audience, single `org_id` and `standards:functionality:test-conformance` in **access-token `permissions`** | Auth0 grants; API/backend enforces |
| Limit standards | No per-user restrictions | **Not in this release:** all admitted users can create/access sandboxes for all supported standards | No new authorization code per standard |
| Personal sandboxes | Index record under `environment#<cognito:username>` | Remain private to their owner, regardless of selected Auth0 Organization. Keep index/operation checks | Conformance backend |
| Personal reports | Report data keyed by user environment | Preserve IDs, historical reports, ownership and read/write checks | Conformance backend |
| Organization membership | No app company model | Login must occur with one active Auth0 Organization; membership/roles assigned centrally. **Do not turn org into workspace ownership** | CIAM + token validation |
| Standard permission names | Not used in application authorization | Can exist centrally, but Conformance ignores per-standard grants in this release | Future product scope |
| Role assignment | No Cognito groups; no app role tiers | DCSA assigns organization-scoped roles; API checks **permissions**, never roles | CIAM + Conformance API |
| Machine-facing APIs | Sandbox-specific API keys | **Unchanged** by human SSO migration | Conformance |
| IAM administration | Admin Lambda IAM access | **Unchanged**; no new human Auth0 admin interface | AWS IAM |
| Identity and data ownership | Cognito username is `environmentId` | Resolve Auth0 `(iss, sub)` to a stable internal `environmentId` | Conformance |
| Disabled legacy accounts | No admission while disabled in old pool | **Exclude from auto-mapping**; retain data and avoid automatic restoration | Migration tooling |

The API must reject authenticated users who lack the permission, even when they bypass the customer portal tile via a direct link. Implement an Angular access-denied page that does not create a redirect/login loop. The portal itself owns tile visibility; Conformance does not treat that UI hiding as its security boundary.

### D2. Permissions and standard taxonomy

**Required permission for this release:** `standards:functionality:test-conformance` for access to all standards. Do **not** require `standards:standard:all-standards` or specific `standards:standard:*` claims in Conformance's API for this release. The name `all-functionalities` is not an implied wildcard unless a future explicit contract defines it.

The following mappings were answered for possible future standard-level access, **not required now**:

| Standard | Future central permission mapping | Release behavior |
|---|---|---|
| eBL, including Shipping Instructions, Transport Documents, Issuance, Surrender, Endorsement Chain | `standards:standard:bill-of-lading` | No standard filter now |
| PINT | `standards:standard:platform-interoperability` | No standard filter now |

George proposed functionality permissions `all-functionalities`, `test-conformance`, `view-releases`, `view-snapshots` and various standard-specific permissions under the `standards:` namespace. Those are part of central CIAM taxonomy discussions; actual tenant availability/assignment was **not** verified by the October 16 source/live assessment. Reference's access to releases/snapshots does not grant ownership of private Conformance reports.

### D3. Roles and central administration

There are no current Conformance viewer/editor/operator/admin roles or Cognito groups to migrate. Every admitted user has the existing capabilities over their **own** sandbox/report data. No local role editor, delegated user-management portal or custom roles are required for this release.

Alan clarified the Q4 approach: within a given Auth0 Organization, users are to have the same permissions; access is managed centrally/automatically. CIAM architecture documents contemplate later delegated administration, but Alan stated the delegated-authorization application is **not part of Q4 2026**. DCSA/CIAM must control which organization-scoped roles grant `test-conformance` to each API audience. Conformance only consumes the resulting permission.

### D4. Organizations, deployment access and workspace policy

- A user belongs to one or more Auth0 Organizations but acts in the context of exactly one per issued token; validate `org_id`. CIAM handles organization selection and membership; Conformance does **not** create/assign organizations from email domains.
- **Release-specific data decision:** sandbox/report data are owned by the user rather than the currently selected `org_id`. Users keep the same personal records if the selected organization changes and they continue to have Conformance permission. This exception to generic CIAM org-scoped resource isolation must remain explicit, not an accidental omission.
- No company-based sharing, organization-owned environments, or automatic movement of historical data to organizations in this release.
- Dev and DT use one Auth0 staging tenant and can share its **identities and organization directory**. They have distinct SPA clients, API audience/access grants, DynamoDB databases, and identity-mapping records. Do not claim separate SPA registrations alone make separate role grants; Alan must configure independent access. Test/current production uses the Auth0 production tenant.
- Reference's existing permissive Auth0 code is **not** an authorization policy for Conformance. Enforce the agreed `test-conformance` permission in our backend.

## E. User migration: agreed design, safeguards, and scope

### E1. What is and is not being migrated

**Agreed:** Existing users register/sign in through central Auth0 (customer self-registration or the centrally configured DCSA staff SSO flow). **Do not bulk-create Auth0 users from Cognito; do not import passwords, MFA factors, refresh tokens, or sessions.** Cognito password hashes are not exportable through the standard API, but that is no longer a blocker because importing credentials is explicitly out of scope.

The migration concerns **continuity of application ownership/data**, not transferring identity-provider accounts:

```text
Old: Cognito internal username -> Conformance environmentId -> DynamoDB data
New: validated Auth0 (issuer, sub) -> Conformance environmentId -> same data
```

Each Conformance environment has its **own legacy Cognito pool and DynamoDB database**. The same staging Auth0 account can legitimately map to different legacy identifiers in dev and DT. Never combine or copy deployment databases or assume a shared mapping just because the Auth0 staging tenant is common.

### E2. Initial legacy lookup preparation

- Read eligible **enabled** Cognito accounts for the corresponding deployment; gather internal username (`environmentId`), email, and relevant status. Snapshot/verify again near rollout; Section B statistics are October 16-only.
- Normalize emails consistently (document case folding/trimming) and prepare a **deployment-specific email -> environmentId lookup**. If storing a deterministic email digest, prefer a protected keyed HMAC over an easily guessed plain hash when it materially reduces exposure; protect any key and avoid unnecessary PII in logs.
- Exclude disabled accounts from automatic linking, including their existing historical data. Preserve records even if no mapping is created. Their status in one old environment must not globally block or enable another environment.
- Detect duplicates/collisions/ambiguous emails and already-claimed legacy environments before writing. Do not automatically merge data from different people/environments. One or two changed-email exceptions may be manually corrected via an explicitly controlled mapping procedure.
- Provide read-only inspection and **dry-run** preparation/reporting before any execution. Actual writes require a separate approved migration operation.

### E3. First-use Auth0 identity resolution

1. Accept requests only after validation of Auth0 **API** access-token issuer, signature, expiry, configured deployment audience and `org_id`, plus `standards:functionality:test-conformance` permission.
2. Look for an existing mapping `(iss, sub) -> environmentId` in this Conformance deployment. If found, use it; do **not** re-resolve ownership by email on every request.
3. If absent, obtain the authenticated user's email using Auth0 `/userinfo` (chosen approach). Ensure the returned subject matches the validated token subject. The expected email and `email_verified` attributes for relevant Auth0 connections **still need staging verification**. Do not accept browser-supplied email as proof of identity.
4. Match the normalized email to the eligible legacy lookup. **One unique unclaimed legacy match is sufficient under the approved Alan/George business rule**; do not require a Cognito re-login or invent an additional identity-proof ceremony.
5. Persist the permanent `(iss, sub) -> existing environmentId` mapping **atomically and idempotently**. Reject unexpected takeover/overwrite, conflicting assignments, and concurrent claim races; handle exceptional cases manually.
6. If genuinely no eligible historical match exists and the identity is otherwise valid, create a new application-owned `environmentId` and permanent mapping. Treat network `/userinfo` failures, ambiguous matches and lookup failures differently from a confirmed new user; do not silently create new ownership records when verification is incomplete.
7. Every downstream sandbox/report/config/operation uses the resolved internal `environmentId`, not Auth0 `sub`, email, or organization ID as the DynamoDB partition key.

There should be no requirement for an Auth0 custom `cognito:username` token claim, no per-request Auth0 Management API, and no old ID in the frontend-supplied request. A limited, audited manual correction/remapping tool is sufficient; **do not build a new user-administration application**.

### E4. Data and operation continuity

Retain the entire existing sandbox/report structure: indexes and sandbox IDs/configuration; session and scenario state; recorded traffic; report digests/contents; deletion state; machine API keys and counterpart credentials; administrative reporting relationships. `WebuiAccessChecker`, handler methods, configuration modifications, deletion and reporting paths must agree on the resolved ID; changing only one identity lookup can make old reports disappear or create records in new partitions. [CG7–CG9]

No existing DynamoDB keys are rewritten solely to match Auth0. Cognito resources remain in place through the rollback window, and cleanup requires separate review. Orphaned/deleted-account records were outside the October 16 inventory and must not be silently deleted.

### E5. Operational test plan

- **DT first, without copying production data.** Use DT's own historical data and test identities. Two extra Cognito test accounts were created after the October 16 inventory; determine their *current* status before preparing the lookup.
- Verify eligible existing users see exactly their old sandboxes and reports after Auth0 login; second login reuses permanent mapping; new users see an empty environment; different users cannot read one another's data.
- Verify users without permission or `org_id` are denied, including bookmarked/direct API requests; standard-specific permissions are **not** required; users' personal data are independent of selected organization when still authorized.
- Verify duplicate/concurrent login, disabled old users, changed email/manual mapping, Auth0 `sub` change/relink, expired/wrong-audience tokens, `/userinfo` failures, and rollback to Cognito.
- Deploy only after reviewing CDK diffs and database backup/rollback procedures; DT → dev → test/current production requires separate approvals. No production-to-DT user or data copying.

## F. Technical implementation impact (selected direction)

| Area | Original Conformance/Cognito | Reference/Auth0 example | **Conformance implementation selected** |
|---|---|---|---|
| Browser login | Angular Cognito form/SRP | Confidential server-side Authorization Code with cookie | **Angular SPA**; Auth0 Universal Login, official Angular SDK, Authorization Code + PKCE. No backend auth service/BFF solely for this migration. |
| Tokens and API calls | Cognito **ID** token in `Authorization` | ID token in HttpOnly cookie; not an API permission flow | Obtain Auth0 **API access token**, attach as Bearer; no SPA client secret; configure issuer/audience and refresh rotation according to tenant policy. |
| Token validation | AWS API Gateway JWT authorizer for Cognito | Lambda@Edge ID-token guard | Configure API Gateway JWT authorizer for chosen provider/issuer/audience; **backend additionally checks** `org_id` and the `permissions` array for `test-conformance`. Do not confuse Auth0 permissions with OAuth `scope`. |
| Identity/data | `cognito:username` used as environment ID | `sub` used for audit/gate | Resolve `(iss, sub)` to stable internal `environmentId`, preserving old DynamoDB keys across all handlers/reports. |
| Standard authorization | All admitted users can use all standards | Reference allows valid users | Enforce Conformance **admission only**, not per-standard permissions. Keep sandbox/report ownership checks. |
| Organization data | No company entity or sharing | Org enforcement absent in inspected Reference code | Require token `org_id` for admission, but do not key private data by org or introduce sharing in this release. |
| Session and logout | Cognito SDK `signOut` | Clears app cookie, retains Auth0 SSO | End **local SPA auth state only** without ending central SSO; guard against immediate undesired silent re-login loops. Cross-app global logout out of scope. |
| Authentication abstraction | `AuthInterface`/`AuthService`, `CognitoAuth`, `DummyAuth` | Different server architecture | Add `Auth0Auth` or appropriate adapter to existing abstraction, keeping `CognitoAuth` temporarily and `DummyAuth` for local. Explicit per-deployment selection, one provider active at a time. |
| New UX | Inline Cognito login | Hosted login | Redirect to Universal Login; implement a standalone Angular access-denied view for signed-in users without permission. |
| Auth0 registrations | Not configured | Independent Reference client/secret | Three **public SPA** app clients, three environment-isolated API audiences/access grants (two staging, one production), no frontend client secrets. Details pending Alan. |
| CDK | Creates Cognito pool/client and user-pool authorizer | Cookie/Lambda@Edge infrastructure | Parameterize current API JWT authorizer, environment config, CSP/CORS while **retaining existing Cognito pools and clients** during rollout; audit `cdk diff` for stateful deletion. |
| Scripts / admin | Cognito-based user/sandbox reporting | Not comparable | Add migration lookup preparation, dry-run, controlled execution and remapping; update Cognito-only reporting joins where necessary. Preserve IAM-controlled Admin Lambda. |
| Test coverage | Local dummy auth; functional tests | JWT/login/callback tests | Auth0 SDK/service, API claims, route guards, identity mapping, per-environment separation, user-data continuity, concurrent link and regressions. |

### F1. Maintainability and provider lifecycle

Use the existing provider abstraction instead of a new generalized identity framework; do not scatter `if (auth0)` throughout business logic. Keep external identity validation distinct from internal `environmentId` resolution. Keep provider selection **explicit and environment-specific** in frontend and backend/CDK, with no automatic fallback or accidental acceptance of both token issuers in one deployment.

Cognito coexistence is a **temporary migration/rollback capability**, not an ongoing business requirement. Once Auth0 is stable in DT, dev and production and rollback is no longer needed, remove old Cognito code/config/resources as a separate change. In particular, never accidentally delete Cognito User Pools/clients when switching JWT authorizer wiring in CDK.

### F2. Known security boundaries and technical risks

- Authentication and backend authorization must both fail closed; the customer portal's tile visibility is not an API access control.
- `org_id` is required even though Conformance workspace data stay personal. For users in more than one organization, permissions must be checked for the **active** organization's token; never union privileges across organizations.
- The API Gateway authorizer handles JWT validation; the application needs the claims for permission checks and environment resolution. Do not rely solely on an `authorizationScopes` check if permissions are only in Auth0's `permissions` claim.
- Confirm `/userinfo` accepts the issued access token with the selected audience/scopes. Validate the returned `sub` and the expected verification information when integrating with staging.
- Review credential/token-sensitive logging: the original source logs complete request events and sandbox configuration; avoid adding Auth0 tokens or PII to logs. [CG6, CG8–CG9]
- Preserve sandbox-specific machine API keys and counterpart credentials untouched; don't replace these with human Auth0 authorization.
- All Auth0 environment values are pending Alan. Implement validation and configuration placeholders locally; fail clearly when selecting Auth0 without required settings, not by deploying fake issuers/audiences.

**Implementation validation status:** no build, runtime integration test or migrated-user validation was performed in the **October 16 original assessment**. This updated document records choices and scope; it does not claim any new code, Auth0 tenant configuration, or live test has been performed.

## G. Configuration requested from Alan and remaining work (October 22)

Most questions in the original assessment have now been answered. **Do not reopen settled business decisions** unless the implementation exposes a concrete incompatibility.

### G1. Requests already sent; configuration pending

Alan has been asked to create the following, prioritizing **DT**:

| Environment | Auth0 tenant | Required configured resources |
|---|---|---|
| DT | `dcsa-staging` | Separate Angular SPA client, distinct Conformance DT API audience/access grants |
| dev | `dcsa-staging` | Separate Angular SPA client, distinct Conformance dev API audience/access grants |
| test (production) | `dcsa-production` | Separate Angular SPA client, distinct Conformance test API audience/access grants |

Common requested setup: OIDC Authorization Code + PKCE; `openid profile email offline_access`; root callback/logout URLs and allowed browser origins matched to each deployment's HTTPS frontend; public SPA clients (no client secret); Universal Login with existing DCSA CIAM customer/Entra connections and organization selection; API RS256 with RBAC and **Add Permissions in the Access Token**, refresh-token rotation/offline access per tenant policy. Each applicable organization-level role grant must provide `standards:functionality:test-conformance` for the correct **environment's API**; dev access must not automatically grant DT access. No per-standard permission filtering required in this release.

Still awaiting: **issuer/domain**, three **client IDs**, three **API audience identifiers**, allowed test accounts/organizations and their permission assignments, actual token/refresh lifetimes and policies, and verification of effective token and `/userinfo` behavior. Do not invent values or silently reuse Reference's issuer/client configuration. No localhost client registration or client secret should be needed for the SPA.

### G2. Application and migration implementation work

1. **Implement authentication/authorization** behind existing abstractions with a deployment-level Cognito/Auth0 switch; use the SPA SDK and central Universal Login, validated API JWTs, `org_id`, permissions, frontend access-denied UX, and local-only logout. Keep DummyAuth local.
2. **Implement data identity continuity** with environment-specific legacy email lookup and permanent Auth0 `(iss, sub)` -> `environmentId` resolution. Preserve all sandbox/report keys, cover every call site, and protect mapping writes against collisions/concurrency. Disabled source users excluded; email exceptions handled manually.
3. **Prepare scripts/tests/docs** for dry-run migration, controlled writes, mapping verification, security regression tests, DT setup, backups and rollback. Do not write live mappings without approval.
4. **Review CDK differences** and retain Cognito resources while changing provider authorizers. Avoid production-impacting Auth0 tenant-wide actions from the Conformance stack.
5. **Verify DT end-to-end** after Alan supplies configuration and authorizes staging accounts/grants. Then perform separately approved dev and production rollout.
6. **Later cleanup**: retire Cognito after an agreed stabilization/rollback period through a separate review; revisit per-standard grants or organization-scoped workspaces only as future business requirements.

### G3. Scope boundaries

- **In scope:** Conformance interactive SSO migration, backend API authorization, legacy user-to-data mapping, Angular access-denied page, temporary provider coexistence, testing and deployment preparation.
- **Out of scope this release:** transfer of Cognito passwords/accounts into Auth0; org-owned/shared sandboxes; per-standard restrictions; delegated user/role administration; browser-local Auth0 configuration; global logout/back-channel single sign-off; replacing machine API-key authentication; new production-to-DT data copy; unrelated Conformance refactors.
- **Optional unrelated environments:** the personal AWS `DCSA-TechTeam-PC` and the development Auth0 lab in `DCSA-Auth0-Dev` are available for experiments, but are not prerequisites for DT migration and should not be treated as the target CIAM staging setup.

## H. Evidence and read-only commands

### H1. Source references

Paths below are repository-relative for portability. Reference repository paths refer to the sibling `reference-dcsa-org` checkout. Line ranges describe the inspected checkout, not immutable commit references.

| ID | File/class/configuration | Relevant evidence |
|---|---|---|
| CG1 | [CognitoAuth](../../webui/src/app/auth/cognito-auth.ts), lines 12–146; [package.json](../../webui/package.json) | SDK, login/session/logout, ID token |
| CG2 | [AuthService](../../webui/src/app/auth/auth.service.ts), lines 11–48; [LoginComponent](../../webui/src/app/pages/login/login.component.ts) | Provider selection, embedded credentials |
| CG3 | [DummyAuth](../../webui/src/app/auth/dummy-auth.ts) | Local flags and synthetic identity token |
| CG4 | [ApiService](../../webui/src/app/service/api.service.ts), lines 21–43 | Authorization header and POST API |
| CG5 | [ConformanceStack](../../cdk/src/main/java/org/dcsa/conformance/cdk/ConformanceStack.java), lines 127–172, 204–264, 334–375, 404–412 | Cognito, DynamoDB, Lambda IAM, authorizer, CSP |
| CG6 | [WebuiLambda](../../lambda/src/main/java/org/dcsa/conformance/lambda/WebuiLambda.java), lines 25–56 | Event logging; `cognito:username` |
| CG7 | [WebuiAccessChecker](../../lambda/src/main/java/org/dcsa/conformance/lambda/WebuiAccessChecker.java), lines 14–27 | Identity/environment and sandbox checks |
| CG8 | [ConformanceWebuiHandler](../../sandbox/src/main/java/org/dcsa/conformance/sandbox/ConformanceWebuiHandler.java), lines 64–184, 187–420, 490–677 | Operations, credentials, resource checks, direct identity use |
| CG9 | [ConformanceSandbox](../../sandbox/src/main/java/org/dcsa/conformance/sandbox/ConformanceSandbox.java), lines 267–314, 484–558, 1128–1288 | Machine auth, reports, index keys, admin operations |
| CG10 | [Cognito helpers](../../scripts/src/aws/cognito.ts); [list-cognito-users](../../scripts/src/scripts/list-cognito-users.ts); [get-users-and-sandboxes](../../scripts/src/scripts/get-users-and-sandboxes.ts); [scripts README](../../scripts/README.md) | Directory reads, email-domain reporting, username joins, naming discrepancy |
| CG11 | [AppRoutingModule](../../webui/src/app/app-routing.module.ts); [EnvironmentComponent](../../webui/src/app/pages/environment/environment.component.ts), lines 34–45 | Frontend authentication/navigation |
| CG12 | [AdminLambda](../../lambda/src/main/java/org/dcsa/conformance/lambda/AdminLambda.java) | IAM-side admin entry point |
| CG13 | [ConformanceApplication](../../spring-boot/src/main/java/org/dcsa/conformance/springboot/ConformanceApplication.java), lines 71–90 | Local full-access checker |
| CG14 | [SupportedStandard](../../sandbox/src/main/java/org/dcsa/conformance/sandbox/SupportedStandard.java); [PINT scenarios](../../specifications/confluence-sync/pint/3.0.0/conformance-scenarios.md), lines 9, 23–24 | Supported modules and protocol roles, not Auth0 entitlements |
| CG15 | [ConformanceApp](../../cdk/src/main/java/org/dcsa/conformance/cdk/ConformanceApp.java); [frontend environments](../../webui/src/environments) | Region, stacks, pool/client IDs, prod placeholder |
| R1 | `apps/cdk/bin/reference-app.ts`, especially lines 147–217 | Public versus Snapshot profiles and Auth0 environments |
| R2 | `apps/cdk/lib/access-mode.ts`, lines 441–560 | Transaction/audit tables, regional/edge handlers, secret reference |
| R3 | `apps/cdk/lib/auth/regional/login-handler.js`, `callback-handler.js`, `index.js`, `logout-handler.js` | Code flow, state/nonce, secret exchange, local logout, safe logging |
| R4 | `apps/cdk/lib/auth/shared/jwt.js`, `cookies.js` | JWT validation/cache and cookie flags |
| R5 | `apps/cdk/lib/auth/edge/edge-guard.js`, lines 85–119 | Valid-user gate; claims for audit; no permission check |
| R6 | `apps/cdk/test/auth-*.test.ts`, `auth0-access-mode.test.ts` | Existing test patterns; not executed here |
| R7 | `.agents/snapshot-access-audit.md`, lines 5–23 | No authoritative organization in audit; logging/privacy boundary |

Local non-credential AWS profile configuration was inspected at `~/.aws/config`. Credential files, SSO caches, Auth0 secrets, and runtime environment secret values were not inspected. No Auth0 Management API call was made.

### H2. Executed read-only command inventory

The commands below summarize the actual successful calls, with environment variables replacing repeated literal identifiers. All AWS calls used `--profile cnf-<environment> --region eu-north-1 --output json --no-cli-pager`. Profiles/resource IDs are in B1. Output selections were used to avoid displaying account identifiers, secret-bearing fields, and user values.

| Executed operation | Scope / output handling |
|---|---|
| `aws sts get-caller-identity --query 'length(Account) > \`0\`'` | Boolean access check per profile; no identity output. The backticks are unescaped in the actual shell command below. |
| `aws cognito-idp describe-user-pool --user-pool-id "$pool" --query 'UserPool.{...}'` | Selected policies, schema, domains, verification/recovery, trigger names; not a raw dump. |
| `aws cognito-idp list-user-pool-clients --user-pool-id "$pool" --query 'UserPoolClients[].{Id:ClientId,Name:ClientName}'` | Client metadata, not `DescribeUserPoolClient`. |
| `aws cognito-idp list-groups --user-pool-id "$pool" --query 'Groups[].{Name:GroupName,Precedence:Precedence,HasIAMRole:RoleArn != \`null\`}'` | Group metadata; no groups returned. |
| `aws cognito-idp list-identity-providers --user-pool-id "$pool" --query 'Providers[].{Name:ProviderName,Type:ProviderType}'` | No providers returned. |
| `aws cognito-idp list-resource-servers --user-pool-id "$pool" --page-size 50 --query 'ResourceServers[].{Identifier:Identifier,Scopes:Scopes[].ScopeName}'` | No resource servers returned. |
| `aws cognito-identity list-identity-pools --max-results 60 --query 'IdentityPools[].{Id:IdentityPoolId,Name:IdentityPoolName}'` | No pools returned in each account/region. |
| `aws cognito-idp list-users --user-pool-id "$pool"` | Automatic pagination; captured by Python/in-memory pipeline. Only counts/statuses/attribute names/overlap printed; no user export. |
| `aws cloudformation list-stack-resources --stack-name "$stack" --query 'StackResourceSummaries[?...].{Type:ResourceType,Logical:LogicalResourceId,Physical:PhysicalResourceId}'` | Selected DynamoDB, API Gateway, and Cognito client metadata. |
| `aws cloudformation get-template --stack-name "$stack" --query "TemplateBody.Resources.$logical.Properties"` | Selected deployed App Client declaration; no runtime secret resolution. |
| `aws apigatewayv2 get-authorizers --api-id "$api" --query 'Items[].{Id:AuthorizerId,Type:AuthorizerType,IdentitySource:IdentitySource,JWT:JwtConfiguration}'` | JWT issuer/audience/source. |
| `aws apigatewayv2 get-routes --api-id "$api" --query 'Items[].{Route:RouteKey,AuthorizationType:AuthorizationType,Authorizer:AuthorizerId,Scopes:AuthorizationScopes}'` | Route protection and scopes. |
| `aws dynamodb describe-table --table-name "$table" --query 'Table.{Name:TableName,ApproximateItems:ItemCount,Bytes:TableSizeBytes,Keys:KeySchema}'` | Table metadata only. |
| `aws dynamodb query --table-name "$table" --key-condition-expression 'PK = :p AND begins_with(SK, :s)' --expression-attribute-values "$values" --projection-expression SK --page-size 50` | `:p=environment#<username>`; `:s=sandbox#` or `report#digest#`. Values generated in memory, results aggregated; automatic pagination; no scans. |

App Client CloudFormation logical IDs used: `devUserPooldevUserPoolClient65EAF212`, `dtUserPooldtUserPoolClient3566D3D7`, `testUserPooltestUserPoolClientA4522541`.

Representative exact safe access check:

```zsh
aws sts get-caller-identity --profile cnf-test --region eu-north-1 \
  --query 'length(Account) > `0`' --output json --no-cli-pager
```

Representative executed aggregate user check (not a personal-data export):

```zsh
aws cognito-idp list-users --profile cnf-test --region eu-north-1 \
  --user-pool-id eu-north-1_VRXXJfTsv \
  --query 'Users[].{status:UserStatus,enabled:Enabled,attributes:Attributes[].Name}' \
  --output json --no-cli-pager |
python3 -c 'import collections,json,sys; u=json.load(sys.stdin); print(json.dumps({"total":len(u),"statuses":dict(collections.Counter(x.get("status") for x in u)),"enabled":dict(collections.Counter(str(x.get("enabled")) for x in u)),"attribute_presence":dict(collections.Counter(a for x in u for a in set(x.get("attributes") or [])))},indent=2))'
```

Full attribute values/identifiers were used only inside in-memory aggregation for username/sub equality, normalized email overlap, enabled-state conflicts, and key lookup. Database calls were sequential or at most two workers, limited to known-user index/digest ranges. Do not interpret the key projection as eliminating read capacity charges; no table-wide scan was used.

**Execution limitations/errors:** Initial dev/DT SSO checks failed with expired sessions, then succeeded after operator login. `ListResourceServers` initially rejected an implicit zero page size; explicitly using `--page-size 50` succeeded. Incomplete terminal pastes were canceled before execution and retried with shorter commands. None of these failures changed resources or permissions.

### H3. What remains unverified or changed since the October 16 inspection

- **New deployment configuration pending Alan:** actual staging/production issuer domains, three SPA client IDs, three API audiences, role/permission/organization assignments, token lifetimes, SDK-compatible login settings, and `/userinfo` claim behavior. The selected design is not yet proof of live deployment.
- **Historical Cognito inventory:** the October 16 counts in B5/B6 are **not live** as of October 22; two extra DT test users were subsequently created and their current status and data were not freshly inspected. Cognito App Client-only settings/drift remain unverified.
- **Database completeness:** orphaned/deleted-user partitions, complete report contents, actual sandbox-integrity checks and migrated-user identity mappings were not verified in this assessment.
- **Original source-only findings:** actual deployed Reference session/authorization behavior and current Auth0 tenant Actions/SaaStart workflows are not validated by the October 16 repository inspection. Relevant CIAM design documents were reviewed subsequently, but intended configuration is not equivalent to a live tenant audit.
- **Runtime tests:** no new code, deployed Auth0 integration, `/userinfo` retrieval, identity mapping writes, or production validation is claimed by this document.

**Updated conclusion (October 22):** Implement an Angular Auth0 SPA with mandatory API permission and org context, preserve personal sandbox/report ownership through an environment-specific `(iss, sub) -> environmentId` mapping, and keep Cognito temporarily available for rollback. Do not add role tiers, per-standard restrictions, or organization-shared workspaces. Alan's registration settings are pending, so implementation can proceed locally, but AWS deployment and real data migration remain controlled, separate stages.
