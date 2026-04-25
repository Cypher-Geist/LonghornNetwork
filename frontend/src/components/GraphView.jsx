import { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';

// One colour per major, consistent across renders
const MAJOR_COLORS = [
  '#bf5700', '#4a8fe8', '#3ecf6e', '#9b72e8',
  '#e85050', '#38c9b2', '#e8a83e', '#e87070',
];

function majorColor(major, majors) {
  const idx = majors.indexOf(major);
  return MAJOR_COLORS[idx % MAJOR_COLORS.length];
}

export default function GraphView({ students, edges }) {
  const svgRef       = useRef(null);
  const containerRef = useRef(null);
  const simRef       = useRef(null);
  const [tooltip, setTooltip] = useState(null); // { student, x, y }

  const majors = [...new Set(students.map(s => s.major))];

  useEffect(() => {
    if (!students.length || !svgRef.current || !containerRef.current) return;

    const W = containerRef.current.clientWidth || 900;
    const H = 520;

    // ── Data ──
    const nodes = students.map(s => ({ ...s, id: s.name }));
    const links = edges.map(e => ({ source: e.source, target: e.target, weight: e.weight }));

    // ── SVG setup ──
    const svg = d3.select(svgRef.current);
    svg.selectAll('*').remove();
    svg.attr('viewBox', `0 0 ${W} ${H}`).attr('width', W).attr('height', H);

    // Background grid (decorative)
    const defs = svg.append('defs');
    const pattern = defs.append('pattern')
      .attr('id', 'grid').attr('width', 40).attr('height', 40)
      .attr('patternUnits', 'userSpaceOnUse');
    pattern.append('path')
      .attr('d', 'M 40 0 L 0 0 0 40')
      .attr('fill', 'none').attr('stroke', '#1e1e22').attr('stroke-width', 1);
    svg.append('rect').attr('width', W).attr('height', H).attr('fill', 'url(#grid)');

    // ── Simulation ──
    const sim = d3.forceSimulation(nodes)
      .force('link',      d3.forceLink(links).id(d => d.id).distance(d => 220 - d.weight * 12))
      .force('charge',    d3.forceManyBody().strength(-500))
      .force('center',    d3.forceCenter(W / 2, H / 2))
      .force('collision', d3.forceCollide(50));
    simRef.current = sim;

    // ── Edge lines ──
    const linkG = svg.append('g');
    const link = linkG.selectAll('line')
      .data(links).enter().append('line')
      .attr('stroke', d => {
        // Highlight roommate edges (weight includes +4 roommate bonus = likely ≥ 4)
        const sNode = nodes.find(n => n.id === (d.source.id || d.source));
        const tNode = nodes.find(n => n.id === (d.target.id || d.target));
        if (sNode && tNode && sNode.roommate === tNode.name) return '#bf5700';
        return '#2e2e36';
      })
      .attr('stroke-width', d => Math.max(1.5, d.weight * 0.4))
      .attr('stroke-opacity', 0.85);

    // ── Edge weight labels ──
    const linkLabelG = svg.append('g');
    const linkLabel = linkLabelG.selectAll('text')
      .data(links).enter().append('text')
      .attr('fill', '#4a4644')
      .attr('font-size', '11px')
      .attr('font-family', 'JetBrains Mono, monospace')
      .attr('text-anchor', 'middle')
      .attr('pointer-events', 'none')
      .text(d => d.weight);

    // ── Node groups ──
    const nodeG = svg.append('g').selectAll('g')
      .data(nodes).enter().append('g')
      .style('cursor', 'grab')
      .call(
        d3.drag()
          .on('start', (event, d) => {
            if (!event.active) sim.alphaTarget(0.3).restart();
            d.fx = d.x; d.fy = d.y;
          })
          .on('drag',  (event, d) => { d.fx = event.x; d.fy = event.y; })
          .on('end',   (event, d) => {
            if (!event.active) sim.alphaTarget(0);
            d.fx = null; d.fy = null;
          })
      );

    // Glow filter for roommated nodes
    const filter = defs.append('filter').attr('id', 'glow');
    filter.append('feGaussianBlur').attr('stdDeviation', '4').attr('result', 'blur');
    const merge = filter.append('feMerge');
    merge.append('feMergeNode').attr('in', 'blur');
    merge.append('feMergeNode').attr('in', 'SourceGraphic');

    // Outer ring for roommate nodes
    nodeG.filter(d => !!d.roommate)
      .append('circle')
      .attr('r', 34)
      .attr('fill', 'none')
      .attr('stroke', '#bf5700')
      .attr('stroke-width', 1.5)
      .attr('stroke-dasharray', '4 3')
      .attr('opacity', 0.6);

    // Main circle
    nodeG.append('circle')
      .attr('r', 26)
      .attr('fill', d => majorColor(d.major, majors))
      .attr('stroke', '#0c0c0e')
      .attr('stroke-width', 2)
      .attr('filter', d => d.roommate ? 'url(#glow)' : null);

    // Name label
    nodeG.append('text')
      .attr('text-anchor', 'middle')
      .attr('dy', '0.35em')
      .attr('fill', '#fff')
      .attr('font-size', '12px')
      .attr('font-family', 'Rubik, sans-serif')
      .attr('font-weight', '600')
      .attr('pointer-events', 'none')
      .text(d => d.name.length > 7 ? d.name.slice(0, 6) + '…' : d.name);

    // Roommate heart badge
    nodeG.filter(d => !!d.roommate)
      .append('text')
      .attr('text-anchor', 'middle')
      .attr('dy', '2.4em')
      .attr('fill', '#ff8c3a')
      .attr('font-size', '9px')
      .attr('font-family', 'JetBrains Mono, monospace')
      .attr('pointer-events', 'none')
      .text(d => `♥ ${d.roommate}`);

    // Tooltip on hover
    nodeG
      .on('mouseover', (event, d) => {
        setTooltip({ student: d, x: event.clientX + 12, y: event.clientY - 10 });
      })
      .on('mousemove', (event) => {
        setTooltip(prev => prev ? { ...prev, x: event.clientX + 12, y: event.clientY - 10 } : null);
      })
      .on('mouseout',  () => setTooltip(null));

    // Tick
    sim.on('tick', () => {
      // Clamp nodes inside SVG
      nodes.forEach(d => {
        d.x = Math.max(36, Math.min(W - 36, d.x));
        d.y = Math.max(36, Math.min(H - 36, d.y));
      });

      link
        .attr('x1', d => d.source.x).attr('y1', d => d.source.y)
        .attr('x2', d => d.target.x).attr('y2', d => d.target.y);

      linkLabel
        .attr('x', d => (d.source.x + d.target.x) / 2)
        .attr('y', d => (d.source.y + d.target.y) / 2 - 5);

      nodeG.attr('transform', d => `translate(${d.x},${d.y})`);
    });

    return () => { sim.stop(); setTooltip(null); };
  }, [students, edges]);

  return (
    <div>
      <div className="section-header">
        <div className="section-title">STUDENT GRAPH</div>
        <div className="section-desc">
          Force-directed graph · nodes = students · edge weight = connection strength
          · dashed orange ring = has roommate · drag nodes to explore
        </div>
      </div>

      <div className="graph-container" ref={containerRef}>
        <svg ref={svgRef} className="graph-svg" style={{ minHeight: 520 }} />
      </div>

      {/* Legend */}
      <div className="graph-legend" style={{ marginTop: 16 }}>
        {majors.map((m, i) => (
          <div key={m} className="legend-item">
            <div className="legend-dot" style={{ background: majorColor(m, majors) }} />
            {m}
          </div>
        ))}
        <div className="legend-item">
          <svg width="14" height="14">
            <circle cx="7" cy="7" r="6" fill="none" stroke="#bf5700"
              strokeWidth="1.5" strokeDasharray="3 2" />
          </svg>
          Has roommate
        </div>
        <div className="legend-item">
          <svg width="22" height="6">
            <line x1="0" y1="3" x2="22" y2="3" stroke="#bf5700" strokeWidth="2.5" />
          </svg>
          Roommate edge
        </div>
      </div>

      {/* Hover tooltip */}
      {tooltip && (
        <div className="student-tooltip"
          style={{ left: tooltip.x, top: tooltip.y }}>
          <div className="tooltip-name">{tooltip.student.name}</div>
          {[
            ['Major',  tooltip.student.major],
            ['GPA',    tooltip.student.gpa],
            ['Year',   `Year ${tooltip.student.year}`],
            ['Age',    tooltip.student.age],
            ['Gender', tooltip.student.gender],
            ['Roommate', tooltip.student.roommate ?? 'None'],
          ].map(([k, v]) => (
            <div key={k} className="tooltip-row">
              <span className="tooltip-key">{k}</span>
              <span className="tooltip-val">{v}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
