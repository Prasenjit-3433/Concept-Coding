package MultiThreading.Lec_32.ReadWriteLock;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedResource {

    boolean isAvailable = false;

    public void read(ReadWriteLock lock) {
        try {
            lock.readLock().lock();
            System.out.println("Read lock acquired by: " + Thread.currentThread().getName());
            Thread.sleep(8000);
            System.out.println("reading isAvailable....: " + isAvailable);
        } catch (Exception e) {
            // exception handling
        } finally {
            System.out.println("Read lock released by: " + Thread.currentThread().getName());
            lock.readLock().unlock();
        }
    }

    public void write(ReadWriteLock lock) {
        try {
            lock.writeLock().lock();
            System.out.println("Write lock acquired by: " + Thread.currentThread().getName());
            isAvailable = true;
            System.out.println("writing isAvailable....: " + isAvailable);
        } catch (Exception e) {
            // exception handling
        } finally {
            System.out.println("Write lock released by: " + Thread.currentThread().getName());
            lock.writeLock().unlock();
        }
    }
}
