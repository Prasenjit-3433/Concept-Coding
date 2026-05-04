package MultiThreading.Lec_31.ThreadJoining;

import MultiThreading.Lec_31.DeprecatedMethods.SharedResource;

public class Main {
    public static void main(String[] args) {

        System.out.println("main thread started...");

        SharedResource sharedResource = new SharedResource();

        // new thread created
        Thread thread1 = new Thread(()-> {
            System.out.println(Thread.currentThread().getName() + " calling produce method");
            sharedResource.produce();
        });

        // starts the above created thread
        thread1.start();

        // when join() invoked on a thread object (i.e. thread1), current thread (i.e. main thread) will be
        // blocked and waits for the specific thread (thread 1) to finish
        try {
            System.out.println("main thread is waiting for " + thread1.getName() + " to finish");
            thread1.join();
        } catch (Exception e) {
            // exception handling
        }

        System.out.println("main thread ends...");
    }
}
