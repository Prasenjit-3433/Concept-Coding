package MultiThreading.Lec_32.InterThreadCommunication;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static void main(String[] args) {
        SharedResource resource = new SharedResource();

        // Create a lock obj
        ReentrantLock lock = new ReentrantLock();

        // Create a CONDITION obj on the same lock obj
        Condition condition = lock.newCondition();

        // Producer thread
        Thread producer = new Thread(()-> {
            for (int i = 0; i < 2; i++) {
                resource.produce(lock, condition);
            }
        }, "Producer");

        // Consumer thread
        Thread consumer = new Thread(()-> {
            for (int i = 0; i < 2; i++) {
                resource.consume(lock, condition);
            }
        }, "Consumer");

        producer.start();
        consumer.start();
    }
}
