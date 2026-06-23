'use client';
import GyroField from '../components/GyroField/GyroField';
import Brand from '../components/Brand';
import './Dashboard.css';

const BARS = [
  ['W1', 42, false], ['W2', 58, false], ['W3', 36, false],
  ['W4', 70, false], ['W5', 88, true], ['W6', 64, false],
];

export default function Dashboard() {
  return (
    <>
      <GyroField variant="horizon" />
      <div className="ww-veil-dash" />
      <div className="ww-page ww-dashboard">
        <div className="bar">
          <Brand size={16} />
          <div className="range">LAST 30 DAYS</div>
        </div>

        <h1>Business<span className="sub">Where the work — and the money — stands.</span></h1>

        <div className="kpis">
          <div className="kpi money"><div className="lab">Revenue</div><div className="val">$48.2k</div><div className="delta">▲ 12% vs prior</div></div>
          <div className="kpi"><div className="lab">Quotes sent</div><div className="val">37</div><div className="delta">▲ 6 this week</div></div>
          <div className="kpi"><div className="lab">Win rate</div><div className="val">61%</div><div className="delta">▲ 4 pts</div></div>
          <div className="kpi money"><div className="lab">Avg quote</div><div className="val">$3.4k</div><div className="delta down">▼ 2% vs prior</div></div>
        </div>

        <div className="lower">
          <div className="panel">
            <h2>Quotes by week <span className="m">USD</span></h2>
            <div className="bars">
              {BARS.map(([label, h, peak]) => (
                <div key={label} className={'b' + (peak ? ' peak' : '')} style={{ height: h + '%' }}>
                  <span>{label}</span>
                </div>
              ))}
            </div>
          </div>
          <div className="panel">
            <h2>Recent quotes</h2>
            <div className="list">
              <div className="li"><span className="who">Maple St.<small>Service upgrade</small></span><span className="amt">$3,834</span></div>
              <div className="li"><span className="who">Linwood Ave.<small>EV charger</small></span><span className="st win">WON</span></div>
              <div className="li"><span className="who">Oak Ridge<small>Rewire</small></span><span className="amt">$11,200</span></div>
              <div className="li"><span className="who">Cedar Ct.<small>Panel swap</small></span><span className="st open">OPEN</span></div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
