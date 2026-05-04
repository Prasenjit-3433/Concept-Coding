package MultiThreading.Lec_35.Ch3_CompletableFuture.Method3_thenCompose;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {

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


    }
}
