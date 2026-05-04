package MultiThreading.Lec_30.ProducerConsumerImpl;

public class Main {
    public static void main(String[] args) {

        System.out.println("Main method starts...");

        // shared resource
        SharedQueue queue = new SharedQueue(20);

        // Creating new producer thread
        Thread producerThread1 = new Thread(new Producer(queue));
        Thread producerThread2 = new Thread(new Producer(queue));

        // creating multiple consumer thread
        Thread consumerThread1 = new Thread(new Consumer(queue));
        Thread consumerThread2 = new Thread(new Consumer(queue));
        Thread consumerThread3 = new Thread(new Consumer(queue));


        consumerThread1.start();

        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            // exception handling
        }

        producerThread1.start();
        producerThread2.start();

        System.out.println("Main method ends...");
    }
}
