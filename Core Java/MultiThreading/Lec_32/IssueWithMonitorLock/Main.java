package MultiThreading.Lec_32.IssueWithMonitorLock;

public class Main {

    public static void main(String[] args) {

        SharedResource resource1 = new SharedResource();
        Thread thread1 = new Thread(() -> {
            resource1.produce();
        });

        SharedResource resource2 = new SharedResource();
        Thread thread2 = new Thread(() -> {
            resource2.produce();
        });

        thread1.start();
        thread2.start();

        // Recall: Monitor lock works on object / instance level.
        //
        // Issue: Here 2 threads working on 2 diff instance of SharedResource, and hence both can acquire lock at the same time.
    }
}
