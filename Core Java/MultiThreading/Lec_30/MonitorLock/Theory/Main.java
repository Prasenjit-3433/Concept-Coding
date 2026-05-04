package MultiThreading.Lec_30.MonitorLock.Theory;

public class Main {

    public static void main(String[] args) {

        MonitorLockExample obj = new MonitorLockExample();

        // using Runnable
        MonitorTask1Runnable runnableObj = new MonitorTask1Runnable(obj);
        Thread t1 = new Thread(runnableObj);

        // using lamba (since runnable is a functional interface)
        Thread t2 = new Thread(() -> {obj.task2();});
        Thread t3 = new Thread(() -> {obj.task3();});

        t1.start();
        t2.start();
        t3.start();
    }
}
