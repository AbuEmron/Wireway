# ⚡ VoltQuote — Complete App Launch Guide
## Phase 1 (PWA) · Phase 2 (Google Play) · Phase 3 (Apple App Store)

---

# ═══════════════════════════════════════
# PHASE 1 — PWA (Progressive Web App)
# Cost: $0 · Time: Today · No approval needed
# ═══════════════════════════════════════

A PWA lets any electrician install VoltQuote directly from the browser
onto their phone home screen. It works offline, looks and feels like a
native app, and costs nothing.

## What users do to install it:

### iPhone (Safari):
1. Open voltquote.app in Safari
2. Tap the Share button (box with arrow pointing up)
3. Scroll down and tap "Add to Home Screen"
4. Tap "Add" — done. VoltQuote icon appears on home screen.

### Android (Chrome):
1. Open voltquote.app in Chrome
2. A banner appears automatically: "Install VoltQuote"
3. Tap Install — done.

## What you need to do:
1. Deploy to Vercel following the README instructions
2. Buy voltquote.app or voltquote.com domain (~$12/year)
3. Share the URL with electricians — they install it themselves

## How to promote the PWA:
- Text the link to every electrician you know
- Post in r/electricians on Reddit
- Post in Facebook Groups (search "electrical contractors")
- Share in your union local WhatsApp or group chat
- Post on LinkedIn

---

# ═══════════════════════════════════════
# PHASE 2 — GOOGLE PLAY STORE
# Cost: $25 one-time · Time: 1-2 weeks
# ═══════════════════════════════════════

Google Play is easier and faster than Apple. Start here.

## What you need:
- Android phone or access to one for testing
- Google account (Gmail)
- $25 one-time developer registration fee
- A credit/debit card
- Computer with internet

## Step-by-Step:

### STEP 1 — Create Your Google Play Developer Account
1. Go to play.google.com/console
2. Sign in with your Google account
3. Click "Get started"
4. Pay the $25 registration fee
5. Fill in your developer name — use "VoltQuote" or your business name
6. Accept the developer agreement

### STEP 2 — Convert VoltQuote to an Android App (TWA Method)
The easiest way to publish a PWA on Google Play is using a
Trusted Web Activity (TWA). This wraps your web app in an Android shell.
No complex coding needed.

Option A — Use Bubblewrap (free, command line):
1. Install Node.js (already done from README)
2. Install Bubblewrap:
   npm install -g @bubblewrap/cli
3. Run:
   bubblewrap init --manifest https://voltquote.app/manifest.json
4. Follow the prompts — enter your app name, package name, etc.
   Package name example: app.voltquote.android
5. Build the APK:
   bubblewrap build
6. This creates a .aab file (Android App Bundle) ready to upload

Option B — Use PWABuilder (easiest, free, no coding):
1. Go to pwabuilder.com
2. Enter: https://voltquote.app
3. Click "Build My PWA"
4. Select "Google Play"
5. Fill in: App name, Package ID (app.voltquote.android), Version
6. Click Generate — downloads a .zip with your Android package
7. Inside the zip is your .aab file

### STEP 3 — Upload to Google Play Console
1. Go back to play.google.com/console
2. Click "Create app"
3. Fill in:
   - App name: VoltQuote — Electrical Estimator
   - Default language: English (US)
   - App or Game: App
   - Free or Paid: Free (or Paid if you add subscriptions)
4. Click "Create app"

### STEP 4 — Fill In Your Store Listing
Copy and paste this exactly:

APP TITLE:
VoltQuote — Electrical Estimator

SHORT DESCRIPTION (80 chars max):
Professional electrical estimates in minutes. Built for the trades.

FULL DESCRIPTION:
VoltQuote is the fastest residential electrical estimating tool built
for working electricians and contractors across the United States.

FEATURES:
⚡ Location-based pricing for 55+ US cities — rates auto-adjust by market
📋 60+ line items covering every residential job type
📷 Photo analysis — upload a room photo, AI identifies what work is needed
📖 NEC 2023 code reference built in — 22 key residential articles
💬 AI electrician chat — ask code questions and get real answers
👤 Customer-facing quote view — professional estimate your client sees
📄 Invoice generator — one tap from estimate to invoice
💰 Overhead calculator — know your real break-even hourly rate
🌐 Full English and Spanish support
📱 Works offline — use it in crawl spaces, basements, anywhere

Whether you're quoting a simple outlet installation or a full panel
upgrade, VoltQuote gives you accurate, professional estimates in under
5 minutes. No subscription required to start.

Built for residential electricians, apprentices, and contractors who
want to look professional and price their work accurately.

CATEGORY: Business > Productivity

TAGS: electrician, electrical estimator, contractor, NEC code, electrical bid

