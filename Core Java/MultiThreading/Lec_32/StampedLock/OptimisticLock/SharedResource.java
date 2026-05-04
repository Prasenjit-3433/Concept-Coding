package MultiThreading.Lec_32.StampedLock.OptimisticLock;

import java.util.concurrent.locks.StampedLock;

public class SharedResource {
    int a = 10;

    public void produce(StampedLock lock) {
        long stamp = lock.tryOptimisticRead();

        try {
            System.out.println("Taken optimistic read...");
            a = 11;
            Thread.sleep(6000);

            if (lock.validate(stamp)) {
                System.out.println("Updated value successfully or commit.");
            } else {
                System.out.println("rollback of work");
                a = 10;
            }
        } catch (Exception e) {
            // exception handling
        }
    }

    public void consume(StampedLock lock) {
        long stamp = lock.writeLock();
        System.out.println("Write lock accquired by: " + Thread.currentThread().getName());

        try {
            System.out.println("performing task...");
            a = 9;
        } finally {
            lock.unlock(stamp); // changes the status of the stamp
            System.out.println("Write lock released by: " + Thread.currentThread().getName());
        }
    }
}
