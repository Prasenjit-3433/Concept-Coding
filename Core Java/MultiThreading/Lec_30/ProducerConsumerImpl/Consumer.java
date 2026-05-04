package MultiThreading.Lec_30.ProducerConsumerImpl;

public class Consumer implements Runnable {

    SharedQueue sharedQueue;

    public Consumer(SharedQueue sharedQueue) {
        this.sharedQueue = sharedQueue;
    }


    @Override
    public void run() {
        System.out.println("Consumer thread: " + Thread.currentThread().getName()  + " is ready!");

        while (true) {
            sharedQueue.consume();

            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                // exception handling
            }
        }
    }
}
