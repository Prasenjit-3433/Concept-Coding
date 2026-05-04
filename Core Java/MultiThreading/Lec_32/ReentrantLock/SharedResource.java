package MultiThreading.Lec_32.ReentrantLock;

import java.util.concurrent.locks.ReentrantLock;

public class SharedResource {

    boolean isAvailable = false;

    public void produce(ReentrantLock lock) {
        try {
            lock.lock();
            System.out.println("Lock acquired by: " + Thread.currentThread().getName());

            isAvailable = true;
            Thread.sleep(4000); // doing some work
        } catch (Exception e) {
            // exception handling
        } finally {
            // Note: even if any exception/error happens, the lock must be released (always!)
            //       and that's why we put unlock() inside finally block

            System.out.println("Lock released by: " + Thread.currentThread().getName());
            lock.unlock(); // released the lock
        }
    }
}
