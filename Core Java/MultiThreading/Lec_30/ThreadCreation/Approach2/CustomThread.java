package MultiThreading.Lec_30.ThreadCreation.Approach2;

public class CustomThread extends Thread {
    @Override
    public void run() {
        System.out.println("code executed by thread: " + Thread.currentThread().getName());
    }
}
