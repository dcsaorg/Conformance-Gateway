import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import { mkdtemp, readFile } from 'node:fs/promises';
import { createServer, Server } from 'node:http';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { afterEach, test } from 'node:test';
import {
  parseArguments,
  parseRoleResults,
  runConformanceSuite,
  runConformanceSuites,
  sandboxIdFor,
  stopApplication,
  supportsOptionalNotifications,
} from './run-conformance-suite';

const SANDBOX_ID = 'booking-200-conformance-auto-all-in-one';
const TOKEN = 'local-token';
const servers: Server[] = [];

function report(statuses: Array<[string, string]>): string {
  return statuses
    .map(
      ([role, status]) =>
        `<h2>${role} conformance</h2><details open><summary>✅ ${status} </summary></details>`,
    )
    .join('\n');
}

async function mockGateway(
  statuses: unknown[],
  htmlReport: string,
  statusDelayMs = 0,
): Promise<{ baseUrl: string; requests: string[] }> {
  const requests: string[] = [];
  const server = createServer((request, response) => {
    const url = request.url ?? '';
    requests.push(url);
    response.statusCode = 200;
    if (url === '/') {
      response.setHeader('content-type', 'text/html');
      response.end(`<a href="/conformance/${TOKEN}/sandbox/${SANDBOX_ID}/reset">Reset</a>`);
    } else if (url.includes('/reset')) {
      response.end('{}');
    } else if (url.endsWith('/status')) {
      const statusBody = JSON.stringify({ scenariosLeft: statuses.shift() ?? 0 });
      if (statusDelayMs > 0) setTimeout(() => response.end(statusBody), statusDelayMs);
      else response.end(statusBody);
    } else if (url.endsWith('/report')) {
      response.setHeader('content-type', 'text/html');
      response.end(htmlReport);
    } else {
      response.statusCode = 404;
      response.end('not found');
    }
  });
  servers.push(server);
  await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  if (!address || typeof address === 'string') throw new Error('Mock server has no TCP address');
  return { baseUrl: `http://127.0.0.1:${address.port}`, requests };
}

afterEach(async () => {
  await Promise.all(
    servers.splice(0).map(server => new Promise<void>(resolve => server.close(() => resolve()))),
  );
});

test('derives the same all-in-one sandbox identifier as the Java application', () => {
  assert.equal(
    sandboxIdFor('Example + Standard', '2.0.0+3.0.0', 'Conformance'),
    'example+standard-200+300-conformance-auto-all-in-one',
  );
  assert.equal(
    sandboxIdFor('eBL', '3.0.0', 'Conformance TD'),
    'ebl-300-conformance-td-auto-all-in-one',
  );
});

test('parses CLI selection and rejects incomplete selectors', () => {
  const options = parseArguments([
    '--standard', 'Booking', '--version', '2.0.0', '--suite', 'Conformance',
  ]);
  assert.equal(options?.sandboxId, SANDBOX_ID);
  assert.equal(options?.notificationMode, 'both');
  assert.equal(
    parseArguments([
      '--standard', 'eBL', '--version', '3.0.0', '--suite', 'Conformance TD',
    ])?.notificationMode,
    'both',
  );
  assert.equal(
    parseArguments([
      '--sandbox-id', 'ovs-300-conformance-auto-all-in-one',
    ])?.notificationMode,
    'with',
  );
  assert.equal(supportsOptionalNotifications(SANDBOX_ID), true);
  assert.equal(supportsOptionalNotifications('ovs-300-conformance-auto-all-in-one'), false);
  assert.throws(() => parseArguments(['--standard', 'Booking']), /Provide --sandbox-id/);
  assert.throws(
    () => parseArguments(['--sandbox-id', SANDBOX_ID, '--standard', 'Booking']),
    /either --sandbox-id/,
  );
});

test('parses top-level role statuses only', () => {
  const html = `${report([['Carrier', 'CONFORMANT'], ['Shipper', 'NON-CONFORMANT']])}
    <h5>✅ nested detail (CONFORMANT)</h5>`;
  assert.deepEqual(parseRoleResults(html), [
    { role: 'Carrier', status: 'CONFORMANT' },
    { role: 'Shipper', status: 'NON-CONFORMANT' },
  ]);
});

