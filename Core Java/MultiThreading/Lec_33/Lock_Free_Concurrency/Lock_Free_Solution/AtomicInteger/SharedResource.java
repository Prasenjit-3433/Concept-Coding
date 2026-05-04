package MultiThreading.Lec_33.Lock_Free_Concurrency.Lock_Free_Solution.AtomicInteger;

import java.util.concurrent.atomic.AtomicInteger;

public class SharedResource {

    AtomicInteger counter = new AtomicInteger(0);

    public void increment() {
        counter.incrementAndGet();
    }

    public int get() {
        return counter.get();
    }
}
