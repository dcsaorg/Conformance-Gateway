import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const indexPath = resolve('dist/webui/browser/index.html');
const indexHtml = readFileSync(indexPath, 'utf8');

if (/\son[a-z]+\s*=/iu.test(indexHtml)) {
  throw new Error(`CSP-incompatible inline event handler found in ${indexPath}`);
}

if (!/<link\b(?=[^>]*\brel=["']stylesheet["'])(?![^>]*\bmedia=["']print["'])[^>]*>/iu.test(indexHtml)) {
  throw new Error(`No screen-active stylesheet link found in ${indexPath}`);
}

console.log('CSP build verification passed.');
