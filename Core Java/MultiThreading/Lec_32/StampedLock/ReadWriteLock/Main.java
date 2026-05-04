package MultiThreading.Lec_32.StampedLock.ReadWriteLock;


import java.util.concurrent.locks.StampedLock;

public class Main {

    public static void main(String[] args) {

        StampedLock lock = new StampedLock();
        SharedResource resource = new SharedResource();

        Thread thread1 = new Thread(() -> {
            resource.read(lock);
        });

        Thread thread2 = new Thread(()-> {
            resource.read(lock);
        });

        Thread thread3 = new Thread(()-> {
            resource.write(lock);
        });

        thread1.start();
        thread2.start();
        thread3.start();
    }
}
