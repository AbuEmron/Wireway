'use client';
import GyroField from '../components/GyroField/GyroField';
import Brand from '../components/Brand';
import './QuoteBuilder.css';

export default function QuoteBuilder() {
  return (
    <>
      <GyroField variant="synapse" />
      <div className="ww-veil-quote" />
      <div className="ww-page ww-quote">
        <div className="bar">
          <Brand size={16} />
          <div className="mode"><span className="d" />QUOTE BUILDER</div>
        </div>

        <div className="core">
          <div>
            <div className="lead">Describe the job.<br /><span className="em">Get a priced quote back.</span></div>
            <p className="sub">Plain words in, NEC-checked line items out — with labor, material, and code references already attached.</p>
          </div>

          <div className="composer">
            <div className="row">
              <textarea placeholder="200A service upgrade, 1960s split-level. Replace the main panel, add 6 AFCI circuits and kitchen GFCIs…" />
              <button className="send" aria-label="Build quote">↑</button>
            </div>
            <div className="chips">
              <span className="chip">Panel swap</span>
              <span className="chip">EV charger circuit</span>
              <span className="chip">Whole-home rewire</span>
              <span className="chip">Hot tub feed</span>
            </div>
          </div>

          <div className="gen">
            <div className="ghead">
              <span className="t">Drafting quote</span>
              <span className="typing">BUILDING<i /><i /><i /></span>
            </div>
            <div className="gline"><span className="l">200A panel, 40-space<span className="c">NEC 408.36</span></span><span className="v">$752.00</span></div>
            <div className="gline"><span className="l">AFCI breakers · 6<span className="c">NEC 210.12</span></span><span className="v">$378.00</span></div>
            <div className="gline"><span className="l">Kitchen GFCI · 4<span className="c">NEC 210.8</span></span><span className="v">$172.00</span></div>
            <div className="gtot"><span className="l">Estimated total</span><span className="v">$1,302.00</span></div>
          </div>
        </div>
      </div>
    </>
  );
}
