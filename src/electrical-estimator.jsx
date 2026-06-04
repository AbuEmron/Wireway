18:35:33.513 Running build in Washington, D.C., USA (East) – iad1
18:35:33.514 Build machine configuration: 2 cores, 8 GB
18:35:33.676 Cloning github.com/AbuEmron/Wireway (Branch: main, Commit: ae4b141)
18:35:33.996 Cloning completed: 320.000ms
18:35:34.331 Restored build cache from previous deployment (7iCNDRs11VgyJxvyHvhw9AnBjRAb)
18:35:34.526 Running "vercel build"
18:35:34.543 Vercel CLI 54.9.0
18:35:34.609 WARNING! Due to `builds` existing in your configuration file, the Build and Development Settings defined in your Project Settings will not apply. Learn More: https://vercel.link/unused-build-settings
18:35:35.149 Installing dependencies...
18:35:37.097 
18:35:37.098 up to date in 2s
18:35:37.098 
18:35:37.099 267 packages are looking for funding
18:35:37.099   run `npm fund` for details
18:35:37.137 Running "npm run build"
18:35:37.235 
18:35:37.236 > voltquote@1.0.0 build
18:35:37.236 > react-scripts build
18:35:37.236 
18:35:38.369 (node:111) [DEP0176] DeprecationWarning: fs.F_OK is deprecated, use fs.constants.F_OK instead
18:35:38.369 (Use `node --trace-deprecation ...` to show where the warning was created)
18:35:38.374 Creating an optimized production build...
18:35:44.556 
18:35:44.557 Treating warnings as errors because process.env.CI = true.
18:35:44.557 Most CI servers set it automatically.
18:35:44.557 
18:35:44.558 Failed to compile.
18:35:44.558 
18:35:44.558 [eslint] 
18:35:44.559 src/electrical-estimator.jsx
18:35:44.559   Line 882:10:  'sendModal' is assigned a value but never used        no-unused-vars
18:35:44.559   Line 882:24:  'setSendModal' is assigned a value but never used     no-unused-vars
18:35:44.559   Line 884:10:  'previewModal' is assigned a value but never used     no-unused-vars
18:35:44.559   Line 884:24:  'setPreviewModal' is assigned a value but never used  no-unused-vars
18:35:44.559 
18:35:44.559 
18:35:44.598 Error: Command "npm run build" exited with 1
