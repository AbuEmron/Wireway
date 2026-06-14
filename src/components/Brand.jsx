/*
  Brand — keeps your EXISTING Wireway logo.

  Default: renders an <img> from /public. Drop your current logo at
  public/wireway-logo.svg (or change `src` below) and it's used everywhere.

  Already have a <Logo /> React component? Swap the <img> for it:
      import Logo from '../wherever/Logo';
      ...
      <Logo className="ww-brand__logo" />

  No-asset fallback: if the image path is wrong it just shows the text
  wordmark, so the build never breaks.
*/

export default function Brand({ pro = true, size = 18, src = '/wireway-logo.svg' }) {
  return (
    <div className="ww-brand" style={{ fontSize: size }}>
      <img
        className="ww-brand__logo"
        src={src}
        alt="Wireway"
        onError={(e) => {
          // graceful fallback to the wordmark if the logo file isn't there yet
          e.currentTarget.style.display = 'none';
          e.currentTarget.nextSibling.style.display = 'inline';
        }}
      />
      <span className="ww-brand__word" style={{ display: 'none' }}>WIREWAY</span>
      {pro && <span className="ww-brand__pro">PRO</span>}
    </div>
  );
}
