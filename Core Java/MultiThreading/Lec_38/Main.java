package MultiThreading.Lec_38;

public class Main {

    public static void main(String[] args) {

        ThreadLocal<String> threadLocalObj = new ThreadLocal<>();

        // main thread
        threadLocalObj.set(Thread.currentThread().getName());

        Thread thread1 = new Thread(()-> {
            threadLocalObj.set(Thread.currentThread().getName());
            System.out.println("task1");
        });

        thread1.start();

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // exception handling
        }

        System.out.println("Main thread: " + threadLocalObj.get());
    }
}
