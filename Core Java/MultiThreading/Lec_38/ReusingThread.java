package MultiThreading.Lec_38;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReusingThread {

    public static void main(String[] args) {

        ThreadLocal<String> threadLocalObj = new ThreadLocal<>();

        // pool
        ExecutorService poolObj = Executors.newFixedThreadPool(5);

        poolObj.submit(()-> {
            threadLocalObj.set(Thread.currentThread().getName());

            // when work completed, clean up
            threadLocalObj.remove();
        });

        for (int i = 0; i < 15; i++) {
            poolObj.submit(()->{
                System.out.println(threadLocalObj.get());
            });
        }

    }
}
