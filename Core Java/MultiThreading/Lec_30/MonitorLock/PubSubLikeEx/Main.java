package MultiThreading.Lec_30.MonitorLock.PubSubLikeEx;

public class Main {

    public static void main(String[] args) {

        System.out.println("Main method starts...");

        SharedResource sharedResource = new SharedResource();

        // Creating producer thread via passing runnable object
        Thread producerThread = new Thread(new ProduceTask(sharedResource));

        // Creating consumer thread via passing runnable object
        Thread consumerThread = new Thread(new ConsumeTask(sharedResource));

        // Note: here producer, consumer thread both work on the same object - sharedResource

        // threads are in "RUNNABLE" state
        producerThread.start();
        consumerThread.start();

        System.out.println("Main method ends!...");
    }
}
