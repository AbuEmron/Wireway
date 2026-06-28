import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.wirewaypro.app',
  appName: 'Wireway Pro',
  webDir: 'build',
  // The bundled web assets are served from the local WebView origin
  // (https://localhost). All backend traffic still goes out over HTTPS to
  // https://www.wirewaypro.com/api/* — see src/lib/nativeBridge.js, which
  // rewrites the app's relative "/api/..." fetches to that absolute origin.
  server: {
    androidScheme: 'https',
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 1200,
      launchAutoHide: false, // App.jsx hides it once React has mounted
      backgroundColor: '#080b10',
      androidScaleType: 'CENTER_CROP',
      showSpinner: false,
      splashFullScreen: true,
      splashImmersive: true,
    },
    StatusBar: {
      style: 'DARK', // dark content area -> light icons; matches the #0a0a0c theme
      backgroundColor: '#0a0a0c',
    },
  },
};

export default config;
