package MultiThreading.Lec_31.DeprecatedMethods;

public class StopSuspendResume {

    public static void main(String[] args) {

        System.out.println("main thread started...");

        SharedResource sharedResource = new SharedResource();

        Thread thread1 = new Thread(()-> {
            System.out.println(Thread.currentThread().getName() + " calling produce method");
            sharedResource.produce();
        });

        Thread thread2 = new Thread(() -> {
            // sleep this thread for 1 sec, so that thread1 can acquire the lock
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                // Exception handling
            }

            System.out.println(Thread.currentThread().getName() + " calling produce method");
            sharedResource.produce();
        });

        thread1.start();
        thread2.start();


        // Main thread suspends the thread1
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            // Exception handling
        }

        System.out.println("Thread-0 is suspended...");
        thread1.suspend(); // execution of the thread on hold, but doesn't release the monitor lock

        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            // Exception handling
        }

        System.out.println("Thread-0 is resumed...");
        thread1.resume(); // resume the execution of suspended thread

        System.out.println("main thread ends...");
    }
}