test('rejects a malformed top-level role result even when another role is conformant', () => {
  const html = `${report([['Carrier', 'CONFORMANT']])}
    <h2>Shipper conformance</h2><details open><summary>✅ UNKNOWN </summary></details>`;

  assert.throws(() => parseRoleResults(html), /Malformed or missing top-level role result: Shipper/);
});

test('runs reset, polls completion, validates roles, and saves the HTML report', async () => {
  const htmlReport = report([['Carrier', 'CONFORMANT'], ['Shipper', 'CONFORMANT']]);
  const gateway = await mockGateway([2, 2, 1, 0], htmlReport);
  const directory = await mkdtemp(path.join(tmpdir(), 'conformance-runner-'));
  const outputPath = path.join(directory, 'booking.html');

  const result = await runConformanceSuite({
    baseUrl: gateway.baseUrl,
    sandboxId: SANDBOX_ID,
    outputPath,
    timeoutMs: 2_000,
    pollIntervalMs: 5,
  });

  assert.deepEqual(result.roles, [
    { role: 'Carrier', status: 'CONFORMANT' },
    { role: 'Shipper', status: 'CONFORMANT' },
  ]);
  assert.equal(await readFile(outputPath, 'utf8'), htmlReport);
  assert.equal(gateway.requests.filter(url => url.endsWith('/reset')).length, 1);
  assert.equal(gateway.requests.filter(url => url.endsWith('/status')).length, 4);
  assert.equal(gateway.requests.filter(url => url.endsWith('/report')).length, 1);
});

test('runs optional-notification suites in both modes and saves distinct reports', async () => {
  const htmlReport = report([
    ['Carrier', 'CONFORMANT'],
    ['Shipper', 'COMPLETED WITHOUT OPTIONAL TRAFFIC'],
  ]);
  const gateway = await mockGateway([0, 0], htmlReport);
  const directory = await mkdtemp(path.join(tmpdir(), 'conformance-runner-'));
  const outputPath = path.join(directory, 'booking.html');

  const results = await runConformanceSuites({
    baseUrl: gateway.baseUrl,
    sandboxId: SANDBOX_ID,
    outputPath,
    timeoutMs: 2_000,
    pollIntervalMs: 5,
    notificationMode: 'both',
  });

  assert.deepEqual(results.map(result => result.notificationMode), ['with', 'without']);
  assert.deepEqual(
    results.map(result => path.basename(result.outputPath)),
    ['booking-with-notifications.html', 'booking-without-notifications.html'],
  );
  assert.equal(await readFile(results[0].outputPath, 'utf8'), htmlReport);
  assert.equal(await readFile(results[1].outputPath, 'utf8'), htmlReport);
  assert.equal(gateway.requests.filter(url => url.endsWith('/reset')).length, 1);
  assert.equal(
    gateway.requests.filter(url => url.endsWith('/reset?suppressNotifications=true')).length,
    1,
  );
});

test("single-result runner rejects notificationMode 'both' instead of silently running one mode", async () => {
  const directory = await mkdtemp(path.join(tmpdir(), 'conformance-runner-'));

  await assert.rejects(
    runConformanceSuite({
      baseUrl: 'http://127.0.0.1:1',
      sandboxId: SANDBOX_ID,
      outputPath: path.join(directory, 'booking.html'),
      timeoutMs: 2_000,
      pollIntervalMs: 5,
      notificationMode: 'both',
    }),
    /use runConformanceSuites/,
  );
});

test('both notification modes share one overall timeout', async () => {
  const htmlReport = report([['Carrier', 'CONFORMANT'], ['Shipper', 'CONFORMANT']]);
  const gateway = await mockGateway([0, 0], htmlReport, 50);
  const directory = await mkdtemp(path.join(tmpdir(), 'conformance-runner-'));
  const startedAt = Date.now();

  await assert.rejects(
    runConformanceSuites({
      baseUrl: gateway.baseUrl,
      sandboxId: SANDBOX_ID,
      outputPath: path.join(directory, 'booking.html'),
      timeoutMs: 80,
      pollIntervalMs: 5,
      notificationMode: 'both',
    }),
    /Overall timeout reached/,
  );
  assert.ok(Date.now() - startedAt < 400);
});

