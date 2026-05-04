package MultiThreading.Lec_32.ReadWriteLock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {

    public static void main(String[] args) {

        SharedResource resource1 = new SharedResource();
        ReadWriteLock lock = new ReentrantReadWriteLock();

        Thread thread1 = new Thread(()-> {
            // thread1 acquiring read lock on "lock" obj, NOT on "SharedResource" obj
            resource1.read(lock);
        });

        Thread thread2 = new Thread(()-> {
            // thread2 acquiring read lock on "lock" obj, NOT on "SharedResource" obj
            resource1.read(lock);
        });

        SharedResource resource2 = new SharedResource();
        Thread thread3 = new Thread(()-> {
            // thread3 acquiring read lock on "lock" obj, NOT on "SharedResource" obj
            resource2.write(lock);
        });

        thread1.start();
        thread2.start();
        thread3.start();
    }
}
