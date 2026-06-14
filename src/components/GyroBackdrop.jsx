// src/components/GyroBackdrop.jsx
// Mounts the living gyro field behind a screen.
//   reskin = true  (default): also re-skins the screen to copper/glass by
//            overriding the theme vars it already uses (--card, --accent, …).
//   reskin = false: ONLY reveals the field (makes --bg-scene transparent) and
//            leaves the screen's own colors exactly as they were.
// Either way, the wrapped screen's code is untouched.
import GyroField from "./GyroField/GyroField";

const FULL_SKIN = {
  position: "relative",
  zIndex: 1,
  minHeight: "100vh",
  "--bg-scene": "transparent",
  "--bg0": "transparent",
  "--card": "rgba(17,22,31,0.62)",
  "--surface": "rgba(16,21,29,0.97)",
  "--line": "rgba(123,134,150,0.16)",
  "--line-strong": "rgba(123,134,150,0.30)",
  "--accent": "#E0904A",
  "--accent-rgb": "224,144,74",
};

const REVEAL_ONLY = {
  position: "relative",
  zIndex: 1,
  minHeight: "100vh",
  "--bg-scene": "transparent",
  "--bg0": "transparent",
};

export default function GyroBackdrop({ variant = "steel", reskin = true, children }) {
  return (
    <>
      <GyroField variant={variant} />
      <div style={reskin ? FULL_SKIN : REVEAL_ONLY}>{children}</div>
    </>
  );
}
