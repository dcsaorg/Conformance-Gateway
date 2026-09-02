import assert from 'node:assert/strict';
import { mkdtemp, readFile } from 'node:fs/promises';
import { createServer, Server } from 'node:http';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { afterEach, test } from 'node:test';
import {
  parseArguments,
  parseRoleResults,
  runConformanceSuite,
  sandboxIdFor,
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
  statuses: number[],
  htmlReport: string,
): Promise<{ baseUrl: string; requests: string[] }> {
  const requests: string[] = [];
  const server = createServer((request, response) => {
    const url = request.url ?? '';
    requests.push(url);
    response.statusCode = 200;
    if (url === '/') {
      response.setHeader('content-type', 'text/html');
      response.end(`<a href="/conformance/${TOKEN}/sandbox/${SANDBOX_ID}/reset">Reset</a>`);
    } else if (url.endsWith('/reset')) {
      response.end('{}');
    } else if (url.endsWith('/status')) {
      response.end(JSON.stringify({ scenariosLeft: statuses.shift() ?? 0 }));
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
    sandboxIdFor('Booking + eBL', '2.0.0+3.0.0', 'Conformance'),
    'booking+ebl-200+300-conformance-auto-all-in-one',
  );
  assert.equal(
    sandboxIdFor('eBL', '3.0.0', 'Conformance SI + TD'),
    'ebl-300-conformance-si-+-td-auto-all-in-one',
  );
});

test('parses CLI selection and rejects incomplete selectors', () => {
  const options = parseArguments([
    '--standard', 'Booking', '--version', '2.0.0', '--suite', 'Conformance',
  ]);
  assert.equal(options?.sandboxId, SANDBOX_ID);
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


