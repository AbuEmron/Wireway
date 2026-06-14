// src/components/GyroBackdrop.jsx
// Mounts the living gyro field behind a screen AND re-skins that screen to
// the new copper/glass design — purely by overriding the theme variables it
// already uses (--card, --surface, --accent, --bg-scene). No edits to the
// wrapped screen's own code; its logic and structure are untouched.
import GyroField from "./GyroField/GyroField";

// New design palette, expressed as the theme vars the app already reads.
const SKIN = {
  position: "relative",
  zIndex: 1,
  minHeight: "100vh",

  // reveal the field: page background goes transparent
  "--bg-scene": "transparent",
  "--bg0": "transparent",

  // glass panels — the field shows through them
  "--card": "rgba(17,22,31,0.62)",
  "--surface": "rgba(16,21,29,0.97)",   // modals: near-solid so they stay readable
  "--line": "rgba(123,134,150,0.16)",
  "--line-strong": "rgba(123,134,150,0.30)",

  // unify the accent to copper
  "--accent": "#E0904A",
  "--accent-rgb": "224,144,74",
};

export default function GyroBackdrop({ variant = "steel", children }) {
  return (
    <>
      <GyroField variant={variant} />
      <div style={SKIN}>{children}</div>
    </>
  );
}
