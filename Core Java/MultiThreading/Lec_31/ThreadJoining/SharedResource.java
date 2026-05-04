package MultiThreading.Lec_31.ThreadJoining;

public class SharedResource {

    boolean isAvailable = false;

    public synchronized void produce() {

        System.out.println("Lock acquired by " + Thread.currentThread().getName());
        isAvailable = true;

        try {
            // "Timed Waiting" state
            Thread.sleep(5000); // doesn't release the monitor lock
        } catch (Exception e) {
            // exception handling
        }

        System.out.println("Lock released by " + Thread.currentThread().getName());
    }
}
