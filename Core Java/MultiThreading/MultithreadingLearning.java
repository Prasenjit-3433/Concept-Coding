package MultiThreading;

public class MultithreadingLearning {

    public static void main(String[] args) {
        System.out.println("Thread Name: " + Thread.currentThread().getName());
        System.out.println("Total logical threads: " + Runtime.getRuntime().availableProcessors());
    }
}
