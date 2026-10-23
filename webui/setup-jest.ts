import 'jest-preset-angular/setup-env/zone';
import { TextDecoder, TextEncoder } from 'node:util';

Object.assign(globalThis, { TextEncoder, TextDecoder });

import { getTestBed } from '@angular/core/testing';
import {
    BrowserTestingModule,
    platformBrowserTesting,
} from '@angular/platform-browser/testing';

getTestBed().initTestEnvironment(
    BrowserTestingModule,
    platformBrowserTesting(),
);
