package MultiThreading.Lec_30.ThreadCreation.Approach1_best;

public class Main {

    public static void main(String[] args) {
        System.out.println("Going inside main method: Thread-" + Thread.currentThread().getName());

        CustomRunnable runnableObj = new CustomRunnable();

        // New Thread is created
        Thread thread = new Thread(runnableObj);

        // start() invokes run() method of the Thread class, which inturn calls run() method of the runnable passed
        thread.start();

        System.out.println("Finish main method: Thread-" + Thread.currentThread().getName());
    }
}
