// src/components/GyroBackdrop.jsx
// Mounts the living gyro field behind any screen and makes that screen's
// themed page background (--bg-scene) transparent, so the field shows
// through — WITHOUT editing the screen's own code. Cards, headers, modals
// and all logic stay exactly as they are; they just float on the field.
import GyroField from "./GyroField/GyroField";

export default function GyroBackdrop({ variant = "steel", children }) {
  return (
    <>
      <GyroField variant={variant} />
      <div style={{ position: "relative", zIndex: 1, minHeight: "100vh", "--bg-scene": "transparent" }}>
        {children}
      </div>
    </>
  );
}
