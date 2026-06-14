'use client';
import GyroField from '../components/GyroField/GyroField';
import Brand from '../components/Brand';
import './Estimator.css';

const ITEMS = [
  ['200A panel, 40-space', 'NEC 408.36 · main breaker', '1', '$412.00', '$340.00', '$752.00'],
  ['Service entrance, 2/0 AWG Cu', 'NEC 310.16 · 75°C', '60 ft', '$318.00', '$210.00', '$528.00'],
  ['Branch circuit, 12 AWG', 'NEC 210.19 · 20A', '14', '$196.00', '$420.00', '$616.00'],
  ['AFCI breaker, 20A', 'NEC 210.12 · bedroom', '8', '$344.00', '$160.00', '$504.00'],
  ['GFCI receptacle', 'NEC 210.8 · kitchen/bath', '6', '$108.00', '$150.00', '$258.00'],
  ['Grounding electrode, 8 ft rod', 'NEC 250.52', '2', '$46.00', '$120.00', '$166.00'],
  ['Permit + inspection', 'local · residential', '1', '$185.00', '—', '$185.00'],
];

export default function Estimator() {
  return (
    <>
      <GyroField variant="steel" />
      <div className="ww-page ww-estimator">
        <div className="bar">
          <Brand size={16} />
          <div className="proj">
            <span className="name">Maple St. — service upgrade</span>
            <span className="badge">NEC 2023</span>
          </div>
        </div>

        <div className="work">
          <div className="card">
            <h2>Line items <span className="muted">{ITEMS.length} ROWS</span></h2>
            <table>
              <thead>
                <tr>
                  <th className="desc">Item</th>
                  <th className="num">Qty</th>
                  <th className="num">Material</th>
                  <th className="num">Labor</th>
                  <th className="num">Total</th>
                </tr>
              </thead>
              <tbody>
                {ITEMS.map(([name, code, qty, mat, lab, tot]) => (
                  <tr key={name}>
                    <td className="desc">{name}<span className="code">{code}</span></td>
                    <td className="num">{qty}</td>
                    <td className="num">{mat}</td>
                    <td className="num">{lab}</td>
                    <td className="num total">{tot}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="addrow"><span className="plus">+</span> Add line item</div>
          </div>

          <div className="card totals">
            <h2>Estimate</h2>
            <div className="trow"><span className="l">Material</span><span className="v">$1,609.00</span></div>
            <div className="trow"><span className="l">Labor</span><span className="v">$1,400.00</span></div>
            <div className="trow"><span className="l">Markup · 18%</span><span className="v">$541.62</span></div>
            <div className="trow"><span className="l">Tax · 8%</span><span className="v">$284.21</span></div>
            <div className="trow grand"><span className="l">Total</span><span className="v">$3,834.83</span></div>
            <div className="actions">
              <button className="btn btn-primary">Send quote</button>
              <button className="btn btn-ghost">Save draft</button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
