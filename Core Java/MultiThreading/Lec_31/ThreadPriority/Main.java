package MultiThreading.Lec_31.ThreadPriority;

public class Main {

    public static void main(String[] args) {

        System.out.println("main thread starts...");

        Thread thread0 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " is printing..");
        });

        Thread thread1 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " is printing..");
        });

        Thread thread2 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " is printing..");
        });

        Thread thread3 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " is printing..");
        });


        thread0.setPriority(5);
        thread1.setPriority(4);
        thread2.setPriority(10);
        thread3.setPriority(1);

        // ideal output: thread2 -> thread0 -> thread1 -> thread3
        thread0.start();
        thread1.start();
        thread2.start();
        thread3.start();

        // Reality: It's not guaranteed any specific order, it's just a hint to "Thread Scheduler"
        //          which to execute next (not strict rule)

        System.out.println("main thread ends...");
    }
}