### STEP 5 — Screenshots (Required)
You need at least 2 screenshots (phone size: 1080x1920px or similar).
How to get them:
1. Open VoltQuote on an Android phone
2. Take screenshots of: the estimator, the NEC reference, the customer view
3. Upload them to the Play Console

### STEP 6 — Set Up Content Rating
1. In the Play Console left menu, click "Content rating"
2. Click "Start questionnaire"
3. Select category: Utility
4. Answer all questions (VoltQuote has no violence, mature content, etc.)
5. All answers will be "No" — VoltQuote is a business tool
6. Click "Save and next" then "Submit"

### STEP 7 — Set Up Pricing
1. Click "Monetization" in the left menu
2. Select "Free" for now
3. You can add in-app subscriptions later via Google Play Billing

### STEP 8 — Submit for Review
1. Go to "Production" track
2. Upload your .aab file
3. Click "Review release" then "Start rollout to Production"
4. Google reviews within a few hours to 3 days
5. You'll get an email when it's live

---

# ═══════════════════════════════════════
# PHASE 3 — APPLE APP STORE
# Cost: $99/year · Time: 2-4 weeks
# ═══════════════════════════════════════

Apple is stricter and requires more steps. Do Google Play first to
practice the process.

## What you need:
- iPhone for testing
- Mac computer (or access to one — even a friend's Mac works)
  OR use a cloud Mac service like MacStadium ($30/month, cancel after)
- Apple ID
- $99/year Apple Developer Program membership
- Credit/debit card

## Step-by-Step:

### STEP 1 — Enroll in Apple Developer Program
1. Go to developer.apple.com/programs
2. Sign in with your Apple ID
3. Click "Enroll"
4. Choose: Individual (not Organization, unless you have an LLC)
5. Fill in your info — Apple will verify your identity
6. Pay $99 — this renews every year
7. Approval takes 24-48 hours

### STEP 2 — Use PWABuilder for iOS (Easiest Method)
Just like Google Play, PWABuilder can create an iOS app from your PWA.

1. Go to pwabuilder.com
2. Enter: https://voltquote.app
3. Click "Build My PWA"
4. Select "iOS"
5. Fill in:
   - Bundle ID: app.voltquote.ios
   - App name: VoltQuote
   - Version: 1.0.0
6. Click Generate — downloads an Xcode project .zip

### STEP 3 — Open in Xcode (Mac Required)
1. Install Xcode free from the Mac App Store
2. Unzip the PWABuilder download
3. Open the .xcodeproj file in Xcode
4. In Xcode, set your Team to your Apple Developer account
   (Xcode → Project Settings → Signing & Capabilities → Team)
5. Change Bundle Identifier to: app.voltquote.ios
6. Connect your iPhone via USB
7. Select your iPhone as the build target
8. Click Play button to build and test on your phone
9. Make sure everything looks right on the actual device

### STEP 4 — Archive and Upload to App Store Connect
1. In Xcode menu: Product → Archive
2. Wait for build to complete (5-15 minutes)
3. The Organizer window opens automatically
4. Click "Distribute App"
5. Select "App Store Connect"
6. Click "Next" through all prompts
7. Click "Upload" — Xcode uploads to Apple

### STEP 5 — Set Up Your App in App Store Connect
1. Go to appstoreconnect.apple.com
2. Sign in with your Apple ID
3. Click "My Apps" → "+" → "New App"
4. Fill in:
   - Platform: iOS
   - Name: VoltQuote — Electrical Estimator
   - Primary Language: English (U.S.)
   - Bundle ID: app.voltquote.ios
   - SKU: voltquote-ios-001
5. Click "Create"

### STEP 6 — Fill In App Store Listing

APP NAME:
VoltQuote — Electrical Estimator

SUBTITLE (30 chars):
Built for the trades

KEYWORDS (100 chars max):
electrician,electrical,estimator,contractor,NEC,bid,quote,residential,wiring,panel

DESCRIPTION:
VoltQuote is the professional electrical estimating tool built for
working electricians and contractors across the United States.

Get accurate, professional estimates in under 5 minutes — from the
truck, the job site, or anywhere you have your phone.

FEATURES:
⚡ Location-based pricing for 55+ US cities
📋 60+ line items — outlets, panels, EV chargers, generators and more
📷 Photo analysis — AI identifies what electrical work is needed
📖 NEC 2023 code reference — 22 residential articles at your fingertips
💬 AI electrician assistant — ask code questions, get real answers
👤 Professional customer quote view
📄 Invoice generator — estimate to invoice in one tap
💰 Overhead calculator — know your real hourly break-even rate
🌐 English and Spanish / Inglés y Español
📱 Works offline

Built for residential electricians, apprentices, and small contractors
who want professional estimates without paying $100+/month for software.

PROMOTIONAL TEXT (170 chars, can change without resubmission):
The fastest electrical estimating tool for contractors. Location-based
pricing, NEC 2023 reference, AI quotes. Free to start.

SUPPORT URL: https://voltquote.app
MARKETING URL: https://voltquote.app
PRIVACY POLICY URL: https://voltquote.app/privacy

PRICE: Free

PRIMARY CATEGORY: Business
SECONDARY CATEGORY: Utilities

### STEP 7 — Screenshots (Required — Apple is strict)
You need screenshots for specific iPhone sizes. Required sizes:
- 6.9" display (iPhone 16 Pro Max): 1320x2868px
- 6.5" display (iPhone 14 Plus): 1242x2688px
- 5.5" display (older iPhones): 1242x2208px

Easiest way: Use the iPhone Simulator in Xcode
1. In Xcode: Xcode → Open Developer Tool → Simulator
2. Select iPhone 16 Pro Max
3. Open VoltQuote in the simulator
4. Take screenshots: Device → Screenshot (or Cmd+S)
5. Repeat for each required size
6. Upload to App Store Connect

### STEP 8 — Privacy Policy (Required by Apple)
Apple requires a privacy policy URL. Create a simple one:

Go to app-privacy-policy-generator.firebaseapp.com
Fill in: App name = VoltQuote, no personal data collected
Copy the generated text
Post it to voltquote.app/privacy (a simple HTML page)

Or use this simple text and host it:
"VoltQuote does not collect, store, or share any personal data.
All estimates are stored locally on your device. The app uses
the Anthropic API for AI features — no personal data is sent,
only your estimate data for generating summaries."

### STEP 9 — Submit for Review
1. In App Store Connect, click "Add for Review"
2. Answer the export compliance questions (all No for VoltQuote)
3. Click "Submit to App Review"
4. Apple reviews in 1-3 business days
5. Common rejection reasons and fixes:
   - Missing privacy policy → add the URL
   - Crashes on launch → test more in simulator
   - UI doesn't follow guidelines → Apple will tell you specifically what to fix
6. If rejected, fix the issue and resubmit — no extra cost

### STEP 10 — After Approval
1. Set your release to "Manually release" so you control the launch date
2. When ready: App Store Connect → Pricing and Availability → Release this version
3. App goes live within a few hours of manual release

---

# ═══════════════════════════════════════
# MONETIZATION — ADDING PAID SUBSCRIPTIONS
# ═══════════════════════════════════════

## Google Play Subscriptions:
1. Play Console → Monetize → Products → Subscriptions
2. Create subscription:
   - Product ID: voltquote_pro_monthly
   - Name: VoltQuote Pro
   - Price: $9.99/month
3. Add a free trial: 7 days (increases conversions)
4. Google handles all billing and pays you monthly

## Apple In-App Subscriptions:
1. App Store Connect → My Apps → VoltQuote → Subscriptions
2. Create subscription group: "VoltQuote Pro"
3. Add subscription:
   - Reference name: Pro Monthly
   - Product ID: app.voltquote.ios.pro.monthly
   - Price: $9.99/month
   - Free trial: 7 days
4. Apple handles billing and pays you monthly (Apple takes 30%, or 15% for small developers)

## Revenue split on $9.99/month:
- Google Play: you keep ~$8.60 per subscriber
- Apple App Store: you keep ~$8.49 per subscriber (15% small dev rate)
- 100 subscribers = ~$860/month
- 500 subscribers = ~$4,300/month

---

# ═══════════════════════════════════════
# TIMELINE SUMMARY
# ═══════════════════════════════════════

Week 1:  Deploy PWA to Vercel. Buy domain. Share with electricians.
Week 2:  Use PWABuilder to create Android package. Submit to Google Play.
Week 3:  Google Play approved and live. Start collecting users.
Week 4:  Set up Apple Developer account. Build iOS version.
Week 5:  Submit to Apple App Store.
Week 6:  Apple approved. VoltQuote live on all 3 platforms.
Month 2: Add paid subscriptions. Start marketing seriously.
Month 3: First paying users. Passive income begins.

---

# ═══════════════════════════════════════
# RESOURCES
# ═══════════════════════════════════════

PWA Testing:    web.dev/measure
PWA Builder:    pwabuilder.com
Google Play:    play.google.com/console
Apple Dev:      developer.apple.com
App Store:      appstoreconnect.apple.com
Xcode:          Free on Mac App Store
Cloud Mac:      macstadium.com (~$30/mo, cancel after)
Privacy Policy: app-privacy-policy-generator.firebaseapp.com
Icon Generator: realfavicongenerator.net

---

Built with ⚡ — VoltQuote
