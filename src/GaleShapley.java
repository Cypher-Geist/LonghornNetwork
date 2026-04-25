import java.util.*;

/**
 * Implements the <em>Gale-Shapley</em> stable-matching algorithm adapted for
 * the roommate-assignment problem in the Longhorn Network.
 *
 * <h3>Algorithm summary</h3>
 * <ol>
 *   <li>All students with non-empty roommate-preference lists are placed in a
 *       proposal queue.</li>
 *   <li>While the queue is not empty:
 *     <ol type="a">
 *       <li>Dequeue a student {@code P} (proposer). If already paired, skip.</li>
 *       <li>{@code P} proposes to the next name on their preference list.</li>
 *       <li>If the receiver {@code R} is free, the pair is formed immediately.</li>
 *       <li>If {@code R} is already paired with {@code C}:
 *         <ul>
 *           <li>If {@code R} prefers {@code P} over {@code C}, {@code R} switches
 *               and {@code C} is returned to the queue.</li>
 *           <li>Otherwise {@code R} rejects {@code P}, who tries their next
 *               preference.</li>
 *         </ul>
 *       </li>
 *     </ol>
 *   </li>
 *   <li>Students who exhaust their preference list remain unpaired.</li>
 * </ol>
 *
 * <h3>Edge cases handled</h3>
 * <ul>
 *   <li>Partial or empty preference lists — students with no preferences are ignored.</li>
 *   <li>Cyclic preferences — the queue empties because each rejection advances the
 *       proposer's index irreversibly.</li>
 *   <li>Preferences referencing names not in the student list — those entries are skipped.</li>
 * </ul>
 */
public class GaleShapley {

    /**
     * Assigns roommates to students in-place using the Gale-Shapley algorithm,
     * then prints the results.
     *
     * <p>Output format (printed to stdout):</p>
     * <pre>
     * Roommate Assignment:
     * Alice is roommates with Bob
     * Bob is roommates with Alice
     * </pre>
     *
     * <p>Only paired students are printed. Students without a roommate are omitted
     * from the output.</p>
     *
     * @param students the full list of students to match; modified in-place
     */
    public static void assignRoommates(List<UniversityStudent> students) {

        // Build a fast name → student lookup
        Map<String, UniversityStudent> nameMap = new HashMap<>();
        for (UniversityStudent s : students) {
            nameMap.put(s.name, s);
        }

        // Track how far each student has advanced through their preference list
        Map<String, Integer> proposalIndex = new HashMap<>();
        for (UniversityStudent s : students) {
            proposalIndex.put(s.name, 0);
        }

        // Initialise the queue with every student who has preferences
        Queue<UniversityStudent> freeQueue = new LinkedList<>();
        for (UniversityStudent s : students) {
            if (!s.roommatePreferences.isEmpty()) {
                freeQueue.add(s);
            }
        }

        while (!freeQueue.isEmpty()) {
            UniversityStudent proposer = freeQueue.poll();

            // Already paired — nothing to do
            if (proposer.getRoommate() != null) continue;

            int idx = proposalIndex.get(proposer.name);

            // Proposer has exhausted all preferences — leave unpaired
            if (idx >= proposer.roommatePreferences.size()) continue;

            String prefName = proposer.roommatePreferences.get(idx);
            proposalIndex.put(proposer.name, idx + 1); // advance before any early return

            UniversityStudent receiver = nameMap.get(prefName);

            // Preferred student not found in the student list — skip to next pref
            if (receiver == null) {
                freeQueue.add(proposer);
                continue;
            }

            if (receiver.getRoommate() == null) {
                // Receiver is free: form the pair
                proposer.setRoommate(receiver);
                receiver.setRoommate(proposer);

            } else {
                // Receiver is already paired — compare the proposer against the current partner
                UniversityStudent currentPartner = receiver.getRoommate();

                int rankProposer = receiver.roommatePreferences.indexOf(proposer.name);
                int rankCurrent  = receiver.roommatePreferences.indexOf(currentPartner.name);

                boolean receiverPrefersProposer =
                        (rankProposer != -1) &&
                                (rankCurrent == -1 || rankProposer < rankCurrent);

                if (receiverPrefersProposer) {
                    // Receiver switches to proposer; current partner is freed
                    currentPartner.setRoommate(null);
                    proposer.setRoommate(receiver);
                    receiver.setRoommate(proposer);

                    // Return the evicted partner to the queue if they have more preferences
                    if (proposalIndex.get(currentPartner.name) <
                            currentPartner.roommatePreferences.size()) {
                        freeQueue.add(currentPartner);
                    }
                } else {
                    // Receiver rejects proposer — proposer tries their next preference
                    if (proposalIndex.get(proposer.name) <
                            proposer.roommatePreferences.size()) {
                        freeQueue.add(proposer);
                    }
                }
            }
        }

        // Print results in the expected format
        printAssignments(students);
    }

    /**
     * Prints the roommate assignments for all paired students in the order they
     * appear in the provided student list.
     *
     * <p>Only students who have been assigned a roommate are printed.
     * Both directions of each pair are printed (i.e. "A is roommates with B"
     * and "B is roommates with A" both appear).</p>
     *
     * <p>Output format:</p>
     * <pre>
     * Roommate Assignment:
     * Alice is roommates with Bob
     * Bob is roommates with Alice
     * </pre>
     *
     * @param students the list of students whose assignments should be printed
     */
    public static void printAssignments(List<UniversityStudent> students) {
        System.out.println("Roommate Assignment:");
        for (UniversityStudent s : students) {
            if (s.getRoommate() != null) {
                System.out.println(s.name + " is roommates with " + s.getRoommate().name);
            }
        }
    }
}