package MultiThreading.Lec_33.Lock_Free_Concurrency.Problem;

public class Main {

    public static void main(String[] args) {

        SharedResource resource = new SharedResource();
        Thread thread1 = new Thread(()-> {
            for (int i = 0; i < 200; i++) {
                resource.increment();
            }
        });

        Thread thread2 = new Thread(()-> {
            for (int i = 0; i < 200; i++) {
                resource.increment();
            }
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (Exception e) {
            // exception handling
        }

        // Expected: 400, but sometimes got: 371 or 378 or 374 (lesser 400)
        System.out.println(resource.getCounter());
    }
}
