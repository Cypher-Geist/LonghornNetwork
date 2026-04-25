/**
 * A {@link Runnable} task that simulates one student sending a chat message
 * to another in the Longhorn Network.
 *
 * <p>When executed, the message is appended to <em>both</em> participants'
 * chat histories in a thread-safe manner by delegating to
 * {@link UniversityStudent#addMessage(String, String)}, which uses an internal
 * {@code synchronized(this)} block on each student object.</p>
 *
 * <h3>Thread safety</h3>
 * <p>Multiple {@code ChatThread} tasks may run concurrently.  Because each
 * {@code addMessage} call synchronises on its own receiver object, there is no
 * risk of data corruption even when two threads write to the same student's
 * history simultaneously.</p>
 */
public class ChatThread implements Runnable {

    /** The student who is sending the message. */
    private final UniversityStudent sender;

    /** The student who is receiving the message. */
    private final UniversityStudent receiver;

    /** The text content of the chat message. */
    private final String message;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code ChatThread} that will record a message from {@code sender}
     * to {@code receiver} when run.
     *
     * @param sender   the student sending the message
     * @param receiver the student receiving the message
     * @param message  the text of the message to send
     */
    public ChatThread(UniversityStudent sender, UniversityStudent receiver, String message) {
        this.sender   = sender;
        this.receiver = receiver;
        this.message  = message;
    }

    // -------------------------------------------------------------------------
    // Runnable
    // -------------------------------------------------------------------------

    /**
     * Executes the chat logic on whichever thread picks up this task.
     *
     * <p>The message is appended to both the sender's and the receiver's chat
     * histories, keyed by the other party's name.  A log line is printed to
     * standard output for tracing purposes.</p>
     */
    @Override
    public void run() {
        // Record in sender's history (they sent it) and receiver's history (they got it)
        sender.addMessage(receiver.name, "[" + sender.name + " → " + receiver.name + "]: " + message);
        receiver.addMessage(sender.name, "[" + sender.name + " → " + receiver.name + "]: " + message);
        System.out.println("[Chat] " + sender.name + " → " + receiver.name + ": " + message);
    }
}