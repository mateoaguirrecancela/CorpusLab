import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Without `test.globals` in vite.config.ts, Testing Library can't
// auto-detect the test framework to register its own cleanup, so each
// component render would otherwise pile up in the DOM across tests.
afterEach(() => {
  cleanup();
});
