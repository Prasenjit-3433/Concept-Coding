package MultiThreading.Lec_30.MonitorLock.PubSubLikeEx;

public class SharedResource {

    boolean isItemAvailable = false;

    // "synchronized" keyword put the monitor lock
    public synchronized void addItem() {
        isItemAvailable = true;
        System.out.println("Item added by: " + Thread.currentThread().getName() + " and invoking all threads which are waiting...");

        // whichever thread is waiting on the same instance of SharedResource, will go back into "RUNNABLE" state
        notifyAll();
    }

    public synchronized void consumeItem() {
        System.out.println("ConsumeItem method invoked by: " + Thread.currentThread().getName());

        // using while loop to avoid "spurious wake-up", sometimes because of system noise
        while (!isItemAvailable) {
            try {
                System.out.println("Thread " + Thread.currentThread().getName() + " is waiting now");

                // goes into "WAITING" state
                // when some other thread on the same instance of SharedResource,
                // calls notify() / notifyAll(), it goes back into "RUNNABLE" state again
                wait(); // it releases the monitor lock
            } catch (Exception e) {
                // handle exception
            }
        }

        System.out.println("Item consumed by: " + Thread.currentThread().getName());
        isItemAvailable = false;
    }
}
