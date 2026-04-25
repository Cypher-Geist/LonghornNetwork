import java.io.IOException;

/**
 * Exception thrown when the {@code Age} field in the student input file
 * cannot be parsed as a valid integer (e.g., {@code Age: Twenty}).
 */
public class InvalidAgeException extends IOException {

    /**
     * Constructs an {@code InvalidAgeException} with the given detail message.
     *
     * @param message a human-readable description of the invalid age value
     */
    public InvalidAgeException(String message) {
        super(message);
    }
}
