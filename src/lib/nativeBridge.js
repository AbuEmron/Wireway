// src/lib/nativeBridge.js
// Capacitor (native Android/iOS) glue. Imported FIRST in src/index.js so its
// side effects (the fetch rewrite below) are installed before any app code runs.
//
// Why this file exists: the web app talks to its backend with RELATIVE paths
// ("/api/..."). On the web those resolve against https://www.wirewaypro.com.
// Inside the native WebView the origin is https://localhost (no server there),
// so every API call — Stripe, Claude AI, Plaid, account deletion, error logs —
// would 404. We transparently rewrite those requests to the real origin and
// expose a canonical PUBLIC_ORIGIN for share links the contractor sends out.
import { Capacitor } from '@capacitor/core';

export const isNative =
  (typeof Capacitor !== 'undefined' && Capacitor.isNativePlatform && Capacitor.isNativePlatform()) || false;

// Deployed site that hosts /api/* and the public quote / pay / appointment pages.
const API_ORIGIN = 'https://www.wirewaypro.com';

// Use for any link a user shares OUT of the app (quotes, pay-draw, referrals).
// On the web this is just the current origin; natively it must be the real site
// because https://localhost/quote/xyz is meaningless to a client.
export const PUBLIC_ORIGIN = isNative ? API_ORIGIN : window.location.origin;

// Rewrite relative /api/* fetches to the deployed origin when running natively.
if (isNative && typeof window !== 'undefined' && window.fetch && !window.__wwFetchPatched) {
  const orig = window.fetch.bind(window);
  window.fetch = (input, init) => {
    try {
      if (typeof input === 'string' && input.startsWith('/api/')) {
        input = API_ORIGIN + input;
      } else if (typeof Request !== 'undefined' && input instanceof Request) {
        const u = input.url || '';
        const idx = u.indexOf('/api/');
        // The WebView absolutizes Request URLs against https://localhost; pull
        // the /api/... tail back off and re-point it at the real backend.
        if (idx !== -1 && u.startsWith('https://localhost')) {
          input = new Request(API_ORIGIN + u.slice(idx), input);
        }
      }
    } catch (_) {
      /* fall through to the original fetch on any parsing surprise */
    }
    return orig(input, init);
  };
  window.__wwFetchPatched = true;
}

// Native chrome setup: hide the splash once React has painted, lock the status
// bar to the brand dark theme. Safe no-op on the web. Call from App on mount.
export async function initNativeApp() {
  if (!isNative) return;
  try {
    const { SplashScreen } = await import('@capacitor/splash-screen');
    const { StatusBar, Style } = await import('@capacitor/status-bar');
    try {
      await StatusBar.setStyle({ style: Style.Dark });
      // Android-only; throws a no-op rejection on iOS which we swallow.
      await StatusBar.setBackgroundColor({ color: '#0a0a0c' });
    } catch (_) { /* status bar styling is best-effort */ }
    await SplashScreen.hide();
  } catch (_) {
    /* plugins unavailable (e.g. web) — ignore */
  }
}
