import 'jest-preset-angular/setup-env/zone';

import { getTestBed } from '@angular/core/testing';
import {
    BrowserTestingModule,
    platformBrowserTesting,
} from '@angular/platform-browser/testing';

getTestBed().initTestEnvironment(
    BrowserTestingModule,
    platformBrowserTesting(),
);