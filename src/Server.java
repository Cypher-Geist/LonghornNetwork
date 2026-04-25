import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Lightweight HTTP server that backs the Longhorn Network React UI.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /data} — returns a single JSON object containing all three
 *       pre-computed test cases (students, graph edges, roommate assignments,
 *       pod assignments, friend lists, chat histories).</li>
 *   <li>{@code GET /referral?tc=N&start=NAME&company=COMPANY} — dynamically
 *       runs Dijkstra's algorithm and returns the referral path as JSON.</li>
 * </ul>
 *
 * <h3>Running</h3>
 * <pre>
 *   cd src
 *   javac *.java
 *   java Server
 * </pre>
 * Then start the React dev server in a separate terminal:
 * <pre>
 *   cd frontend
 *   npm install
 *   npm run dev
 * </pre>
 * Open {@code http://localhost:5173} in your browser.
 */
public class Server {

    /** Port the HTTP server listens on. */
    private static final int PORT = 9090;

    /** Cached JSON string built once at startup. */
    private static String cachedData;

    /**
     * Post-GaleShapley graphs kept for live referral queries.
     * Index 0 = test case 1, index 1 = test case 2, index 2 = test case 3.
     */
    private static final List<StudentGraph> graphs = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * Starts the Longhorn Network backend server.
     *
     * <p>Pre-computes all three test cases on startup (suppressing the normal
     * algorithm log output), then begins accepting HTTP connections.</p>
     *
     * @param args command-line arguments (unused)
     * @throws Exception if the server socket cannot be opened
     */
    public static void main(String[] args) throws Exception {
        System.out.println("[Server] Pre-computing test cases...");
        precompute();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/data",     Server::handleData);
        server.createContext("/referral", Server::handleReferral);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("[Server] Longhorn Network API running at http://localhost:" + PORT);
        System.out.println("[Server] Start the UI:  cd frontend && npm install && npm run dev");
    }

    // -------------------------------------------------------------------------
    // Pre-computation
    // -------------------------------------------------------------------------

    /**
     * Generates all three test cases, runs every algorithm, and serialises the
     * results to {@link #cachedData}. Standard output is silenced during the
     * computation phase so the algorithm logging does not pollute the server log.
     *
     * @throws InterruptedException if a thread pool does not shut down in time
     */
    private static void precompute() throws InterruptedException {
        // Redirect stdout to /dev/null during computation
        PrintStream original = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override public void write(int b) { /* null sink */ }
        }));

        try {
            List<List<UniversityStudent>> cases = Arrays.asList(
                Main.generateTestCase1(),
                Main.generateTestCase2(),
                Main.generateTestCase3()
            );

            StringBuilder sb = new StringBuilder("{\"testCases\":[");

            for (int i = 0; i < cases.size(); i++) {
                List<UniversityStudent> students = cases.get(i);

                // 1. Build pre-GaleShapley graph (used for pod formation)
                StudentGraph preGraph = new StudentGraph(students);
                PodFormation pf = new PodFormation(preGraph);
                List<List<String>> pods = pf.getPods(4);

                // 2. Assign roommates (modifies students in-place)
                GaleShapley.assignRoommates(students);

                // 3. Rebuild graph with roommate bonuses now included
                StudentGraph postGraph = new StudentGraph(students);
                graphs.add(postGraph);

                // 4. Simulate friend requests and chat messages via threads
                runSocialThreads(students);

                if (i > 0) sb.append(",");
                appendTestCaseJson(sb, i + 1, students, postGraph, pods);
            }

            sb.append("]}");
            cachedData = sb.toString();

        } finally {
            System.setOut(original);
            System.out.println("[Server] Pre-computation complete — 3 test cases ready.");
        }
    }

    /**
     * Runs {@link FriendRequestThread} and {@link ChatThread} tasks for the
     * first two students in the list, mirroring the logic in {@link Main}.
     *
     * @param students the student list; must contain at least 2 entries
     * @throws InterruptedException if the executor does not terminate in time
     */
    private static void runSocialThreads(List<UniversityStudent> students)
            throws InterruptedException {
        if (students.size() < 2) return;
        UniversityStudent s1 = students.get(0);
        UniversityStudent s2 = students.get(1);
        ExecutorService exec = Executors.newFixedThreadPool(4);
        exec.submit(new FriendRequestThread(s1, s2));
        exec.submit(new ChatThread(s1, s2, "Hello there!"));
        exec.submit(new FriendRequestThread(s2, s1));
        exec.submit(new ChatThread(s2, s1, "Hi back!"));
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);
    }

    // -------------------------------------------------------------------------
    // HTTP handlers
    // -------------------------------------------------------------------------

    /**
     * Handles {@code GET /data} — returns the pre-computed JSON for all test cases.
     *
     * @param exchange the incoming HTTP exchange
     * @throws IOException if the response cannot be written
     */
    private static void handleData(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        sendJson(exchange, cachedData);
    }

    /**
     * Handles {@code GET /referral?tc=N&start=NAME&company=COMPANY}.
     *
     * <p>Dynamically runs Dijkstra's algorithm on the stored post-GaleShapley
     * graph for test case {@code N} and returns the referral path as JSON.</p>
     *
     * @param exchange the incoming HTTP exchange
     * @throws IOException if the response cannot be written
     */
    private static void handleReferral(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        int tcIndex = Integer.parseInt(params.getOrDefault("tc", "1")) - 1;
        String startName = params.getOrDefault("start", "");
        String company   = params.getOrDefault("company", "");

        // Suppress algorithm logging during the live query
        PrintStream original = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override public void write(int b) { /* null sink */ }
        }));

        String json;
        try {
            if (tcIndex < 0 || tcIndex >= graphs.size()) {
                json = "{\"path\":[],\"error\":\"Invalid test case index\"}";
            } else {
                StudentGraph graph = graphs.get(tcIndex);
                UniversityStudent start = graph.getStudent(startName);
                if (start == null) {
                    json = "{\"path\":[],\"error\":\"Student not found: " + esc(startName) + "\"}";
                } else {
                    ReferralPathFinder finder = new ReferralPathFinder(graph);
                    List<UniversityStudent> path = finder.findReferralPath(start, company);
                    StringBuilder sb = new StringBuilder("{\"path\":[");
                    for (int i = 0; i < path.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append("\"").append(esc(path.get(i).name)).append("\"");
                    }
                    sb.append("]}");
                    json = sb.toString();
                }
            }
        } finally {
            System.setOut(original);
        }

        sendJson(exchange, json);
    }

    // -------------------------------------------------------------------------
    // JSON serialisation helpers
    // -------------------------------------------------------------------------

    /**
     * Appends the full JSON object for one test case to the given builder.
     *
     * @param sb       the builder to append to
     * @param id       the 1-based test case number
     * @param students the list of students (with roommates already assigned)
     * @param graph    the post-GaleShapley graph
     * @param pods     pod assignments as lists of student names
     */
    private static void appendTestCaseJson(StringBuilder sb, int id,
            List<UniversityStudent> students,
            StudentGraph graph,
            List<List<String>> pods) {

        sb.append("{\"id\":").append(id).append(",");

        // --- Students ---
        sb.append("\"students\":[");
        for (int i = 0; i < students.size(); i++) {
            if (i > 0) sb.append(",");
            appendStudentJson(sb, students.get(i));
        }
        sb.append("],");

        // --- Edges (deduplicated, undirected) ---
        sb.append("\"edges\":[");
        Set<String> seen = new HashSet<>();
        boolean firstEdge = true;
        for (UniversityStudent s : graph.getAllNodes()) {
            for (StudentGraph.Edge e : graph.getNeighbors(s)) {
                // Canonical key: alphabetically smaller name first
                String key = s.name.compareTo(e.neighbor.name) < 0
                        ? s.name + "||" + e.neighbor.name
                        : e.neighbor.name + "||" + s.name;
                if (seen.add(key)) {
                    if (!firstEdge) sb.append(",");
                    sb.append("{\"source\":\"").append(esc(s.name)).append("\",");
                    sb.append("\"target\":\"").append(esc(e.neighbor.name)).append("\",");
                    sb.append("\"weight\":").append(e.weight).append("}");
                    firstEdge = false;
                }
            }
        }
        sb.append("],");

        // --- Pods ---
        sb.append("\"pods\":[");
        for (int i = 0; i < pods.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"index\":").append(i).append(",\"members\":[");
            List<String> members = pods.get(i);
            for (int j = 0; j < members.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append("\"").append(esc(members.get(j))).append("\"");
            }
            sb.append("]}");
        }
        sb.append("]");

        sb.append("}");
    }

    /**
     * Appends a single student as a JSON object.
     *
     * @param sb the builder to append to
     * @param s  the student to serialise
     */
    private static void appendStudentJson(StringBuilder sb, UniversityStudent s) {
        sb.append("{");
        sb.append("\"name\":\"").append(esc(s.name)).append("\",");
        sb.append("\"age\":").append(s.age).append(",");
        sb.append("\"gender\":\"").append(esc(s.gender)).append("\",");
        sb.append("\"year\":").append(s.year).append(",");
        sb.append("\"major\":\"").append(esc(s.major)).append("\",");
        sb.append("\"gpa\":").append(s.gpa).append(",");

        // roommatePreferences
        sb.append("\"roommatePreferences\":[");
        for (int i = 0; i < s.roommatePreferences.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(esc(s.roommatePreferences.get(i))).append("\"");
        }
        sb.append("],");

        // previousInternships
        sb.append("\"previousInternships\":[");
        for (int i = 0; i < s.previousInternships.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(esc(s.previousInternships.get(i))).append("\"");
        }
        sb.append("],");

        // roommate (nullable)
        if (s.getRoommate() != null) {
            sb.append("\"roommate\":\"").append(esc(s.getRoommate().name)).append("\",");
        } else {
            sb.append("\"roommate\":null,");
        }

        // friends (names only to avoid circular references)
        sb.append("\"friends\":[");
        for (int i = 0; i < s.friends.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(esc(s.friends.get(i).name)).append("\"");
        }
        sb.append("],");

        // chatHistory: { "partnerName": ["msg1", "msg2"] }
        sb.append("\"chatHistory\":{");
        Map<String, List<String>> chat = s.getChatHistory();
        boolean firstEntry = true;
        for (Map.Entry<String, List<String>> entry : chat.entrySet()) {
            if (!firstEntry) sb.append(",");
            sb.append("\"").append(esc(entry.getKey())).append("\":[");
            List<String> msgs = entry.getValue();
            for (int i = 0; i < msgs.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(esc(msgs.get(i))).append("\"");
            }
            sb.append("]");
            firstEntry = false;
        }
        sb.append("}");

        sb.append("}");
    }

    // -------------------------------------------------------------------------
    // HTTP utilities
    // -------------------------------------------------------------------------

    /**
     * Writes a JSON string as the HTTP response with status 200.
     *
     * @param exchange the HTTP exchange to respond to
     * @param json     the JSON string to send
     * @throws IOException if writing fails
     */
    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Adds CORS headers to every response so the React dev server on port 5173
     * can reach the API on port 8080 without browser security errors.
     *
     * @param exchange the HTTP exchange to add headers to
     */
    private static void addCorsHeaders(HttpExchange exchange) {
        Headers h = exchange.getResponseHeaders();
        h.set("Access-Control-Allow-Origin",  "*");
        h.set("Access-Control-Allow-Methods", "GET, OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type");
    }

    /**
     * Parses a URL query string into a key-value map.
     *
     * @param query the raw query string (may be {@code null})
     * @return a map of decoded parameter names to decoded values
     */
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    map.put(URLDecoder.decode(kv[0], "UTF-8"),
                            URLDecoder.decode(kv[1], "UTF-8"));
                } catch (IOException e) {
                    map.put(kv[0], kv[1]);
                }
            }
        }
        return map;
    }

    /**
     * Escapes a string for safe inclusion inside a JSON string literal.
     *
     * @param s the raw string (may be {@code null})
     * @return a JSON-safe escaped string, or an empty string for {@code null} input
     */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
