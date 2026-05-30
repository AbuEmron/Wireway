# ⚡ VoltQuote — Residential Electrical Estimator

Professional electrical estimating tool for contractors across the US. Location-based pricing, NEC 2023 reference, AI-powered quote summaries, photo analysis, and customer-facing quote views.

---

## 🚀 HOW TO GO LIVE — STEP BY STEP

### WHAT YOU NEED (ALL FREE TO START)
- A computer (Windows or Mac)
- A free GitHub account → github.com
- A free Vercel account → vercel.com
- An Anthropic API key → console.anthropic.com (for AI features)

---

### STEP 1 — GET YOUR ANTHROPIC API KEY
The AI features (summaries, photo analysis, chat) need an API key.

1. Go to **console.anthropic.com**
2. Sign up for a free account
3. Click **"API Keys"** in the left menu
4. Click **"Create Key"** — name it "VoltQuote"
5. Copy the key — it starts with `sk-ant-...`
6. Keep it safe — you will need it in Step 4

---

### STEP 2 — INSTALL NODE.JS (if you don't have it)
1. Go to **nodejs.org**
2. Download the **LTS version** (the recommended one)
3. Install it — click through all the defaults
4. To verify: open Terminal (Mac) or Command Prompt (Windows) and type:
   ```
   node --version
   ```
   You should see something like `v20.x.x`

---

### STEP 3 — PUT YOUR PROJECT ON GITHUB
1. Go to **github.com** and create a free account
2. Click the **"+"** button → **"New repository"**
3. Name it: `voltquote`
4. Make it **Public**
5. Click **"Create repository"**
6. On your computer, open Terminal / Command Prompt
7. Navigate to this project folder:
   ```
   cd path/to/voltquote
   ```
8. Run these commands one by one:
   ```
   git init
   git add .
   git commit -m "VoltQuote initial launch"
   git branch -M main
   git remote add origin https://github.com/YOURUSERNAME/voltquote.git
   git push -u origin main
   ```
   (Replace YOURUSERNAME with your GitHub username)

---

### STEP 4 — ADD YOUR API KEY SECURELY
1. In your GitHub repo, click **Settings**
2. Click **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Name: `REACT_APP_ANTHROPIC_KEY`
5. Value: paste your `sk-ant-...` key
6. Click **Add secret**

Then open `src/ElectricalEstimator.js` and find every line that says:
```
headers: { "Content-Type": "application/json" },
```
And change it to:
```
headers: {
  "Content-Type": "application/json",
  "x-api-key": process.env.REACT_APP_ANTHROPIC_KEY,
  "anthropic-version": "2023-06-01"
},
```
Save the file, then push again:
```
git add .
git commit -m "Add API key config"
git push
```

---

### STEP 5 — DEPLOY ON VERCEL (FREE)
1. Go to **vercel.com** and sign up with your GitHub account
2. Click **"Add New Project"**
3. Find your `voltquote` repo and click **Import**
4. Under **Environment Variables**, add:
   - Name: `REACT_APP_ANTHROPIC_KEY`
   - Value: your `sk-ant-...` key
5. Click **Deploy**
6. Wait 2-3 minutes — Vercel builds and deploys automatically
7. You get a live URL like: `voltquote.vercel.app`

**That's it. Your app is live.**

---

### STEP 6 — GET A CUSTOM DOMAIN (OPTIONAL, ~$12/year)
1. Go to **namecheap.com**
2. Search for `voltquote.com` or `voltquote.app`
3. Purchase it (~$12-15/year)
4. In Vercel, go to your project → **Settings** → **Domains**
5. Add your domain and follow Vercel's DNS instructions
6. Done — your app runs at voltquote.com

---

## 💰 HOW TO ADD PAID SUBSCRIPTIONS

When you're ready to charge users:

1. Create a free account at **stripe.com**
2. Use a service like **Lemon Squeezy** (lemonsqueezy.com) — easiest for solo developers
3. Create a product: "VoltQuote Pro — $9.99/month"
4. Add a payment link to your app's landing page
5. Use Lemon Squeezy's webhook to unlock features after payment

Or reach out to a developer on **Upwork** to add auth + payments for ~$300-500 one time.

---

## 📱 FEATURES
- ⚡ 60+ residential electrical line items
- 🗺️ Location-based pricing for 55+ US cities
- 📊 Flat rate and Time & Material modes
- 📷 Photo-to-estimate AI analysis
- 💬 AI electrician chat assistant
- 📖 NEC 2023 code reference
- 👤 Contractor profile + customer-facing quote view
- 📋 One-click quote copy

---

## 🛠️ MAKING UPDATES
After going live, to update the app:
1. Edit files on your computer
2. Run:
   ```
   git add .
   git commit -m "describe your change"
   git push
   ```
3. Vercel automatically redeploys in ~2 minutes

---

## 📣 HOW TO GET YOUR FIRST USERS
- Post in **r/electricians** on Reddit
- Share in **Facebook Groups** for electricians (search "electrical contractors")
- Post on **LinkedIn** as a licensed electrician who built a tool
- Share with your union local
- List on **Product Hunt** (free) for national exposure

---

## 📬 QUESTIONS?
Built with ⚡ by a licensed electrician for licensed electricians.

