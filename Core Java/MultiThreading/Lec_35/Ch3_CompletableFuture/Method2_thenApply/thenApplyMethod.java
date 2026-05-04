package MultiThreading.Lec_35.Ch3_CompletableFuture.Method2_thenApply;

import java.util.concurrent.*;

public class thenApplyMethod {

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

        // supplyAsync(Supplier) -> CompletableFuture
        // thenApply(Function) -> CompletableFuture
        CompletableFuture<String> asyncTask1 = CompletableFuture.supplyAsync(()-> {
            // task which thread need to execute
            return "Concept and ";
        }, executor).thenApply((String val)-> {
            // functionality which can work on the result of previous async work
            return val + "Coding";
        });
    }
}
