// VoltQuote — Secure Anthropic API Proxy
// This Vercel serverless function keeps your API key hidden from the browser.
// Your key lives only in Vercel's environment variables — never in the app code.

export default async function handler(req, res) {
  // Only allow POST requests
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }

  // Your API key lives securely in Vercel environment variables
  const apiKey = process.env.ANTHROPIC_API_KEY;
  if (!apiKey) {
    return res.status(500).json({ error: "API key not configured" });
  }

  try {
    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": apiKey,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify(req.body),
    });

    const data = await response.json();

    // Pass the response back to the browser
    return res.status(response.status).json(data);

  } catch (error) {
    console.error("VoltQuote proxy error:", error);
    return res.status(500).json({ error: "Proxy request failed" });
  }
}
