'use client';
import { useState, useRef, useCallback } from 'react';
import GyroField from '../components/GyroField/GyroField';
import Brand from '../components/Brand';
import './SignIn.css';

export default function SignIn() {
  const [energized, setEnergized] = useState(false);
  const timer = useRef(null);

  // fired each time a current pulse reaches the card
  const onEnergize = useCallback(() => {
    setEnergized(true);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => setEnergized(false), 520);
  }, []);

  return (
    <>
      <GyroField variant="circuit" onEnergize={onEnergize} />
      <div className="ww-veil-auth" />
      <div className="ww-page ww-auth">
        <div className={'card' + (energized ? ' energize' : '')}>
          <Brand size={18} />
          <div className="tag">CLOSE THE CIRCUIT</div>

          <div className="fld">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" placeholder="you@shop.com" autoComplete="email" />
          </div>
          <div className="fld">
            <label htmlFor="pw">Password</label>
            <input id="pw" type="password" placeholder="••••••••" autoComplete="current-password" />
          </div>

          <button className="btn">Sign in</button>

          <div className="meta">
            <a href="#">Forgot password</a>
            <a href="#" className="alt">Create account</a>
          </div>
        </div>
      </div>
    </>
  );
}
