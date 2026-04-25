import java.io.*;
import java.util.*;

/**
 * Utility class responsible for reading and parsing student data files into
 * {@link UniversityStudent} objects.
 *
 * <h3>Input file format</h3>
 * <p>Each student block begins with the literal line {@code Student:} and is
 * followed by key-value pairs, one per line, separated by {@code ': '}.
 * Student blocks are separated by blank lines (a trailing blank line is not
 * required for the final block).</p>
 *
 * <pre>
 * Student:
 * Name: Alice
 * Age: 20
 * Gender: Female
 * Year: 2
 * Major: Computer Science
 * GPA: 3.8
 * RoommatePreferences: Bob, Charlie
 * PreviousInternships: Google, Amazon
 * </pre>
 *
 * <h3>Validation &amp; exceptions</h3>
 * <ul>
 *   <li>{@link IncorrectFormatException} — a line is missing the {@code ':'}
 *       separator (e.g., {@code Name Simon}).</li>
 *   <li>{@link InvalidAgeException} — the {@code Age} value cannot be parsed
 *       as an integer (e.g., {@code Age: Twenty}).</li>
 *   <li>{@link InvalidGPAException} — the {@code GPA} value cannot be parsed
 *       as a double (e.g., {@code GPA: ThreePointFive}).</li>
 *   <li>{@link MissingFieldException} — a required field is absent from a
 *       student block (e.g., no {@code RoommatePreferences} line).</li>
 * </ul>
 *
 * <h3>Note on {@code "None"} internships</h3>
 * <p>The literal value {@code "None"} in the {@code PreviousInternships} field
 * is treated as "no internships" and results in an empty list.</p>
 */
public class DataParser {

    /**
     * Required field keys that every student block must contain.
     * Validation is performed in {@link #createStudent(Map)}.
     */
    private static final String[] REQUIRED_FIELDS = {
            "Name", "Age", "Gender", "Year", "Major", "GPA",
            "RoommatePreferences", "PreviousInternships"
    };

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parses a student data file and returns a list of {@link UniversityStudent} objects.
     *
     * <p>Each student block in the file produces exactly one {@code UniversityStudent}.
     * The method reads the file line-by-line, accumulating key-value pairs for each
     * block, then delegates construction to {@link #createStudent(Map)}.</p>
     *
     * @param filename path to the input file (relative or absolute)
     * @return a list of parsed {@link UniversityStudent} objects in file order
     * @throws IncorrectFormatException if a content line is missing the {@code ':'} separator
     * @throws InvalidAgeException      if an {@code Age} value cannot be parsed as an integer
     * @throws InvalidGPAException      if a {@code GPA} value cannot be parsed as a double
     * @throws MissingFieldException    if a required field is absent from any student block
     * @throws IOException              for underlying file I/O errors
     */
    public static List<UniversityStudent> parseStudents(String filename) throws IOException {
        List<UniversityStudent> students = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            // fields == null means we have not encountered a "Student:" header yet
            Map<String, String> fields = null;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // ----- Start of a new student block -----
                if (trimmed.equals("Student:")) {
                    if (fields != null && !fields.isEmpty()) {
                        students.add(createStudent(fields));
                    }
                    fields = new LinkedHashMap<>();
                    continue;
                }

                // ----- Before the first "Student:" header -----
                if (fields == null) continue;

                // ----- Blank line: end of current block -----
                if (trimmed.isEmpty()) {
                    if (!fields.isEmpty()) {
                        students.add(createStudent(fields));
                        fields = new LinkedHashMap<>();
                    }
                    continue;
                }

                // ----- Parse "Key: Value" pair -----
                int colonIdx = trimmed.indexOf(':');
                if (colonIdx == -1) {
                    throw new IncorrectFormatException(
                            "Parsing error: Incorrect format in line: '" + trimmed +
                                    "'. Expected format 'Name: <value>'.");
                }

                String key   = trimmed.substring(0, colonIdx).trim();
                String value = trimmed.substring(colonIdx + 1).trim();
                fields.put(key, value);
            }

            // ----- Finalise the last block (no trailing blank line needed) -----
            if (fields != null && !fields.isEmpty()) {
                students.add(createStudent(fields));
            }
        }

        return students;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Constructs a {@link UniversityStudent} from a map of raw field strings.
     *
     * <p>The Name field is resolved first so it can appear in error messages for
     * subsequent validation failures.  Required-field presence is checked before
     * any type-specific parsing.</p>
     *
     * @param fields a map of field names to their raw string values
     * @return a fully initialised {@link UniversityStudent}
     * @throws InvalidAgeException   if the {@code Age} value is not a valid integer
     * @throws InvalidGPAException   if the {@code GPA} value is not a valid double
     * @throws MissingFieldException if any required field is absent from {@code fields}
     * @throws IOException           for any other I/O-related parsing issue
     */
    private static UniversityStudent createStudent(Map<String, String> fields)
            throws IOException {

        // Resolve the name early so it can appear in all downstream error messages.
        String name = fields.getOrDefault("Name", "Unknown");

        // --- Validate all required fields are present ---
        for (String req : REQUIRED_FIELDS) {
            if (!fields.containsKey(req)) {
                throw new MissingFieldException(
                        "Parsing error: Missing required field '" + req +
                                "' in student entry for " + name + ".");
            }
        }

        // --- Age ---
        int age;
        try {
            age = Integer.parseInt(fields.get("Age"));
        } catch (NumberFormatException e) {
            throw new InvalidAgeException(
                    "Number format error: Invalid number format for age: '" +
                            fields.get("Age") + "' in student entry for " + name + ".");
        }

        // --- Gender ---
        String gender = fields.get("Gender");

        // --- Year ---
        int year;
        try {
            year = Integer.parseInt(fields.get("Year"));
        } catch (NumberFormatException e) {
            throw new IncorrectFormatException(
                    "Parsing error: Incorrect format in line: 'Year " +
                            fields.get("Year") + "'. Expected format 'Name: <value>'.");
        }

        // --- Major ---
        String major = fields.get("Major");

        // --- GPA ---
        double gpa;
        try {
            gpa = Double.parseDouble(fields.get("GPA"));
        } catch (NumberFormatException e) {
            throw new InvalidGPAException(
                    "Number format error: Invalid number format for GPA: '" +
                            fields.get("GPA") + "' in student entry for " + name + ".");
        }

        // --- RoommatePreferences (comma-separated, may be empty) ---
        List<String> roommatePrefs = parseCommaSeparatedList(fields.get("RoommatePreferences"));

        // --- PreviousInternships ---
        // The literal "None" signals no internships; filter it out so the list is
        // genuinely empty rather than containing the placeholder string.
        List<String> internships = parseCommaSeparatedList(fields.get("PreviousInternships"));
        internships.removeIf(s -> s.equals("None"));

        return new UniversityStudent(name, age, gender, year, major, gpa,
                roommatePrefs, internships);
    }

    /**
     * Splits a comma-separated string into a trimmed list of tokens.
     * An empty or {@code null} input returns an empty list.
     *
     * @param value the raw comma-separated value string (may be {@code null} or blank)
     * @return a mutable list of trimmed, non-empty tokens; may be empty but never {@code null}
     */
    private static List<String> parseCommaSeparatedList(String value) {
        List<String> list = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) return list;
        for (String token : value.split(",")) {
            String t = token.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }
}