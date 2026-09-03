import { ChildProcess, spawn } from 'child_process';
import { mkdir, writeFile } from 'fs/promises';
import * as path from 'path';

export interface RunnerOptions {
  baseUrl: string;
  sandboxId: string;
  outputPath: string;
  timeoutMs: number;
  pollIntervalMs: number;
  startCommand?: string;
  notificationMode?: NotificationMode;
}

export type NotificationMode = 'with' | 'without' | 'both';

export interface RoleResult {
  role: string;
  status: string;
}

export interface RunnerResult {
  sandboxId: string;
  outputPath: string;
  durationMs: number;
  roles: RoleResult[];
  notificationMode: Exclude<NotificationMode, 'both'>;
}

const PASSING_STATUSES = new Set(['CONFORMANT', 'COMPLETED WITHOUT OPTIONAL TRAFFIC']);
const TOP_LEVEL_ROLE_PATTERN = /<h2>(.*?) conformance<\/h2>/g;
const STATUS_PATTERN =
  /<h2>(.*?) conformance<\/h2><details open><summary>.*? (CONFORMANT|PARTIALLY CONFORMANT|COMPLETED WITHOUT OPTIONAL TRAFFIC|SKIPPED|NON-CONFORMANT|IRRELEVANT|NO TRAFFIC) <\/summary>/g;
const REQUEST_TIMEOUT_MS = 30_000;
const EXPECTED_ALL_IN_ONE_ROLE_RESULTS = 2;
const OPTIONAL_NOTIFICATION_SANDBOX_PREFIXES = ['booking-', 'ebl-'];

function usage(): string {
  return `Run one local all-in-one conformance suite and save its HTML report.

Usage:
  npm run run-conformance-suite -- --standard <name> --version <version> --suite <suite> [options]
  npm run run-conformance-suite -- --sandbox-id <id> [options]

Options:
  --base-url <url>          Gateway URL (default: http://localhost:8080)
  --output <file>           HTML output path (notification mode suffixes are added for two reports)
  --notification-mode <m>  with, without, both, or auto (default: auto)
  --timeout-seconds <n>     Overall startup/execution timeout (default: 900)
  --poll-interval-ms <n>    Status polling interval (default: 500)
  --start-command <command> Start the application, then stop it after the run
  --help                    Show this help
`;
}

export function sandboxIdFor(standard: string, version: string, suite: string): string {
  const standardPart = standard.replace(/ /g, '').toLowerCase();
  const versionPart = version.replace(/[.-]/g, '').toLowerCase();
  const suitePart = suite.replace(/ /g, '-').toLowerCase();
  return `${standardPart}-${versionPart}-${suitePart}-auto-all-in-one`;
}

export function parseRoleResults(report: string): RoleResult[] {
  const roleHeadings = Array.from(report.matchAll(TOP_LEVEL_ROLE_PATTERN), match => match[1]);
  const results = Array.from(report.matchAll(STATUS_PATTERN), match => ({
    role: match[1],
    status: match[2],
  }));
  if (roleHeadings.length !== results.length) {
    const parsedRoles = new Set(results.map(result => result.role));
    const malformedRoles = roleHeadings.filter(role => !parsedRoles.has(role));
    throw new Error(
      `Malformed or missing top-level role result${malformedRoles.length === 1 ? '' : 's'}: ` +
        (malformedRoles.join(', ') || 'unknown role'),
    );
  }
  if (new Set(results.map(result => result.role)).size !== results.length) {
    throw new Error('Report contains duplicate top-level role results');
  }
  return results;
}

