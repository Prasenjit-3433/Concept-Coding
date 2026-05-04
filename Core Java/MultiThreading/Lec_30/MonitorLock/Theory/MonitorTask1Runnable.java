package MultiThreading.Lec_30.MonitorLock.Theory;

public class MonitorTask1Runnable implements Runnable {
    MonitorLockExample obj;

    public MonitorTask1Runnable(MonitorLockExample obj) {
        this.obj = obj;
    }

    @Override
    public void run() {
        obj.task1();
    }
}
