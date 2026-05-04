package MultiThreading.Lec_30.MonitorLock.PubSubLikeEx;

public class ProduceTask implements Runnable {

    SharedResource sharedResource;

    public ProduceTask(SharedResource sharedResource) {
        this.sharedResource = sharedResource;
    }


    @Override
    public void run() {
        System.out.println("Producer thread: " + Thread.currentThread().getName());

        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            // handle exception
        }

        sharedResource.addItem();
    }
}
