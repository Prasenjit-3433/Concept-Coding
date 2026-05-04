package MultiThreading.Lec_30.ProducerConsumerImpl;

public class Producer implements Runnable {

    SharedQueue sharedQueue;

    public Producer(SharedQueue sharedQueue) {
        this.sharedQueue = sharedQueue;
    }


    @Override
    public void run() {
        System.out.println("Producer thread: " + Thread.currentThread().getName() + " is ready!");

        for (int i = 0; i < 5; i++) {
            sharedQueue.publish("Message-" + (int) (Math.random() * 1000));

            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                // exception handling
            }
        }

    }
}
