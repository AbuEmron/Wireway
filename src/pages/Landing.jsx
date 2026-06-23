'use client';
import GyroField from '../components/GyroField/GyroField';
import Brand from '../components/Brand';
import './Landing.css';

// onSignIn / onSignUp are passed down from App.jsx — they trigger your
// existing Supabase auth flow (setAuthMode), so login is unchanged.
export default function Landing({ onSignIn, onSignUp }) {
  return (
    <>
      <GyroField variant="hero" />
      <div className="ww-veil-landing" />
      <div className="ww-page ww-landing">
        <header className="top">
          <Brand src="/logo192.png" />
          <nav className="nav">
            <button type="button" className="nav-link" onClick={onSignIn}>Sign in</button>
          </nav>
        </header>

        <section className="hero">
          <div className="eyebrow">NEC 2023 · Residential</div>
          <h1>Walk the job.<br />Quote it <span className="wired">wired right.</span></h1>
          <p className="sub">
            Build residential estimates on site — <b>line items, labor, and code references in one pass.</b>{' '}
            Price the panel before you leave the truck.
          </p>
          <div className="cta-row">
            <button type="button" className="btn btn-primary" onClick={onSignUp}>Start an estimate</button>
            <button type="button" className="btn btn-ghost" onClick={onSignIn}>Sign in</button>
          </div>
        </section>

        <footer className="strip">
          <span className="legend"><span className="dot" style={{ background: '#1a1a1a', border: '1px solid #333' }} />HOT</span>
          <span className="legend"><span className="dot" style={{ background: '#e9e6dd' }} />NEUTRAL</span>
          <span className="legend"><span className="dot" style={{ background: '#3f8a4a' }} />GROUND</span>
          <span>·</span>
          <span>NEC&nbsp;<span className="val">2023</span></span>
          <span>LINE&nbsp;LIBRARY&nbsp;<span className="val">120+</span></span>
          <span>LABOR&nbsp;+&nbsp;MATERIAL&nbsp;<span className="val">ONE&nbsp;PASS</span></span>
        </footer>
      </div>
    </>
  );
}
