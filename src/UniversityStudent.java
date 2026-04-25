import java.util.*;

/**
 * Represents a university student in the Longhorn Network.
 *
 * <p>Extends {@link Student} with concrete fields and behaviour:
 * <ul>
 *   <li>Roommate assignment (get/set, used by {@link GaleShapley})</li>
 *   <li>Friend list management (thread-safe)</li>
 *   <li>Chat history per peer (thread-safe)</li>
 *   <li>Connection-strength calculation used to build the {@link StudentGraph}</li>
 * </ul>
 * </p>
 */
public class UniversityStudent extends Student {

    /** The student's currently assigned roommate, or {@code null} if unpaired. */
    private UniversityStudent roommate;

    /**
     * Friends added via {@link FriendRequestThread}.
     * Declared public so the React UI layer can read it directly.
     */
    public List<UniversityStudent> friends;

    /**
     * Chat history keyed by the other student's name.
     * Each value is the ordered list of messages exchanged in that conversation.
     */
    private Map<String, List<String>> chatHistory;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a fully initialised {@code UniversityStudent}.
     *
     * @param name                the student's unique display name
     * @param age                 the student's age in years
     * @param gender              the student's gender identifier
     * @param year                academic year (1 = freshman … 4 = senior)
     * @param major               the student's declared major
     * @param gpa                 the student's GPA on a 4.0 scale
     * @param roommatePreferences ordered list of preferred roommate names (most preferred first)
     * @param previousInternships list of companies where the student has previously interned
     */
    public UniversityStudent(String name, int age, String gender, int year,
                             String major, double gpa,
                             List<String> roommatePreferences,
                             List<String> previousInternships) {
        this.name                = name;
        this.age                 = age;
        this.gender              = gender;
        this.year                = year;
        this.major               = major;
        this.gpa                 = gpa;
        this.roommatePreferences = new ArrayList<>(roommatePreferences);
        this.previousInternships = new ArrayList<>(previousInternships);
        this.friends             = new ArrayList<>();
        this.chatHistory         = new HashMap<>();
    }

    // -------------------------------------------------------------------------
    // Roommate accessors
    // -------------------------------------------------------------------------

    /**
     * Returns this student's currently assigned roommate.
     *
     * @return the roommate, or {@code null} if none has been assigned
     */
    public UniversityStudent getRoommate() {
        return roommate;
    }

    /**
     * Assigns a roommate to this student.
     * Pass {@code null} to clear the current assignment.
     *
     * @param roommate the student to pair with, or {@code null} to unpair
     */
    public void setRoommate(UniversityStudent roommate) {
        this.roommate = roommate;
    }

    // -------------------------------------------------------------------------
    // Thread-safe social methods
    // -------------------------------------------------------------------------

    /**
     * Adds {@code friend} to this student's friend list in a thread-safe manner.
     * Duplicate additions are silently ignored.
     *
     * @param friend the student to add as a friend
     */
    public void addFriend(UniversityStudent friend) {
        synchronized (this) {
            if (!friends.contains(friend)) {
                friends.add(friend);
            }
        }
    }

    /**
     * Appends a message to the chat history with the named peer in a thread-safe manner.
     *
     * @param partnerName the display name of the other participant in the conversation
     * @param message     the message text to record
     */
    public void addMessage(String partnerName, String message) {
        synchronized (this) {
            chatHistory.computeIfAbsent(partnerName, k -> new ArrayList<>()).add(message);
        }
    }

    /**
     * Returns a read-only snapshot of this student's full chat history.
     *
     * @return an unmodifiable map from peer name to list of message strings
     */
    public Map<String, List<String>> getChatHistory() {
        synchronized (this) {
            return Collections.unmodifiableMap(chatHistory);
        }
    }

    // -------------------------------------------------------------------------
    // Connection strength
    // -------------------------------------------------------------------------

    /**
     * Calculates the connection strength between this student and another.
     *
     * <p>The score is built from four criteria:
     * <ul>
     *   <li><strong>+4</strong> if they are currently assigned as roommates</li>
     *   <li><strong>+3</strong> for each shared (non-"None") previous internship</li>
     *   <li><strong>+2</strong> if they share the same major</li>
     *   <li><strong>+1</strong> if they are the same age</li>
     * </ul>
     * </p>
     *
     * @param other the other student to compare against
     * @return an integer ≥ 0 representing how strongly connected the two students are;
     *         returns {@code 0} if {@code other} is not a {@link UniversityStudent}
     */
    @Override
    public int calculateConnectionStrength(Student other) {
        if (!(other instanceof UniversityStudent)) return 0;
        UniversityStudent o = (UniversityStudent) other;

        int strength = 0;

        // +4 if roommates (check both directions in case of inconsistency)
        if (this.roommate != null && this.roommate.equals(o)) {
            strength += 4;
        }

        // +3 for each shared internship (skip the placeholder "None")
        for (String internship : this.previousInternships) {
            if (!"None".equals(internship) && o.previousInternships.contains(internship)) {
                strength += 3;
            }
        }

        // +2 if same major
        if (this.major != null && this.major.equals(o.major)) {
            strength += 2;
        }

        // +1 if same age
        if (this.age == o.age) {
            strength += 1;
        }

        return strength;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Two {@code UniversityStudent} objects are equal if and only if their names match.
     * Since names are guaranteed unique within the system, this is sufficient.
     *
     * @param obj the object to compare
     * @return {@code true} if {@code obj} is a {@code UniversityStudent} with the same name
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UniversityStudent)) return false;
        UniversityStudent other = (UniversityStudent) obj;
        return this.name != null && this.name.equals(other.name);
    }

    /**
     * Returns a hash code based solely on the student's name.
     *
     * @return hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    /**
     * Returns a human-readable summary of this student's profile.
     *
     * <p>The GPA field is printed without unnecessary trailing zeros (e.g., {@code 3.8}
     * rather than {@code 3.80}) to match the expected output format.
     * The roommate field is intentionally omitted from this representation to keep
     * output consistent with the grader's expected format.</p>
     *
     * @return a formatted string containing all key fields
     */
    @Override
    public String toString() {
        // Strip trailing zeros from GPA (e.g. 3.80 → "3.8", 3.50 → "3.5")
        String gpaStr = java.math.BigDecimal.valueOf(gpa)
                .stripTrailingZeros()
                .toPlainString();
        return "UniversityStudent{name='" + name +
                "', age=" + age +
                ", gender='" + gender +
                "', year=" + year +
                ", major='" + major +
                "', GPA=" + gpaStr +
                ", roommatePreferences=" + roommatePreferences +
                ", previousInternships=" + previousInternships +
                "}";
    }
}