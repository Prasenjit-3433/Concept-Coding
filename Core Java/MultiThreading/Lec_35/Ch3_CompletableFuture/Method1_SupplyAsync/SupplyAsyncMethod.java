package MultiThreading.Lec_35.Ch3_CompletableFuture.Method1_SupplyAsync;

import java.util.concurrent.*;

public class SupplyAsyncMethod {

    public static void main(String[] args) {

        // creating executor
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                1,
                TimeUnit.HOURS,
                new ArrayBlockingQueue<>(10),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        // supplyAsync(Supplier): initiates an Async operation
        CompletableFuture<String> asyncTask1 = CompletableFuture.supplyAsync(()-> {
            // this is the task which need to be completed by thread
            return "task completed";
        }, executor);

        try {
            // CompletableFuture child of Future and hence get() is available on it as well
            String result = asyncTask1.get();
            System.out.println(result);
        } catch (Exception e) {
            // exception handling
        }
    }
}
