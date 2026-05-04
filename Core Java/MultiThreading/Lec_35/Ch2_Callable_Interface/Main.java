package MultiThreading.Lec_35.Ch2_Callable_Interface;

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

        // Runnable: No return value
        Future<?> futureObj1 = poolExecutor.submit(() -> System.out.println("Do something"));

        // Callable: has return value
        Future<Integer> futureObj2 = poolExecutor.submit(()-> {
            System.out.println("Do something");
            return 45;
        });

        poolExecutor.shutdown();
    }
}
