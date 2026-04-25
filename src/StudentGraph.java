import java.util.*;

/**
 * Weighted undirected graph that models the relationship network between students.
 *
 * <p>Each student is a node; a weighted edge between two students represents their
 * <em>connection strength</em> as computed by
 * {@link UniversityStudent#calculateConnectionStrength(Student)}.
 * Pairs of students with a connection strength of {@code 0} are <strong>not</strong>
 * connected by an edge (they are naturally disconnected).</p>
 *
 * <p>The graph is represented as an adjacency list:
 * {@code student → list of (neighbour, weight)} pairs.</p>
 *
 * <p>This graph is used by both {@link ReferralPathFinder} (Dijkstra's algorithm)
 * and {@link PodFormation} (Prim's algorithm).</p>
 */
public class StudentGraph {

    // -------------------------------------------------------------------------
    // Inner class: Edge
    // -------------------------------------------------------------------------

    /**
     * Represents a single directed edge from one student to another in the graph.
     *
     * <p>Because the graph is undirected, every logical edge is stored as two
     * {@code Edge} objects — one in each direction — with the same {@code weight}.</p>
     */
    public static class Edge {

        /** The student at the far end of this edge. */
        public final UniversityStudent neighbor;

        /** The connection-strength weight assigned to this edge. */
        public final int weight;

        /**
         * Constructs an {@code Edge} to the given neighbour with the given weight.
         *
         * @param neighbor the student this edge points to
         * @param weight   the connection strength between the two students
         */
        public Edge(UniversityStudent neighbor, int weight) {
            this.neighbor = neighbor;
            this.weight   = weight;
        }

        /**
         * Returns a compact string representation for debugging.
         *
         * @return a string of the form {@code "(neighbourName, weight)"}
         */
        @Override
        public String toString() {
            return "(" + neighbor.name + ", " + weight + ")";
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Adjacency list: maps each student node to its list of outgoing edges. */
    private final Map<UniversityStudent, List<Edge>> adjacencyList;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Builds the student graph from the given list of students.
     *
     * <p>For every pair {@code (A, B)}, the connection strength is calculated.
     * If the strength is greater than {@code 0}, an undirected edge is added in
     * both directions with that weight.  Pairs with a strength of {@code 0} are
     * intentionally left disconnected.</p>
     *
     * @param students the complete list of students to model as graph nodes
     */
    public StudentGraph(List<UniversityStudent> students) {
        adjacencyList = new LinkedHashMap<>();

        // Register every student as a node (even isolated ones)
        for (UniversityStudent student : students) {
            adjacencyList.put(student, new ArrayList<>());
        }

        // Add undirected weighted edges for every pair with strength > 0
        for (int i = 0; i < students.size(); i++) {
            for (int j = i + 1; j < students.size(); j++) {
                UniversityStudent a = students.get(i);
                UniversityStudent b = students.get(j);
                int weight = a.calculateConnectionStrength(b);
                if (weight > 0) {
                    adjacencyList.get(a).add(new Edge(b, weight));
                    adjacencyList.get(b).add(new Edge(a, weight));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Graph accessors
    // -------------------------------------------------------------------------

    /**
     * Returns all student nodes currently registered in the graph.
     *
     * @return a new list containing every {@link UniversityStudent} node;
     *         order mirrors the insertion order
     */
    public List<UniversityStudent> getAllNodes() {
        return new ArrayList<>(adjacencyList.keySet());
    }


    /**
     * Returns the student node with the given name, or {@code null} if no such
     * student exists in the graph.
     *
     * <p>Names are guaranteed to be unique within the system, so at most one
     * node will match.</p>
     *
     * @param name the student's display name to search for
     * @return the matching {@link UniversityStudent}, or {@code null} if not found
     */
    public UniversityStudent getStudent(String name) {
        for (UniversityStudent student : adjacencyList.keySet()) {
            if (student.name.equals(name)) {
                return student;
            }
        }
        return null;
    }

    /**
     * Returns the list of edges connecting the given student to its neighbours.
     *
     * @param student the node whose adjacency list is requested
     * @return the (possibly empty) list of {@link Edge} objects for that student;
     *         returns an empty list if the student is not in the graph
     */
    public List<Edge> getNeighbors(UniversityStudent student) {
        return adjacencyList.getOrDefault(student, Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    /**
     * Prints a human-readable adjacency list to standard output.
     *
     * <p>Each line has the format:
     * <pre>  StudentName -> [(NeighbourA, 5), (NeighbourB, 3)]</pre>
     * Isolated nodes are displayed with an empty bracket list.</p>
     */
    public void displayGraph() {
        System.out.println("\n--- Student Graph (Adjacency List) ---");
        for (Map.Entry<UniversityStudent, List<Edge>> entry : adjacencyList.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(entry.getKey().name).append(" -> [");
            List<Edge> edges = entry.getValue();
            for (int i = 0; i < edges.size(); i++) {
                sb.append(edges.get(i));
                if (i < edges.size() - 1) sb.append(", ");
            }
            sb.append("]");
            System.out.println(sb);
        }
        System.out.println("--------------------------------------\n");
    }
}