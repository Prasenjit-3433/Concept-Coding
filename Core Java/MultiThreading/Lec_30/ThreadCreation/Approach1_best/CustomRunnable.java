package MultiThreading.Lec_30.ThreadCreation.Approach1_best;

public class CustomRunnable implements Runnable {

    @Override
    public void run() {
        System.out.println("code executed by thread: " + Thread.currentThread().getName());
    }
}
