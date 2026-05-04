package MultiThreading.Lec_32.StampedLock.ReadWriteLock;

import java.util.concurrent.locks.StampedLock;

public class SharedResource {

    boolean isAvailable = true;

    public void read(StampedLock lock) {
        long stamp = lock.readLock();

        try {
            System.out.println("Read lock acquired by: " + Thread.currentThread().getName());
            System.out.println("Reading isAvailable...: " + isAvailable);
            Thread.sleep(6000);
        } catch (Exception e) {
            // exception handling
        } finally {
            lock.unlock(stamp);
            System.out.println("Read lock released by: " + Thread.currentThread().getName());
        }
    }

    public void write(StampedLock lock) {
        long stamp = lock.writeLock();

        try {
            System.out.println("Write lick acquired by: " + Thread.currentThread().getName());
            isAvailable = false;
        } catch (Exception e) {
            // exception handling
        } finally {
            lock.unlock(stamp);
            System.out.println("Write lock released by: " + Thread.currentThread().getName());
        }
    }
}
