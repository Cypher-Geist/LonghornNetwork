import { useState } from 'react';

export default function ReferralView({ students, tcId }) {
  const [startName, setStartName]   = useState(students[0]?.name ?? '');
  const [company,   setCompany]     = useState('');
  const [loading,   setLoading]     = useState(false);
  const [result,    setResult]      = useState(null);  // null | { path: string[] }
  const [queried,   setQueried]     = useState(false);

  // Collect all companies across all students in this test case
  const allCompanies = [...new Set(
    students.flatMap(s => s.previousInternships)
  )].sort();

  const handleFind = async () => {
    if (!startName || !company.trim()) return;
    setLoading(true);
    setQueried(false);
    try {
      const url = `/api/referral?tc=${tcId}&start=${encodeURIComponent(startName)}&company=${encodeURIComponent(company.trim())}`;
      const res = await fetch(url);
      const json = await res.json();
      setResult(json);
    } catch (e) {
      setResult({ path: [], error: e.message });
    } finally {
      setLoading(false);
      setQueried(true);
    }
  };

  const hasPath = result && result.path && result.path.length > 0;

  return (
    <div>
      <div className="section-header">
        <div className="section-title">REFERRAL PATH FINDER</div>
        <div className="section-desc">
          Dijkstra's algorithm with inverted weights (10 − strength) ·
          select a starting student and target company to find the strongest connection path
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, alignItems: 'start' }}>

        {/* ── Form ── */}
        <div className="referral-form">
          <div className="referral-form-title">SEARCH PARAMETERS</div>

          <div className="form-row">
            <label className="form-label">Starting Student</label>
            <select className="form-select"
              value={startName}
              onChange={e => setStartName(e.target.value)}>
              {students.map(s => (
                <option key={s.name} value={s.name}>{s.name} — {s.major}</option>
              ))}
            </select>
          </div>

          <div className="form-row">
            <label className="form-label">Target Company</label>
            <input
              className="form-input"
              list="company-suggestions"
              placeholder="e.g. Google, Pfizer, DummyCompany…"
              value={company}
              onChange={e => setCompany(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleFind()}
            />
            <datalist id="company-suggestions">
              {allCompanies.map(c => <option key={c} value={c} />)}
            </datalist>
          </div>

          <button className="find-btn"
            onClick={handleFind}
            disabled={loading || !company.trim()}>
            {loading ? '⏳  Searching…' : '⇢  Find Referral Path'}
          </button>

          {/* Known companies hint */}
          {allCompanies.length > 0 && (
            <div style={{ marginTop: 14 }}>
              <div className="form-label" style={{ marginBottom: 8 }}>
                Companies in this test case
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {allCompanies.map(c => (
                  <button key={c}
                    onClick={() => setCompany(c)}
                    style={{
                      background: company === c ? 'var(--orange-dim)' : 'var(--surface2)',
                      border: `1px solid ${company === c ? 'var(--orange)' : 'var(--border)'}`,
                      borderRadius: 20,
                      color: company === c ? 'var(--orange-light)' : 'var(--text-muted)',
                      fontSize: 11,
                      fontFamily: 'JetBrains Mono',
                      padding: '3px 10px',
                      cursor: 'pointer',
                      transition: 'all 0.15s',
                    }}>
                    {c}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* ── Result ── */}
        <div className="referral-result">
          <div className="referral-result-title">
            {queried ? `RESULT — "${company}"` : 'RESULT'}
          </div>

          {!queried && (
            <div className="no-path">
              <span style={{ fontSize: 20 }}>⇢</span>
              Run a search to see the referral chain here.
            </div>
          )}

          {queried && hasPath && (
            <>
              <div style={{ fontSize: 12, color: 'var(--text-muted)',
                fontFamily: 'JetBrains Mono', marginBottom: 14 }}>
                Path length: {result.path.length} hop{result.path.length !== 1 ? 's' : ''}
              </div>
              <div className="path-display">
                {result.path.map((name, i) => {
                  const isTarget = i === result.path.length - 1;
                  const s = students.find(x => x.name === name);
                  return (
                    <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div className={`path-node${isTarget ? ' target' : ''}`}>
                        <div style={{ fontWeight: 700 }}>{name}</div>
                        {s && (
                          <div style={{
                            fontSize: 10, fontFamily: 'JetBrains Mono',
                            color: isTarget ? 'var(--green)' : 'var(--orange)',
                            marginTop: 3,
                          }}>
                            {s.major}
                            {isTarget ? ` · ✓ worked at ${company}` : ''}
                          </div>
                        )}
                      </div>
                      {i < result.path.length - 1 && (
                        <span className="path-arrow">→</span>
                      )}
                    </div>
                  );
                })}
              </div>
            </>
          )}

          {queried && !hasPath && (
            <div className="no-path">
              <span style={{ fontSize: 18 }}>✕</span>
              <div>
                <div style={{ color: 'var(--text)', marginBottom: 4 }}>No referral path found</div>
                <div style={{ fontSize: 11 }}>
                  {startName} may already work there, the company may not exist in this
                  test case, or the graph is disconnected.
                </div>
              </div>
            </div>
          )}

          {result?.error && (
            <div className="no-path" style={{ color: 'var(--red)', marginTop: 10 }}>
              ✕ Server error: {result.error}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
