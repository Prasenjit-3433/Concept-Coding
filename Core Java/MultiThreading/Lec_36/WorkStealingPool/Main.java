package MultiThreading.Lec_36.WorkStealingPool;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class Main {

    public static void main(String[] args) {

        ForkJoinPool pool = ForkJoinPool.commonPool();
        Future<Integer> futureObj =  pool.submit(new ComputeSumTask(0, 100));

        try {
            int result = futureObj.get();
            System.out.println(result);
        } catch (Exception e) {
            // exception handling
        }
    }
}
