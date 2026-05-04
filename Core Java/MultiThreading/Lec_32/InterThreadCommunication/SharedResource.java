package MultiThreading.Lec_32.InterThreadCommunication;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SharedResource {
    // single slot (not a whole buffer like producer-consumer)
    boolean isAvailable = false;

    public void produce(ReentrantLock lock, Condition condition) {
        try {
            lock.lock();
            System.out.println("Produce lock acquired by: " + Thread.currentThread().getName());

            // already available, thread has to wait for the item to be consumed before producing new item
            while (isAvailable) {
                System.out.println("Producer thread is waiting: " + Thread.currentThread().getName());
                // starts waiting on "CONDITION" object, not on "LOCK" obj (because a lock can have multiple condition obj)
                condition.await(); // releases the custom lock
            }

            // produce item
            isAvailable = true;
            System.out.println("Produced by " + Thread.currentThread().getName());

            // Signal consumer that item is available (wakes up those threads which are waiting on the SAME condition obj)
            condition.signal(); // Wake up waiting thread (like notify())

        } catch (Exception e) {
            // exception handling
        } finally {
            lock.unlock();
            System.out.println("Produce lock released by: " + Thread.currentThread().getName());
        }
    }

    public void consume(ReentrantLock lock, Condition condition) {
        try {
            Thread.sleep(1000);
            lock.lock();
            System.out.println("Consumer lock acquired by: " + Thread.currentThread().getName());

            // If item not available, wait for producer
            if (!isAvailable) {
                System.out.println("Consumer thread is waiting: " + Thread.currentThread().getName());
                // starts waiting on "CONDITION" object, not on "LOCK" obj (because a lock can have multiple condition obj)
                condition.await(); // releases the custom lock
            }

            // Consume item
            isAvailable = false;
            System.out.println("Consumed by " + Thread.currentThread().getName());

            // Signal producer that space is available (wakes up those threads which are waiting on the SAME condition obj)
            condition.signal();  // Wake up waiting thread (like notify())

        } catch (Exception e) {
            // exception handling
        } finally {
            lock.unlock();
            System.out.println("Consume lock released by: " + Thread.currentThread().getName());
        }
    }
}
