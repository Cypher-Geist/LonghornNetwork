import java.util.*;

/**
 * Groups students into <em>pods</em> (study or social groups) using a
 * maximum-weight spanning forest approach inspired by <em>Prim's algorithm</em>.
 *
 * <h3>Strategy</h3>
 * <p>Rather than minimising edge cost (the classic MST goal), the algorithm
 * <em>maximises</em> connection strength so that the most closely related
 * students end up in the same pod. The algorithm:</p>
 * <ol>
 *   <li>Grows a maximum spanning tree for each connected component using Prim's
 *       algorithm driven by a max-priority queue.</li>
 *   <li>Collects the resulting tree nodes in discovery order.</li>
 *   <li>Partitions that ordered list into consecutive groups of {@code podSize}
 *       (the final group may be smaller if the component does not divide evenly).</li>
 * </ol>
 *
 * <p>Isolated nodes (no edges) each form their own single-student pod.</p>
 *
 * <h3>Output format</h3>
 * <pre>
 * Pod Assignments:
 *   Pod 0: Alice, Bob, Charlie,
 *   Pod 1: Dave,
 * </pre>
 * Pods are numbered from {@code 0}. Each student name is followed by a comma.
 */
public class PodFormation {

    /** The graph used to determine connection strengths between students. */
    private final StudentGraph graph;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code PodFormation} instance backed by the given student graph.
     *
     * @param graph the student graph to use for pod formation; must not be {@code null}
     */
    public PodFormation(StudentGraph graph) {
        this.graph = graph;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Forms pods of (up to) {@code podSize} students each and prints the result.
     *
     * <p>Each connected component is processed independently. Within a component,
     * a maximum spanning tree is grown greedily via Prim's algorithm; students are
     * then grouped into pods of {@code podSize} in MST discovery order. The last
     * pod of a component may contain fewer students if the component size is not
     * a multiple of {@code podSize}.</p>
     *
     * <p>Output format (pods are 0-indexed; each name is followed by a comma):</p>
     * <pre>
     * Pod Assignments:
     *   Pod 0: Alice, Bob, Charlie, Dave,
     *   Pod 1: Eve,
     * </pre>
     *
     * @param podSize the maximum number of students per pod; must be ≥ 1
     * @throws IllegalArgumentException if {@code podSize} is less than 1
     */
    public void formPods(int podSize) {
        if (podSize < 1) {
            throw new IllegalArgumentException("podSize must be at least 1, got: " + podSize);
        }

        // Sort alphabetically by name so the starting node for each connected
        // component is deterministic and matches the expected output ordering.
        List<UniversityStudent> allNodes = graph.getAllNodes();
        allNodes.sort(Comparator.comparing(s -> s.name));
        Set<UniversityStudent> globalVisited = new HashSet<>();
        int podIndex = 0; // 0-indexed pod numbering

        System.out.println("Pod Assignments:");

        for (UniversityStudent startNode : allNodes) {
            if (globalVisited.contains(startNode)) continue;

            // Collect the connected component via Prim's maximum spanning tree
            List<UniversityStudent> component = primMaxSpanningTree(startNode, globalVisited);
            globalVisited.addAll(component);

            // Partition component into consecutive pods of size podSize
            for (int i = 0; i < component.size(); i += podSize) {
                int end = Math.min(i + podSize, component.size());
                List<UniversityStudent> pod = component.subList(i, end);

                // Build "Name, Name, Name," — each name followed by a comma
                List<String> names = new ArrayList<>();
                for (UniversityStudent s : pod) names.add(s.name);
                String podLine = String.join(", ", names) + ",";

                System.out.println("  Pod " + podIndex + ": " + podLine);
                podIndex++;
            }
        }
    }


    /**
     * Returns pod assignments as a list of name lists without printing anything.
     *
     * <p>Uses the same Prim maximum-spanning-forest logic as {@link #formPods(int)},
     * but returns the data so callers (e.g., the HTTP server) can serialize it
     * instead of printing to stdout.</p>
     *
     * @param podSize maximum students per pod; must be ≥ 1
     * @return ordered list of pods, each pod being an ordered list of student names
     */
    public List<List<String>> getPods(int podSize) {
        if (podSize < 1) podSize = 1;
        List<UniversityStudent> allNodes = graph.getAllNodes();
        allNodes.sort(Comparator.comparing(s -> s.name));
        Set<UniversityStudent> globalVisited = new HashSet<>();
        List<List<String>> result = new ArrayList<>();

        for (UniversityStudent startNode : allNodes) {
            if (globalVisited.contains(startNode)) continue;
            List<UniversityStudent> component = primMaxSpanningTree(startNode, globalVisited);
            globalVisited.addAll(component);
            for (int i = 0; i < component.size(); i += podSize) {
                int end = Math.min(i + podSize, component.size());
                List<String> pod = new ArrayList<>();
                for (UniversityStudent s : component.subList(i, end)) pod.add(s.name);
                result.add(pod);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Grows a maximum spanning tree rooted at {@code start} using Prim's algorithm
     * with a max-heap (priority queue).
     *
     * <p>When two candidate nodes have equal edge weight, the one discovered earlier
     * (lower insertion counter) is preferred, ensuring a consistent traversal order.</p>
     *
     * @param start         the root node from which to begin growing the tree
     * @param globalVisited nodes already assigned to a previous component;
     *                      these are skipped during traversal
     * @return an ordered list of students in this connected component,
     *         in the order they were added to the spanning tree
     */
    private List<UniversityStudent> primMaxSpanningTree(UniversityStudent start,
                                                        Set<UniversityStudent> globalVisited) {
        List<UniversityStudent> treeOrder = new ArrayList<>();
        Set<UniversityStudent> inTree = new HashSet<>();

        // Track insertion order so ties in weight are broken by discovery time.
        // Entry format: int[]{weight, insertionCounter}
        final int[] counter = {0};
        Map<Integer, UniversityStudent> indexToNode = new HashMap<>();

        // Max-heap: higher weight = higher priority; lower counter breaks ties
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a, b) -> {
            if (b[0] != a[0]) return Integer.compare(b[0], a[0]); // descending weight
            return Integer.compare(a[1], b[1]);                    // ascending counter (earlier first)
        });

        // Seed with the start node at maximum priority
        indexToNode.put(counter[0], start);
        maxHeap.add(new int[]{Integer.MAX_VALUE, counter[0]++});

        while (!maxHeap.isEmpty()) {
            int[] entry = maxHeap.poll();
            UniversityStudent current = indexToNode.get(entry[1]);

            if (inTree.contains(current) || globalVisited.contains(current)) continue;
            inTree.add(current);
            treeOrder.add(current);

            // Add all neighbours not yet in the tree to the heap
            for (StudentGraph.Edge edge : graph.getNeighbors(current)) {
                UniversityStudent neighbor = edge.neighbor;
                if (!inTree.contains(neighbor) && !globalVisited.contains(neighbor)) {
                    indexToNode.put(counter[0], neighbor);
                    maxHeap.add(new int[]{edge.weight, counter[0]++});
                }
            }
        }

        return treeOrder;
    }
}