async function requestText(url: string, deadline: number): Promise<string> {
  const remainingMs = deadline - Date.now();
  if (remainingMs <= 0) throw new Error(`Overall timeout reached before requesting ${url}`);
  const requestTimeoutMs = Math.min(REQUEST_TIMEOUT_MS, remainingMs);
  let response: Response;
  try {
    response = await fetch(url, { signal: AbortSignal.timeout(requestTimeoutMs) });
  } catch (error) {
    if (Date.now() >= deadline) throw new Error(`Overall timeout reached while requesting ${url}`);
    throw error;
  }
  const body = await response.text();
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} from ${url}: ${body.slice(0, 500)}`);
  }
  return body;
}

function endpoint(baseUrl: string, token: string, sandboxId: string, operation: string): string {
  return `${baseUrl}/conformance/${encodeURIComponent(token)}/sandbox/${encodeURIComponent(sandboxId)}/${operation}`;
}

function outputPathForMode(
  outputPath: string,
  mode: Exclude<NotificationMode, 'both'>,
  addModeSuffix: boolean,
): string {
  if (!addModeSuffix) return outputPath;
  const extension = path.extname(outputPath);
  const base = extension ? outputPath.slice(0, -extension.length) : outputPath;
  return `${base}-${mode}-notifications${extension || '.html'}`;
}

export function supportsOptionalNotifications(sandboxId: string): boolean {
  return OPTIONAL_NOTIFICATION_SANDBOX_PREFIXES.some(prefix => sandboxId.startsWith(prefix));
}

function discoverToken(homepage: string, sandboxId: string): string {
  const escapedSandboxId = sandboxId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = homepage.match(
    new RegExp(`/conformance/([^/]+)/sandbox/${escapedSandboxId}/reset`),
  );
  if (!match) {
    throw new Error(
      `Auto all-in-one sandbox '${sandboxId}' was not found on the gateway homepage. ` +
        `Check --standard/--version/--suite and that auto-testing sandboxes are enabled.`,
    );
  }
  return decodeURIComponent(match[1]);
}

async function waitForGateway(baseUrl: string, deadline: number): Promise<string> {
  let lastError: unknown;
  while (Date.now() < deadline) {
    try {
      return await requestText(`${baseUrl}/`, deadline);
    } catch (error) {
      lastError = error;
      await sleep(Math.min(250, Math.max(0, deadline - Date.now())));
    }
  }
  throw new Error(`Gateway did not become ready at ${baseUrl}: ${String(lastError)}`);
}

async function waitForCompletion(
  baseUrl: string,
  token: string,
  sandboxId: string,
  deadline: number,
  pollIntervalMs: number,
): Promise<void> {
  let previousScenariosLeft: number | undefined;
  while (Date.now() < deadline) {
    const body = await requestText(endpoint(baseUrl, token, sandboxId, 'status'), deadline);
    let scenariosLeft: unknown;
    try {
      scenariosLeft = (JSON.parse(body) as { scenariosLeft?: unknown }).scenariosLeft;
    } catch {
      throw new Error(`Invalid status JSON for '${sandboxId}': ${body}`);
    }
    if (!Number.isInteger(scenariosLeft) || (scenariosLeft as number) < 0) {
      throw new Error(`Invalid scenariosLeft status for '${sandboxId}': ${body}`);
    }
    if (scenariosLeft === 0) return;
    if (scenariosLeft !== previousScenariosLeft) {
      console.log(`Scenarios remaining: ${scenariosLeft}`);
      previousScenariosLeft = scenariosLeft as number;
    }
    await sleep(Math.min(pollIntervalMs, Math.max(0, deadline - Date.now())));
  }
  throw new Error(`Timed out waiting for '${sandboxId}' to complete`);
}

function sleep(milliseconds: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, milliseconds));
}

function startApplication(command: string): ChildProcess {
  const repositoryRoot = path.resolve(__dirname, '../../..');
  console.log(`Starting application with: ${command}`);
  return spawn(command, {
    cwd: repositoryRoot,
    shell: true,
    stdio: 'inherit',
    detached: process.platform !== 'win32',
  });
}

export async function stopApplication(
  child: ChildProcess | undefined,
  gracePeriodMs = 5_000,
): Promise<void> {
  if (!child || child.exitCode !== null || child.signalCode !== null || child.pid === undefined) return;
  console.log('Stopping application...');
  const exited = new Promise<void>(resolve => child.once('exit', () => resolve()));
  if (process.platform === 'win32') child.kill('SIGTERM');
  else process.kill(-child.pid, 'SIGTERM');
  const stoppedDuringGracePeriod = await Promise.race([
    exited.then(() => true),
    sleep(gracePeriodMs).then(() => false),
  ]);
  if (stoppedDuringGracePeriod) return;
  if (child.exitCode !== null || child.signalCode !== null) return;
  if (process.platform === 'win32') child.kill('SIGKILL');
  else process.kill(-child.pid, 'SIGKILL');
  await exited;
}

async function runConformanceSuiteMode(
  options: RunnerOptions,
  notificationMode: Exclude<NotificationMode, 'both'>,
  outputPath: string,
): Promise<RunnerResult> {
  const startedAt = Date.now();
  const deadline = startedAt + options.timeoutMs;
  const homepage = await waitForGateway(options.baseUrl, deadline);
  const token = discoverToken(homepage, options.sandboxId);

  console.log(`Running conformance suite (${notificationMode} notifications): ${options.sandboxId}`);
  const resetOperation =
    notificationMode === 'without' ? 'reset?suppressNotifications=true' : 'reset';
  await requestText(endpoint(options.baseUrl, token, options.sandboxId, resetOperation), deadline);
  await waitForCompletion(
    options.baseUrl,
    token,
    options.sandboxId,
    deadline,
    options.pollIntervalMs,
  );

  const report = await requestText(
    endpoint(options.baseUrl, token, options.sandboxId, 'report'),
    deadline,
  );
  await mkdir(path.dirname(outputPath), { recursive: true });
  await writeFile(outputPath, report, 'utf8');
  const roles = parseRoleResults(report);
  if (roles.length === 0) {
    throw new Error(`Report contains no top-level role results; saved diagnostic report to ${outputPath}`);
  }
  if (roles.length !== EXPECTED_ALL_IN_ONE_ROLE_RESULTS) {
    throw new Error(
      `Expected ${EXPECTED_ALL_IN_ONE_ROLE_RESULTS} top-level role results but parsed ${roles.length}; ` +
        `saved diagnostic report to ${outputPath}`,
    );
  }
  const failures = roles.filter(result => !PASSING_STATUSES.has(result.status));
  if (failures.length > 0) {
    throw new Error(
      `Conformance failed (${failures.map(result => `${result.role}: ${result.status}`).join(', ')}); ` +
        `report saved to ${outputPath}`,
    );
  }

  return {
    sandboxId: options.sandboxId,
    outputPath,
    durationMs: Date.now() - startedAt,
    roles,
    notificationMode,
  };
}

export async function runConformanceSuite(options: RunnerOptions): Promise<RunnerResult> {
  let application: ChildProcess | undefined;
  try {
    if (options.startCommand) application = startApplication(options.startCommand);
    const mode = options.notificationMode === 'without' ? 'without' : 'with';
    return await runConformanceSuiteMode(options, mode, options.outputPath);
  } finally {
    await stopApplication(application);
  }
}

export async function runConformanceSuites(options: RunnerOptions): Promise<RunnerResult[]> {
  let application: ChildProcess | undefined;
  try {
    if (options.startCommand) application = startApplication(options.startCommand);
    const notificationMode = options.notificationMode ?? 'with';
    const modes: Array<Exclude<NotificationMode, 'both'>> =
      notificationMode === 'both' ? ['with', 'without'] : [notificationMode];
    const results: RunnerResult[] = [];
    for (const mode of modes) {
      results.push(
        await runConformanceSuiteMode(
          options,
          mode,
          outputPathForMode(options.outputPath, mode, modes.length > 1),
        ),
      );
    }
    return results;
  } finally {
    await stopApplication(application);
  }
}

function requiredValue(args: string[], index: number, option: string): string {
  const value = args[index + 1];
  if (!value || value.startsWith('--')) throw new Error(`Missing value for ${option}`);
  return value;
}

export function parseArguments(args: string[]): RunnerOptions | undefined {
  let baseUrl = 'http://localhost:8080';
  let standard: string | undefined;
  let version: string | undefined;
  let suite: string | undefined;
  let sandboxId: string | undefined;
  let outputPath: string | undefined;
  let timeoutMs = 900_000;
  let pollIntervalMs = 500;
  let startCommand: string | undefined;
  let requestedNotificationMode: NotificationMode | 'auto' = 'auto';

  for (let index = 0; index < args.length; index += 1) {
    const option = args[index];
    if (option === '--help') return undefined;
    const value = requiredValue(args, index, option);
    index += 1;
    switch (option) {
      case '--base-url': baseUrl = value.replace(/\/$/, ''); break;
      case '--standard': standard = value; break;
      case '--version': version = value; break;
      case '--suite': suite = value; break;
      case '--sandbox-id': sandboxId = value; break;
      case '--output': outputPath = path.resolve(value); break;
      case '--timeout-seconds': timeoutMs = Number(value) * 1_000; break;
      case '--poll-interval-ms': pollIntervalMs = Number(value); break;
      case '--start-command': startCommand = value; break;
      case '--notification-mode':
        if (!['auto', 'with', 'without', 'both'].includes(value)) {
          throw new Error('--notification-mode must be auto, with, without, or both');
        }
        requestedNotificationMode = value as NotificationMode | 'auto';
        break;
      default: throw new Error(`Unknown option: ${option}`);
    }
  }

  if (sandboxId && (standard || version || suite)) {
    throw new Error('Use either --sandbox-id or --standard/--version/--suite, not both');
  }
  if (!sandboxId) {
    if (!standard || !version || !suite) {
      throw new Error('Provide --sandbox-id or all of --standard, --version, and --suite');
    }
    sandboxId = sandboxIdFor(standard, version, suite);
  }
  if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) throw new Error('--timeout-seconds must be positive');
  if (!Number.isFinite(pollIntervalMs) || pollIntervalMs <= 0) {
    throw new Error('--poll-interval-ms must be positive');
  }
  const notificationMode =
    requestedNotificationMode === 'auto'
      ? supportsOptionalNotifications(sandboxId) ? 'both' : 'with'
      : requestedNotificationMode;

  return {
    baseUrl,
    sandboxId,
    outputPath:
      outputPath ?? path.resolve(__dirname, '../../../target/conformance-reports', `${sandboxId}.html`),
    timeoutMs,
    pollIntervalMs,
    startCommand,
    notificationMode,
  };
}

async function main(): Promise<void> {
  try {
    const options = parseArguments(process.argv.slice(2));
    if (!options) {
      console.log(usage());
      return;
    }
    const results = await runConformanceSuites(options);
    for (const result of results) {
      console.log(
        `Conformance passed (${result.notificationMode} notifications) in ` +
          `${(result.durationMs / 1_000).toFixed(1)}s: ` +
          result.roles.map(role => `${role.role}: ${role.status}`).join(', '),
      );
      console.log(`Report: ${result.outputPath}`);
    }
  } catch (error) {
    console.error(error instanceof Error ? error.message : error);
    process.exitCode = 1;
  }
}

if (require.main === module) void main();


