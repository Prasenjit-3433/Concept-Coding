package MultiThreading.Lec_30.MonitorLock.PubSubLikeEx;

public class ConsumeTask implements Runnable {

    SharedResource sharedResource;

    public ConsumeTask(SharedResource sharedResource) {
        this.sharedResource = sharedResource;
    }


    @Override
    public void run() {
        System.out.println("Consumer thread: " + Thread.currentThread().getName());
        sharedResource.consumeItem();
    }
}
