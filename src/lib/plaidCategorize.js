// src/lib/plaidCategorize.js
// Maps Plaid personal_finance_category (primary + detailed) and merchant name
// to the app's 12 Schedule C tax categories.
//
// Plaid PFC reference: https://plaid.com/docs/transactions/pfc/
// App categories: materials | fuel | tools | vehicle | insurance |
//                 advertising | meals | phone | permits | subcontractors | office | other

// Keyword → category lookup for merchant names (case-insensitive substring match)
const MERCHANT_RULES = [
  // Fuel / gas
  { keywords: ["shell", "exxon", "mobil", "chevron", "bp", "sunoco", "speedway", "marathon", "circle k", "wawa", "pilot", "flying j", "loves travel", "kwik trip", "casey", "valero", "arco"], cat: "fuel" },
  // Materials & Parts (electrical supply)
  { keywords: ["home depot", "lowes", "lowe's", "menards", "fastenal", "grainger", "platt electric", "rexel", "anixter", "graybar", "wesco", "city electric", "independent electric", "border states", "crescent electric", "watt electric", "elliot electric", "cec industries", "sonepar", "veritiv", "summit electric"], cat: "materials" },
  // Tools & Equipment
  { keywords: ["harbor freight", "northern tool", "snap-on", "snapon", "klein tools", "dewalt", "milwaukee tool", "ridgid", "fluke", "ideal industries", "tool depot", "mac tools", "matco"], cat: "tools" },
  // Vehicle
  { keywords: ["autozone", "advance auto", "o'reilly", "oreilly", "napa auto", "car quest", "pep boys", "jiffy lube", "firestone", "goodyear", "mavis", "valvoline", "midas", "monro", "sears auto", "les schwab", "discount tire", "americas tire", "ntb", "mr. tire", "take 5 oil"], cat: "vehicle" },
  // Insurance
  { keywords: ["progressive", "geico", "state farm", "allstate", "nationwide", "liberty mutual", "travelers", "farmers insurance", "zurich", "hartford", "markel", "guard insurance", "usaa", "insurance"], cat: "insurance" },
  // Advertising / marketing
  { keywords: ["google ads", "facebook ads", "meta ads", "yelp for business", "angi", "angies list", "thumbtack", "homeadvisor", "houzz", "nextdoor ads", "mailchimp", "constant contact", "vistaprint", "4imprint"], cat: "advertising" },
  // Meals
  { keywords: ["mcdonald", "subway", "chick-fil-a", "taco bell", "burger king", "wendy's", "dominos", "pizza hut", "starbucks", "dunkin", "panera", "chipotle", "olive garden", "applebee", "denny", "ihop", "cracker barrel", "perkins", "waffle house", "sonic drive", "dairy queen", "popeyes", "kfc", "little caesars"], cat: "meals" },
  // Phone / internet
  { keywords: ["verizon", "at&t", "t-mobile", "sprint", "us cellular", "comcast", "xfinity", "spectrum", "cox communications", "charter communications", "google fi", "visible wireless", "cricket wireless", "metro pcs", "boost mobile"], cat: "phone" },
  // Office / software
  { keywords: ["staples", "office depot", "amazon business", "costco business", "sam's club", "uline", "quill"], cat: "office" },
  // Subcontractors
  { keywords: ["subcontract", "labor only", "1099"], cat: "subcontractors" },
];

// PFC primary → category (fallback when detailed doesn't match)
const PRIMARY_MAP = {
  "TRANSPORTATION":           "vehicle",
  "GAS_AND_CONVENIENCE":      "fuel",
  "HOME_IMPROVEMENT":         "materials",
  "GENERAL_MERCHANDISE":      "materials",
  "FOOD_AND_DRINK":           "meals",
  "RESTAURANTS":              "meals",
  "TRAVEL":                   "vehicle",
  "UTILITIES":                "phone",
  "TELECOMMUNICATION_SERVICES": "phone",
  "INSURANCE":                "insurance",
  "PROFESSIONAL_SERVICES":    "subcontractors",
  "ADVERTISING_AND_MARKETING": "advertising",
};

