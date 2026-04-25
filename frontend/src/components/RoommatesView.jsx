const MAJOR_COLORS = [
  '#bf5700','#4a8fe8','#3ecf6e','#9b72e8',
  '#e85050','#38c9b2','#e8a83e','#e87070',
];
const allMajors = (students) => [...new Set(students.map(s => s.major))];
const avatarColor = (name, students) => {
  const majors = allMajors(students);
  const s = students.find(x => x.name === name);
  const idx = s ? majors.indexOf(s.major) : 0;
  return MAJOR_COLORS[idx % MAJOR_COLORS.length];
};

export default function RoommatesView({ students }) {
  // Build unique pairs (avoid showing A-B and B-A)
  const seen = new Set();
  const pairs = [];
  for (const s of students) {
    if (s.roommate && !seen.has(s.name)) {
      pairs.push({ a: s, b: students.find(x => x.name === s.roommate) });
      seen.add(s.name);
      seen.add(s.roommate);
    }
  }
  const unpaired = students.filter(s => !s.roommate);

  return (
    <div>
      <div className="section-header">
        <div className="section-title">ROOMMATE ASSIGNMENTS</div>
        <div className="section-desc">
          Gale-Shapley stable matching · {pairs.length} pair{pairs.length !== 1 ? 's' : ''} ·
          {unpaired.length > 0 ? ` ${unpaired.length} unpaired` : ' all paired'}
        </div>
      </div>

      {/* Matched pairs */}
      {pairs.length > 0 && (
        <>
          <div style={{ fontSize: 11, letterSpacing: 2, color: 'var(--text-muted)',
            fontFamily: 'JetBrains Mono', textTransform: 'uppercase', marginBottom: 12 }}>
            Matched Pairs
          </div>
          <div className="pair-grid" style={{ marginBottom: 28 }}>
            {pairs.map(({ a, b }) => (
              <div key={a.name} className="pair-card">
                {/* Student A */}
                <div className="pair-avatar"
                  style={{ background: avatarColor(a.name, students) }}>
                  {a.name[0]}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="pair-name">{a.name}</div>
                  <div className="pair-meta">{a.major} · GPA {a.gpa}</div>
                </div>

                <span className="pair-heart">♥</span>

                {/* Student B */}
                <div style={{ flex: 1, minWidth: 0, textAlign: 'right' }}>
                  <div className="pair-name">{b?.name}</div>
                  <div className="pair-meta">{b?.major} · GPA {b?.gpa}</div>
                </div>
                <div className="pair-avatar"
                  style={{ background: avatarColor(b?.name, students) }}>
                  {b?.name?.[0]}
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Unpaired students */}
      {unpaired.length > 0 && (
        <>
          <div style={{ fontSize: 11, letterSpacing: 2, color: 'var(--text-muted)',
            fontFamily: 'JetBrains Mono', textTransform: 'uppercase', marginBottom: 12 }}>
            Unpaired Students
          </div>
          <div className="pair-grid">
            {unpaired.map(s => (
              <div key={s.name} className="pair-card" style={{ opacity: 0.6 }}>
                <div className="pair-avatar"
                  style={{ background: avatarColor(s.name, students) }}>
                  {s.name[0]}
                </div>
                <div style={{ flex: 1 }}>
                  <div className="pair-name">{s.name}</div>
                  <div className="pair-meta">{s.major}</div>
                  {s.roommatePreferences.length === 0 && (
                    <div className="pair-meta" style={{ color: 'var(--text-dim)', marginTop: 2 }}>
                      No preferences listed
                    </div>
                  )}
                </div>
                <span className="unpaired-badge">unpaired</span>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
