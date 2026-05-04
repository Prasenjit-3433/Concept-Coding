package MultiThreading.Lec_35.Ch2_Callable_Interface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SubmitMethod {

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

        // submit method has 3 flavours

        // ---------------------------------- UseCase-1: submit(Runnable) ------------------------------------------
        Future<?> futureObj1 = poolExecutor.submit(() -> System.out.println("Do something"));

        // FutureTask has 3 component: Runnable, State, return_value
        // in case of Runnable, return_value is always null (hard coded!)

        try {
            Object obj = futureObj1.get();
            System.out.println("is return value `null`: " + (obj == null));
        } catch (Exception e) {
            // error handling
        }


        // ---------------------------------- UseCase-2: submit(Runnable, T) --------------------------------------
        List<Integer> commonObj = new ArrayList<>();
        Future<List<Integer>>futureObj2 = poolExecutor.submit(new MyRunnable(commonObj), commonObj);

        try {
            // wait for completion of task
            List<Integer> output = futureObj2.get();

            System.out.println("use case 2: " + output);
        } catch (Exception e) {
            // error handling
        }

        // ---------------------------------- UseCase-3: submit(Callable) -----------------------------------------
        Future<List<Integer>> futureObj3 = poolExecutor.submit(()-> {
            // odd num from 27 to 50
            Stream<Integer> stream = Stream.iterate(27, (Integer a)-> a + 1)
                    .limit(50).
                    filter((Integer a)-> a % 2 != 0);
            List<Integer> ouput = stream.collect(Collectors.toList());
            return ouput;
        });

        try {
            List<Integer> result = futureObj3.get();
            System.out.println("use case 3: " + result);
        } catch (Exception e) {
            // exception handling
        }

        // ---------------------- shutting down pool ------------
        poolExecutor.shutdown();
    }
}
