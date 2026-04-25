import java.io.IOException;

/**
 * Exception thrown when a line in the student input file does not follow
 * the expected "Key: Value" format (e.g., missing the ':' separator).
 *
 * <p>Example trigger: a line like {@code Name Simon} instead of {@code Name: Simon}.</p>
 */
public class IncorrectFormatException extends IOException {

    /**
     * Constructs an {@code IncorrectFormatException} with the given detail message.
     *
     * @param message a human-readable description of the formatting error
     */
    public IncorrectFormatException(String message) {
        super(message);
    }
}
