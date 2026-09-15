# OWASP ZAP scanning

The repository provides manually triggered OWASP ZAP scans for the deployed Conformance Web UI. These scans collect evidence for security-header and passive web-security findings without modifying deployed application data.

## Workflows

`OWASP ZAP Baseline Scan` performs unauthenticated passive scanning of known Angular route entry points. It supports Dev, DT, and Test.

`OWASP ZAP Authenticated Scan` signs in through the application's existing Cognito login form, verifies the session against the Web UI API, and passively scans a small set of read-only requests. Its initial profile supports Dev only.

Neither workflow performs an active scan. The Conformance Web UI API multiplexes read and write operations through one POST endpoint, and a generic crawler or active scanner could create, update, reset, or delete sandbox data. Broader authenticated crawling and active scanning require a separately reviewed request allowlist and cleanup strategy.

## GitHub Environment configuration

Create these GitHub Environments and variables:

| GitHub Environment | `APP_URL` | `API_URL` |
| --- | --- | --- |
| `conformance-dev` | `https://dev.conformance-development-1.dcsa.org` | `https://dev-webui.conformance-development-1.dcsa.org` |
| `conformance-dt` | `https://dt.conformance-dt-1.dcsa.org` | `https://dt-webui.conformance-dt-1.dcsa.org` |
| `conformance-test` | `https://test.conformance-test-1.dcsa.org` | `https://test-webui.conformance-test-1.dcsa.org` |

Store these secrets in `conformance-dev` for the authenticated scan:

| Secret | Purpose |
| --- | --- |
| `CONFORMANCE_USER_EMAIL` | Dedicated non-production Conformance scan account |
| `CONFORMANCE_USER_PASSWORD` | Password for the scan account |

Do not store credentials in repository files or GitHub Environment variables. The account should be dedicated to security scanning and should not own valuable sandbox data.

## Running a scan

1. Open the repository's Actions page.
2. Select `OWASP ZAP Baseline Scan` or `OWASP ZAP Authenticated Scan`.
3. Choose **Run workflow** and select an allowed target environment.
4. When the run finishes, download the environment-specific HTML report from the run's artifacts.

The report is uploaded even when plan validation, authentication, or scanning fails. In that case it may contain a fallback page and the workflow log contains the failure reason without exposing secret values.

Medium findings make the ZAP plan warn without failing the job. High findings fail the job. No alert suppressions are configured initially; add a suppression only after a report demonstrates a false positive and the rule, evidence, and affected URL can all be constrained narrowly.

## Scope and limitations

The scan contexts include only the selected Conformance application origin and, for the authenticated scan, its matching Web UI API origin. Cognito and other third-party origins used while loading or authenticating the application are not part of the scan context.

The browser authentication plan intentionally uses the Cognito `AuthenticationResult.IdToken` as the API's raw `Authorization` header value, matching the Web UI implementation. Authentication diagnostics are disabled because they are unnecessary for routine runs and can contain sensitive session evidence.

A successful ZAP scan does not replace focused authentication, authorization, or access-control tests. The first Dev reports should be reviewed before extending authenticated scanning to DT or Test or enabling any active-scan behavior.
