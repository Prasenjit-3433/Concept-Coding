package MultiThreading.Lec_30.MonitorLock.Theory;

public class MonitorLockExample {

    // synchronization on method level
    public synchronized void task1() {
        // do something
        try {
            System.out.println("Lock acquired by " + Thread.currentThread().getName());
            System.out.println("inside task1, processing...");

            // whichever thread comes here, sleeps for 10 sec
            Thread.sleep(10000);
            System.out.println("task1 completed!");
            System.out.println("Lock released by " + Thread.currentThread().getName());
        } catch (Exception e) {
            // exception handling
        }
    }

    public void task2() {
        System.out.println("task2, but before synchronized block");

        // synchronization on block level
        synchronized (this) {
            System.out.println("Lock acquired by " + Thread.currentThread().getName());
            System.out.println("task2, inside synchronized block");
            System.out.println("Lock released by " + Thread.currentThread().getName());
        }
    }

    public void task3() {
        System.out.println("task3 completed!");
    }
}
