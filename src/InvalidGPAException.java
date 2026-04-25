import java.io.IOException;

/**
 * Exception thrown when the {@code GPA} field in the student input file
 * cannot be parsed as a valid decimal number (e.g., {@code GPA: ThreePointFive}).
 */
public class InvalidGPAException extends IOException {

    /**
     * Constructs an {@code InvalidGPAException} with the given detail message.
     *
     * @param message a human-readable description of the invalid GPA value
     */
    public InvalidGPAException(String message) {
        super(message);
    }
}
