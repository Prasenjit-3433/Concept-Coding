package MultiThreading.Lec_37;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulePoolExc {

    private static void testByRunnable() {
        ScheduledExecutorService poolObj = Executors.newScheduledThreadPool(5);

        // Runnable
        poolObj.schedule(()-> {
            System.out.println("logging out on 10 secs!");
        }, 10, TimeUnit.SECONDS);

        poolObj.schedule(()-> {
            System.out.println("logging out on 5 secs!");
        }, 5, TimeUnit.SECONDS);
    }

    private static void testByCallable() {
        ScheduledExecutorService poolObj = Executors.newScheduledThreadPool(5);

        // Callable
        Future<String> futureObj =  poolObj.schedule(()-> {
            return "return val of callable, after 10 sec";
        }, 10, TimeUnit.SECONDS);

        try {
            System.out.println(futureObj.get());
        } catch(Exception e) {
            // exception handling
        }
    }

    private static void testRepetition() {
        ScheduledExecutorService poolObj = Executors.newScheduledThreadPool(5);

        // repeat
        Future<?> futureObj = poolObj.scheduleWithFixedDelay(()-> {
            System.out.println("Thread picked the task");

            try {
                Thread.sleep(6000);
            } catch (Exception e) {
                // exception handling
            }

            System.out.println("Thread completed the task");
        }, 1, 3, TimeUnit.SECONDS);

        try {
            Thread.sleep(20000);
            futureObj.cancel(true);
        } catch (Exception e) {
            // exception handling
        }
    }

    public static void main(String[] args) {
        testRepetition();
    }
}
