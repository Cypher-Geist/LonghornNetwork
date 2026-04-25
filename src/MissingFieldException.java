import java.io.IOException;

/**
 * Exception thrown when a required field (e.g., {@code RoommatePreferences})
 * is completely absent from a student block in the input file.
 */
public class MissingFieldException extends IOException {

    /**
     * Constructs a {@code MissingFieldException} with the given detail message.
     *
     * @param message a human-readable description of the missing field
     */
    public MissingFieldException(String message) {
        super(message);
    }
}
