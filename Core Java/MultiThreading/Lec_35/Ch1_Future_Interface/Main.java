package MultiThreading.Lec_35.Ch1_Future_Interface;

import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                1,
                1,
                1,
                TimeUnit.HOURS,
                new ArrayBlockingQueue<>(10),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        // new thread will be created, and it will perform the task
        Future<?> futureObj = poolExecutor.submit(()-> {
            System.out.println("Task being processed...");

            try {
                Thread.sleep(7000);
            } catch (Exception e) {
                // exception handling
            }

            System.out.println("Task finished processing!!!");
        });

        // caller is checking the status of the thread it created
        System.out.println("is Done: " + futureObj.isDone());

       try {
           // Caller thread (i.e. main thread) waits for 2 sec & throws "TimeoutException" if task is not finish within that time
           futureObj.get(2, TimeUnit.SECONDS);
       } catch (TimeoutException e) {
           System.out.println("TimeoutException happened");
       } catch (Exception e) {
           // exception handling
       }

       try {
           // Caller thread (i.e. main thread) waits unless the task finishes
           futureObj.get();
       } catch (Exception e) {
           // exception handling
       }

        // true for completion (inc. normal termination, exception, cancellation)
        System.out.println("is Done: " + futureObj.isDone());

       // check: whether task completed normally or cancelled
        System.out.println("is Cancelled: " + futureObj.isCancelled());

        poolExecutor.shutdown();
    }
}
