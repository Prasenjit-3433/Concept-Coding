package MultiThreading.Lec_32.ReentrantLock;

import java.util.concurrent.locks.ReentrantLock;

public class Main {

    public static void main(String[] args) {

        ReentrantLock lock = new ReentrantLock();

        // Instead of relying on object, they rely on the lock object

        SharedResource resource1 = new SharedResource();
        Thread thread1 = new Thread(() -> {
            // pack the lock instance
            resource1.produce(lock);
        });

        SharedResource resource2 = new SharedResource();
        Thread thread2 = new Thread(() -> {
            // pack the lock instance
            resource2.produce(lock);
        });

        thread1.start();
        thread2.start();
    }
}
