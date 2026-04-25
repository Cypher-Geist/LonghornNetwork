import { useState, useEffect } from 'react';
import GraphView from './components/GraphView.jsx';
import RoommatesView from './components/RoommatesView.jsx';
import PodsView from './components/PodsView.jsx';
import ReferralView from './components/ReferralView.jsx';
import SocialView from './components/SocialView.jsx';

const SECTIONS = [
  { id: 'graph',     label: 'Student Graph',    icon: '⬡' },
  { id: 'roommates', label: 'Roommates',         icon: '♥' },
  { id: 'pods',      label: 'Pods',              icon: '◈' },
  { id: 'referral',  label: 'Referral Finder',   icon: '⇢' },
  { id: 'social',    label: 'Social',            icon: '◉' },
];

const LOADING_STEPS = [
  'Parsing student data from Main.java…',
  'Building StudentGraph (adjacency list)…',
  'Running Gale-Shapley roommate matching…',
  'Forming pods with Prim algorithm…',
  'Computing referral paths (Dijkstra)…',
  'Launching FriendRequest & Chat threads…',
];

export default function App() {
  const [data, setData]         = useState(null);
  const [loading, setLoading]   = useState(true);
  const [loadStep, setLoadStep] = useState(0);
  const [error, setError]       = useState(null);
  const [selectedTC, setSelectedTC]     = useState(0);
  const [activeSection, setActiveSection] = useState('graph');

  // Animate loading steps, then fetch
  useEffect(() => {
    let step = 0;
    const timer = setInterval(() => {
      step++;
      setLoadStep(step);
      if (step >= LOADING_STEPS.length) {
        clearInterval(timer);
        fetch('/api/data')
          .then(r => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.json(); })
          .then(d => { setData(d); setLoading(false); })
          .catch(e => { setError(e.message); setLoading(false); });
      }
    }, 300);
    return () => clearInterval(timer);
  }, []);

  if (loading || error) {
    return (
      <div className="loading-screen">
        <div className="loading-title">LONGHORN NETWORK</div>
        {error ? (
          <div style={{ color: 'var(--red)', fontFamily: 'JetBrains Mono', fontSize: 13 }}>
            ✕ Could not reach the Java server.<br />
            <span style={{ color: 'var(--text-muted)', fontSize: 11, marginTop: 6, display: 'block' }}>
              Make sure Server.java is running:&nbsp;
              <code style={{ color: 'var(--orange-light)' }}>java Server</code>
            </span>
            <span style={{ color: 'var(--text-dim)', fontSize: 11 }}>{error}</span>
          </div>
        ) : (
          <>
            <div className="loading-bar-track"><div className="loading-bar-fill" /></div>
            <div className="loading-steps">
              {LOADING_STEPS.slice(0, loadStep).map((s, i) => (
                <div key={i} className="loading-step" style={{ animationDelay: `${i * 0.05}s` }}>
                  <span className="loading-step-icon">✓</span>{s}
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    );
  }

  const tc = data.testCases[selectedTC];

  return (
    <div className="app">

      {/* ── Header ── */}
      <header className="header">
        <span className="header-logo">🤘 LONGHORN NETWORK</span>
        <span className="header-sub">ECE 422C · Lab 6</span>
        <div className="header-spacer" />
        <div className="header-status">
          <span className="status-dot" />
          Java server connected
        </div>
      </header>

      {/* ── Test Case Selector ── */}
      <div className="tc-selector">
        <span className="tc-label">Test Case</span>
        {data.testCases.map((tc, i) => (
          <button
            key={i}
            className={`tc-btn${selectedTC === i ? ' active' : ''}`}
            onClick={() => setSelectedTC(i)}
          >
            Case {i + 1} — {tc.students.length} students
          </button>
        ))}
      </div>

      {/* ── Section Tabs ── */}
      <div className="section-tabs">
        {SECTIONS.map(s => (
          <button
            key={s.id}
            className={`sec-tab${activeSection === s.id ? ' active' : ''}`}
            onClick={() => setActiveSection(s.id)}
          >
            <span className="sec-tab-icon">{s.icon}</span>
            {s.label}
            {s.id === 'graph' && (
              <span className="sec-tab-badge">{tc.edges.length} edges</span>
            )}
            {s.id === 'pods' && (
              <span className="sec-tab-badge">{tc.pods.length} pods</span>
            )}
          </button>
        ))}
      </div>

      {/* ── Content ── */}
      <main className="content">
        {activeSection === 'graph'     && <GraphView     students={tc.students} edges={tc.edges} />}
        {activeSection === 'roommates' && <RoommatesView students={tc.students} />}
        {activeSection === 'pods'      && <PodsView      pods={tc.pods} students={tc.students} />}
        {activeSection === 'referral'  && <ReferralView  students={tc.students} tcId={selectedTC + 1} />}
        {activeSection === 'social'    && <SocialView    students={tc.students} />}
      </main>

    </div>
  );
}
