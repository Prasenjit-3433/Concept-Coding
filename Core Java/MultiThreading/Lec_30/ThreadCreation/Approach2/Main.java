package MultiThreading.Lec_30.ThreadCreation.Approach2;

public class Main {
    public static void main(String[] args) {
        System.out.println("Going inside main method: Thread-" + Thread.currentThread().getName());

        // New Thread is created
        CustomThread myThread = new CustomThread();

        // start() invokes the overloaded run() method of the child class myThread
        myThread.start();

        System.out.println("Finish main method: Thread-" + Thread.currentThread().getName());
    }
}
