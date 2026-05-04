package MultiThreading.Lec_32.SemaphoreLock;

import java.util.concurrent.Semaphore;

public class Main {

    public static void main(String[] args) {
        SharedResource resource = new SharedResource();

        // permits = how many threads can acquire lock
        Semaphore lock = new Semaphore(2);

        Thread thread1 = new Thread(() -> {
            resource.producer(lock);
        });

        Thread thread2 = new Thread(() -> {
            resource.producer(lock);
        });

        Thread thread3 = new Thread(() -> {
            resource.producer(lock);
        });

        Thread thread4 = new Thread(() -> {
            resource.producer(lock);
        });

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
    }
}
