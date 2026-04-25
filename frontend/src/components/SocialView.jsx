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

export default function SocialView({ students }) {
  return (
    <div>
      <div className="section-header">
        <div className="section-title">SOCIAL ACTIVITY</div>
        <div className="section-desc">
          Friend requests and chat history from FriendRequestThread & ChatThread ·
          executed concurrently via ExecutorService
        </div>
      </div>

      <div className="social-grid">
        {students.map(s => {
          const hasFriends = s.friends && s.friends.length > 0;
          const chatEntries = Object.entries(s.chatHistory || {});
          const hasChat = chatEntries.length > 0;

          return (
            <div key={s.name} className="social-card">
              {/* Header */}
              <div className="social-card-header">
                <div className="social-avatar"
                  style={{ background: avatarColor(s.name, students) }}>
                  {s.name[0]}
                </div>
                <div>
                  <div className="social-name">{s.name}</div>
                  <div className="social-major">{s.major} · Year {s.year}</div>
                </div>
              </div>

              <div className="social-body">
                {/* Friends */}
                <div>
                  <div className="social-section-label">Friend Requests</div>
                  {hasFriends ? (
                    <div className="friends-list">
                      {s.friends.map(f => (
                        <span key={f} className="friend-chip">+ {f}</span>
                      ))}
                    </div>
                  ) : (
                    <span className="none-label">None</span>
                  )}
                </div>

                {/* Chat history */}
                <div>
                  <div className="social-section-label">Chat History</div>
                  {hasChat ? (
                    chatEntries.map(([partner, messages]) => (
                      <div key={partner} style={{ marginBottom: 8 }}>
                        <div style={{
                          fontSize: 10,
                          color: 'var(--text-muted)',
                          fontFamily: 'JetBrains Mono',
                          marginBottom: 4,
                        }}>
                          with {partner}
                        </div>
                        {messages.map((msg, i) => (
                          <div key={i} className="chat-bubble">{msg}</div>
                        ))}
                      </div>
                    ))
                  ) : (
                    <span className="none-label">None</span>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
