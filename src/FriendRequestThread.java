/**
 * A {@link Runnable} task that simulates one student sending a friend request
 * to another in the Longhorn Network.
 *
 * <p>When executed by a thread (e.g., via an {@link java.util.concurrent.ExecutorService}),
 * it adds each student to the other's friend list in a thread-safe manner by
 * delegating to {@link UniversityStudent#addFriend(UniversityStudent)}, which
 * uses an internal {@code synchronized(this)} block.</p>
 *
 * <h3>Thread safety</h3>
 * <p>Multiple {@code FriendRequestThread} tasks can run concurrently without
 * corrupting either student's friend list, because each {@code addFriend} call
 * synchronises on its own receiver object independently.</p>
 */
public class FriendRequestThread implements Runnable {

    /** The student who is sending the friend request. */
    private final UniversityStudent sender;

    /** The student who is receiving the friend request. */
    private final UniversityStudent receiver;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code FriendRequestThread} that will add {@code sender} and
     * {@code receiver} to each other's friend lists when run.
     *
     * @param sender   the student initiating the request
     * @param receiver the student being added as a friend
     */
    public FriendRequestThread(UniversityStudent sender, UniversityStudent receiver) {
        this.sender   = sender;
        this.receiver = receiver;
    }

    // -------------------------------------------------------------------------
    // Runnable
    // -------------------------------------------------------------------------

    /**
     * Executes the friend-request logic on whichever thread picks up this task.
     *
     * <p>Both students are added to each other's friend lists, and a confirmation
     * message is printed to standard output for tracing purposes.</p>
     */
    @Override
    public void run() {
        sender.addFriend(receiver);
        receiver.addFriend(sender);
        System.out.println("[FriendRequest] " + sender.name
                + " sent a friend request to " + receiver.name + ".");
    }
}