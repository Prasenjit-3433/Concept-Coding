package MultiThreading.Lec_32.SemaphoreLock;

import java.util.concurrent.Semaphore;

public class SharedResource {

    boolean isAvailable = false;

    public void producer(Semaphore lock) {

        try {
            lock.acquire();
            System.out.println("Lock acquired by: " + Thread.currentThread().getName());
            isAvailable = true;
            Thread.sleep(4000);
        } catch (Exception e) {
            // exception handling
        } finally {
            System.out.println("Lock released by: " + Thread.currentThread().getName());
            lock.release();
        }
    }
}
