package MultiThreading.Lec_34.Hands_on;

import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {

        // Configuring & creating the pool
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                4,
                10,
                TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(2),
                new CustomThreadFactory(),
                new CustomRejectHandler()
        );

        // Optional: To have threads of pool to time out after 10 mins
        executor.allowCoreThreadTimeOut(true);
        System.out.println("allowsCoreThreadTimeOut: " + executor.allowsCoreThreadTimeOut());

        for (int i = 1; i <= 7; i++) {
            int taskID = i;
            executor.submit(()-> {
                System.out.println("Task-" + taskID + " being processed by: " + Thread.currentThread().getName());

                // simulate some work
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    // exception handling
                }

                System.out.println("Task-" + taskID + " finished processing by: " + Thread.currentThread().getName());
            });
        }

        // ThreadPoolExecutor stop accepting new task, but continue executing existing tasks
        executor.shutdown();
    }
}

// Creating own implementation of `ThreadFactory`
class CustomThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(false);

        return thread;
    }
}

// Creating own implementation of `RejectedExecutionHandler`
class CustomRejectHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

        System.out.println("Task rejected: " + r.toString());
    }
}
