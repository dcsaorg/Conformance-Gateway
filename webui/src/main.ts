import { platformBrowser } from '@angular/platform-browser';

import { loadAuthenticationConfig } from './app/auth/auth-config';


loadAuthenticationConfig()
  .then(() => import('./app/app.module'))
  .then(({ AppModule }) => platformBrowser().bootstrapModule(AppModule))
  .catch(() => {
    document.body.textContent = 'Conformance could not start. Please reload or contact DCSA support.';
  });
