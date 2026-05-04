package MultiThreading.Lec_37;

import java.util.concurrent.*;

public class Main {

    private static void testShutdown() {
        ExecutorService poolObj = Executors.newFixedThreadPool(1);

        poolObj.submit(()-> {
            System.out.println("Task1 being processed....");

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                // exception handling
            }

            System.out.println("Task1 finished processing!");
        });

        // Stops accepting new tasks, but existing tasks continues to execute
        poolObj.shutdown();

        System.out.println("Main thread is complete");
    }

    private static void testAwaitTermination() {
        ExecutorService poolObj = Executors.newFixedThreadPool(1);

        poolObj.submit(()-> {
            System.out.println("Task1 being processed....");

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                // exception handling
            }

            System.out.println("Task1 finished processing!");
        });

        poolObj.shutdown();

        try {
            // 'main thread' waits here for 2 seconds to pool get shutdown (inc. existing tasks)
            // within 2 secs, if the pool gets shutdown, then it returns true, otherwise false.
            // e.g.: Since the task here takes 5 secs itself to finish executing, the pool can't get shutdown within 2 secs.
            boolean isTerminated = poolObj.awaitTermination(2, TimeUnit.SECONDS);
            System.out.println("Is terminated: " + isTerminated);
        } catch (Exception e) {
            // exception handling
        }

        System.out.println("Main thread is complete");
    }

    private static void testShutdownNow() {
        ExecutorService poolObj = Executors.newFixedThreadPool(1);

        // halts this processing
        poolObj.submit(()-> {
            System.out.println("Task1 being processed....");

            try {
                Thread.sleep(15000);
            } catch (Exception e) {
                // exception handling
            }

            System.out.println("Task1 finished processing!");
        });

        poolObj.shutdownNow();


        System.out.println("Main thread is complete");
    }



    public static void main(String[] args) {



    }

}