test('saves a failing report and rejects the run', async () => {
  const htmlReport = report([['Carrier', 'CONFORMANT'], ['Shipper', 'NON-CONFORMANT']]);
  const gateway = await mockGateway([0], htmlReport);
  const directory = await mkdtemp(path.join(tmpdir(), 'conformance-runner-'));
  const outputPath = path.join(directory, 'failed.html');

  await assert.rejects(
    runConformanceSuite({
      baseUrl: gateway.baseUrl,
      sandboxId: SANDBOX_ID,
      outputPath,
      timeoutMs: 2_000,
      pollIntervalMs: 5,
    }),
    /Shipper: NON-CONFORMANT/,
  );
  assert.equal(await readFile(outputPath, 'utf8'), htmlReport);
});

test('rejects and saves a report containing only one role result', async () => {
  const htmlReport = report([['Carrier', 'CONFORMANT']]);
  const gateway = await mockGateway([0], htmlReport);
  const directory = await mkdtemp(path.join(tmpdir(), 'conformance-runner-'));
  const outputPath = path.join(directory, 'missing-role.html');

  await assert.rejects(
    runConformanceSuite({
      baseUrl: gateway.baseUrl,
      sandboxId: SANDBOX_ID,
      outputPath,
      timeoutMs: 2_000,
      pollIntervalMs: 5,
    }),
    /Expected 2 top-level role results but parsed 1/,
  );
  assert.equal(await readFile(outputPath, 'utf8'), htmlReport);
});

test('rejects an invalid scenariosLeft polling status', async () => {
  const gateway = await mockGateway(['not-a-number'], report([
    ['Carrier', 'CONFORMANT'],
    ['Shipper', 'CONFORMANT'],
  ]));
  const directory = await mkdtemp(path.join(tmpdir(), 'conformance-runner-'));

  await assert.rejects(
    runConformanceSuite({
      baseUrl: gateway.baseUrl,
      sandboxId: SANDBOX_ID,
      outputPath: path.join(directory, 'invalid-status.html'),
      timeoutMs: 2_000,
      pollIntervalMs: 5,
    }),
    /Invalid scenariosLeft status/,
  );
});

test('a stalled status request cannot exceed the overall run deadline', async () => {
  const gateway = await mockGateway([0], report([
    ['Carrier', 'CONFORMANT'],
    ['Shipper', 'CONFORMANT'],
  ]), 500);
  const directory = await mkdtemp(path.join(tmpdir(), 'conformance-runner-'));
  const startedAt = Date.now();

  await assert.rejects(
    runConformanceSuite({
      baseUrl: gateway.baseUrl,
      sandboxId: SANDBOX_ID,
      outputPath: path.join(directory, 'timeout.html'),
      timeoutMs: 75,
      pollIntervalMs: 5,
    }),
    /Overall timeout reached while requesting/,
  );
  assert.ok(Date.now() - startedAt < 400);
});

test('rejects a missing sandbox without resetting another suite', async () => {
  const gateway = await mockGateway([0], report([['Carrier', 'CONFORMANT']]));
  const directory = await mkdtemp(path.join(tmpdir(), 'conformance-runner-'));

  await assert.rejects(
    runConformanceSuite({
      baseUrl: gateway.baseUrl,
      sandboxId: 'missing-auto-all-in-one',
      outputPath: path.join(directory, 'missing.html'),
      timeoutMs: 2_000,
      pollIntervalMs: 5,
    }),
    /was not found/,
  );
  assert.equal(gateway.requests.some(url => url.endsWith('/reset')), false);
});

test('waits for a managed application to exit after SIGKILL', async () => {
  const child = spawn(
    process.execPath,
    ['-e', "process.on('SIGTERM', () => {}); console.log('ready'); setInterval(() => {}, 1000)"],
    {
      detached: process.platform !== 'win32',
      stdio: ['ignore', 'pipe', 'ignore'],
    },
  );

  try {
    await once(child.stdout!, 'data');
    await stopApplication(child, 20);
    assert.notEqual(child.signalCode, null);
  } finally {
    if (child.exitCode === null && child.signalCode === null) child.kill('SIGKILL');
  }
});


