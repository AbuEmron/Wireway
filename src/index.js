import { isNative } from './lib/nativeBridge'; // MUST be first: installs the native /api fetch rewrite
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/tokens.css';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<React.StrictMode><App /></React.StrictMode>);

// Register PWA service worker — but NOT inside the native app, where a cached
// shell would shadow freshly-installed APK assets on every update.
if (!isNative && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/sw.js')
      .then((registration) => {
        console.log('VoltQuote: Service worker registered', registration);
      })
      .catch((error) => {
        console.log('VoltQuote: Service worker failed', error);
      });
  });
}