// PFC detailed → category (higher priority than primary)
const DETAILED_MAP = {
  // Fuel
  "TRANSPORTATION_GAS_AND_CONVENIENCE":  "fuel",
  "TRANSPORTATION_FUEL":                 "fuel",
  "GAS_AND_CONVENIENCE_GAS":             "fuel",
  // Vehicle
  "TRANSPORTATION_PARKING":              "vehicle",
  "TRANSPORTATION_AUTOMOTIVE":           "vehicle",
  "TRANSPORTATION_AUTO_PARTS":           "vehicle",
  "TRANSPORTATION_CAR_RENTAL":           "vehicle",
  "TRANSPORTATION_TAXIS_AND_RIDE_SHARES": "vehicle",
  // Food
  "FOOD_AND_DRINK_FAST_FOOD":           "meals",
  "FOOD_AND_DRINK_COFFEE":              "meals",
  "FOOD_AND_DRINK_RESTAURANTS":         "meals",
  "FOOD_AND_DRINK_BAR":                 "meals",
  // Materials
  "HOME_IMPROVEMENT_HARDWARE_STORES":   "materials",
  "HOME_IMPROVEMENT_PLUMBING":          "materials",
  "HOME_IMPROVEMENT_SECURITY":          "materials",
  "HOME_IMPROVEMENT_FURNITURE":         "materials",
  "HOME_IMPROVEMENT_CONTRACTORS":       "subcontractors",
  // Tools / equipment
  "GENERAL_MERCHANDISE_WHOLESALE_CLUBS": "materials",
  "GENERAL_MERCHANDISE_ONLINE_MARKETPLACES": "materials",
  // Insurance
  "INSURANCE_AUTO_INSURANCE":           "insurance",
  "INSURANCE_BUSINESS_INSURANCE":       "insurance",
  "INSURANCE_HEALTH_INSURANCE":         "insurance",
  "INSURANCE_LIFE_INSURANCE":           "insurance",
  "INSURANCE_PROPERTY_AND_CASUALTY":    "insurance",
  // Phone / internet
  "TELECOMMUNICATION_SERVICES_WIRELESS": "phone",
  "TELECOMMUNICATION_SERVICES_CABLE":   "phone",
  "TELECOMMUNICATION_SERVICES_INTERNET": "phone",
  // Advertising
  "ADVERTISING_AND_MARKETING_MEDIA":    "advertising",
  "ADVERTISING_AND_MARKETING_PRINT":    "advertising",
  // Professional services / subs
  "PROFESSIONAL_SERVICES_CONSULTING":   "subcontractors",
  "PROFESSIONAL_SERVICES_STAFFING":     "subcontractors",
  "PROFESSIONAL_SERVICES_LEGAL":        "office",
  "PROFESSIONAL_SERVICES_ACCOUNTING":   "office",
  // Utilities (map to phone as catch-all for operational costs)
  "UTILITIES_ELECTRIC":                 "other",
  "UTILITIES_WATER":                    "other",
};

export function mapPlaidCategory({ primary = "", detailed = "", merchant = "" }) {
  const merchantLower = merchant.toLowerCase();

  // 1. Merchant name rules — most specific
  for (const rule of MERCHANT_RULES) {
    if (rule.keywords.some((kw) => merchantLower.includes(kw))) {
      return rule.cat;
    }
  }

  // 2. Detailed PFC code
  const detailedKey = detailed.toUpperCase().replace(/[^A-Z_]/g, "_");
  if (DETAILED_MAP[detailedKey]) return DETAILED_MAP[detailedKey];

  // 3. Primary PFC code
  const primaryKey = primary.toUpperCase().replace(/[^A-Z_]/g, "_");
  if (PRIMARY_MAP[primaryKey]) return PRIMARY_MAP[primaryKey];

  return "other";
}
