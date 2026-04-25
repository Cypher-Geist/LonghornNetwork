import java.util.*;

/**
 * Finds the <em>strongest referral path</em> from a starting student to the
 * nearest student who has previously interned at a target company.
 *
 * <h3>Algorithm</h3>
 * <p>Dijkstra's shortest-path algorithm is applied on the {@link StudentGraph},
 * but with <em>inverted</em> edge weights ({@code 10 - connectionStrength}).
 * This transforms "maximize connection strength" into the standard
 * "minimize path cost" problem that Dijkstra solves efficiently.
 * A {@link PriorityQueue} (min-heap) drives the traversal.</p>
 *
 * <h3>Edge cases</h3>
 * <ul>
 *   <li>The start student already has the target internship — referral not needed;
 *       prints "No referral path found" and returns an empty list.</li>
 *   <li>No student has the target internship → empty list.</li>
 *   <li>The start is disconnected from all students with the target → empty list.</li>
 * </ul>
 *
 * <h3>Output format</h3>
 * <pre>
 * For Alice:
 * Alice -> Bob -> Ivy
 *
 * For Alice:
 * No referral path found
 * </pre>
 */
public class ReferralPathFinder {

    /** The graph on which Dijkstra's algorithm is executed. */
    private final StudentGraph graph;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code ReferralPathFinder} backed by the given graph.
     *
     * @param graph the student graph to search; must not be {@code null}
     */
    public ReferralPathFinder(StudentGraph graph) {
        this.graph = graph;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Finds and prints the highest-strength referral path from {@code start} to the
     * first reachable student who has {@code targetCompany} in their internship list.
     *
     * <p>If the start student already has the target internship, or if no reachable
     * student has it, the method prints "No referral path found" and returns an
     * empty list.</p>
     *
     * <p>Output format:</p>
     * <pre>
     * For {startName}:
     * {startName} -> {next} -> ... -> {target}
     * </pre>
     * or:
     * <pre>
     * For {startName}:
     * No referral path found
     * </pre>
     *
     * @param start         the student initiating the referral search
     * @param targetCompany the company name to search for in internship histories
     * @return an ordered list of {@link UniversityStudent} objects forming the
     *         referral chain, or an empty list if no path exists
     */
    public List<UniversityStudent> findReferralPath(UniversityStudent start,
                                                    String targetCompany) {
        System.out.println("For " + start.name + ":");

        // If the start student already interned there, no referral path is needed
        if (start.previousInternships.contains(targetCompany)) {
            System.out.println("No referral path found");
            System.out.println();
            return new ArrayList<>();
        }

        // --- Initialise distance map ---
        Map<UniversityStudent, Double> dist = new HashMap<>();
        for (UniversityStudent node : graph.getAllNodes()) {
            dist.put(node, Double.MAX_VALUE);
        }
        dist.put(start, 0.0);

        // --- Previous-node map for path reconstruction ---
        Map<UniversityStudent, UniversityStudent> prev = new HashMap<>();

        // --- Min-heap ordered by tentative distance ---
        PriorityQueue<UniversityStudent> pq = new PriorityQueue<>(
                Comparator.comparingDouble(s -> dist.getOrDefault(s, Double.MAX_VALUE))
        );
        pq.add(start);

        Set<UniversityStudent> visited = new HashSet<>();
        UniversityStudent target = null;

        // --- Dijkstra main loop ---
        while (!pq.isEmpty()) {
            UniversityStudent current = pq.poll();

            // Lazy-deletion: skip if already settled
            if (visited.contains(current)) continue;
            visited.add(current);

            // Check whether this node (excluding start) has the desired internship
            if (current != start && current.previousInternships.contains(targetCompany)) {
                target = current;
                break;
            }

            // Relax outgoing edges using inverted weight (10 - w)
            for (StudentGraph.Edge edge : graph.getNeighbors(current)) {
                UniversityStudent neighbor = edge.neighbor;
                if (visited.contains(neighbor)) continue;

                double invertedCost = 10.0 - edge.weight;
                double newDist = dist.get(current) + invertedCost;

                if (newDist < dist.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    dist.put(neighbor, newDist);
                    prev.put(neighbor, current);
                    pq.add(neighbor);
                }
            }
        }

        // --- Reconstruct and print path ---
        if (target == null) {
            System.out.println("No referral path found");
            System.out.println();
            return new ArrayList<>();
        }

        List<UniversityStudent> path = new ArrayList<>();
        for (UniversityStudent cur = target; cur != null; cur = prev.get(cur)) {
            path.add(0, cur);
        }

        System.out.println(formatPath(path));
        System.out.println();
        return path;
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Formats a referral path as a human-readable arrow chain.
     *
     * @param path the ordered list of students in the path
     * @return a string such as {@code "Alice -> Bob -> Ivy"}
     */
    private String formatPath(List<UniversityStudent> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            sb.append(path.get(i).name);
            if (i < path.size() - 1) sb.append(" -> ");
        }
        return sb.toString();
    }
}