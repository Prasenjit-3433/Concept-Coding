package MultiThreading.Lec_36.Executors_Utility;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {

        // FixedThreadPoolExecutor
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);
        fixedThreadPool.submit(()-> "this is async task");

        // CachedThreadPool: dynamically create as many threads as needed
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        cachedThreadPool.submit(()-> "this is the async task");

        // SingleThreadExecutor: executor with just single worker thread
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadExecutor.submit(()-> "this is the async task");

    }
}
