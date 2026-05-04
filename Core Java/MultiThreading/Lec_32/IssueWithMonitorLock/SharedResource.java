package MultiThreading.Lec_32.IssueWithMonitorLock;

public class SharedResource {

    boolean isAvailable = false;

    public synchronized void produce() {
        try {
            System.out.println("Lock acquired by: " + Thread.currentThread().getName());
            isAvailable = true;
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Lock released by: " + Thread.currentThread().getName());
    }
}
