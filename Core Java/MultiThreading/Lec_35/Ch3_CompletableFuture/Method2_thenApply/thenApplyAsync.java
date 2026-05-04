package MultiThreading.Lec_35.Ch3_CompletableFuture.Method2_thenApply;

import java.util.concurrent.*;

public class thenApplyAsync {

    public static void main(String[] args) {

        // creating executor
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5,
                5,
                1,
                TimeUnit.HOURS,
                new ArrayBlockingQueue<>(10),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        CompletableFuture<String> completableFutureObj1 = CompletableFuture.supplyAsync(()-> {
            try {
                System.out.println("Thread name of supplyAsync: " + Thread.currentThread().getName());
                Thread.sleep(5000);
            } catch (Exception e) {
                // exception handling
            }

            return "CONCEPT";
        }, executor);

        CompletableFuture<String> completableFutureObj2 = completableFutureObj1.thenApplyAsync((String val)-> {

            System.out.println("Thread name of thenApply: " + Thread.currentThread().getName());
            return val + " AND CODING";
        }, executor);

        try {
            String finalResult = completableFutureObj2.get();
            System.out.println(finalResult);
        } catch (Exception e) {
            // exception handling
        }


        // -------------------------------- Shutting down executor ------------------------
        executor.shutdown();
    }
}
