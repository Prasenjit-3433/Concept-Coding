package MultiThreading.Lec_31.DaemonThread;

public class Main {

    public static void main(String[] args) {

        System.out.println("main thread started...");

        Thread thread1 = new Thread(() -> {
            System.out.println("user thread: " + Thread.currentThread().getName() + " started...");

            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                // exception handling
            }

            System.out.println("user thread: " + Thread.currentThread().getName() + " ended...");
        });

        Thread thread2 = new Thread(() -> {
            System.out.println("Daemon thread: " + Thread.currentThread().getName() + " started...");

            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                // exception handling
            }

            System.out.println("Daemon thread: " + Thread.currentThread().getName() + " ended...");
        });

        thread2.setDaemon(true);


        // user thread: 10 sec, daemon thread: 4 sec
        thread2.start();
        thread1.start();

        // Note: in order to stay alive, daemon thread needs any other user thread to be alive.

        System.out.println("main thread ended...");
    }
}
