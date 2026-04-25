const POD_ACCENT_COLORS = [
  '#bf5700','#4a8fe8','#3ecf6e','#9b72e8',
  '#38c9b2','#e8a83e','#e87070','#e85050',
];

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

export default function PodsView({ pods, students }) {
  return (
    <div>
      <div className="section-header">
        <div className="section-title">POD ASSIGNMENTS</div>
        <div className="section-desc">
          Prim maximum-spanning-forest · students grouped by highest connection strength ·
          {pods.length} pod{pods.length !== 1 ? 's' : ''} total
        </div>
      </div>

      <div className="pods-grid">
        {pods.map((pod, i) => (
          <div key={i} className="pod-card"
            style={{ borderTopColor: POD_ACCENT_COLORS[i % POD_ACCENT_COLORS.length] }}>
            <div className="pod-label" style={{ color: POD_ACCENT_COLORS[i % POD_ACCENT_COLORS.length] }}>
              POD {pod.index} — {pod.members.length} member{pod.members.length !== 1 ? 's' : ''}
            </div>
            <div className="pod-members">
              {pod.members.map(name => {
                const s = students.find(x => x.name === name);
                return (
                  <div key={name} className="pod-member">
                    <div style={{
                      width: 28, height: 28, borderRadius: '50%',
                      background: avatarColor(name, students),
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 12, fontFamily: 'Bebas Neue', color: '#fff',
                      flexShrink: 0,
                    }}>
                      {name[0]}
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: 13 }}>{name}</div>
                      {s && (
                        <div style={{
                          fontSize: 10, color: 'var(--text-muted)',
                          fontFamily: 'JetBrains Mono',
                        }}>
                          {s.major} · {s.roommate ? `♥ ${s.roommate}` : 'unpaired'}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
