package MultiThreading.Lec_36.WorkStealingPool;

import java.util.concurrent.RecursiveTask;

public class ComputeSumTask extends RecursiveTask<Integer> {

    int start;
    int end;

    public ComputeSumTask(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {

        if (end - start <= 4) {
            int totalSum = 0;
            for (int i = start; i <= end; i++) {
                totalSum += i;
            }
            return totalSum;
        } else {
            // split the task
            int mid = (start + end) / 2;

            ComputeSumTask leftTask = new ComputeSumTask(start, mid);
            ComputeSumTask rightTask = new ComputeSumTask(mid+1, end);

            // Fork tasks for parallel execution
            leftTask.fork();
            rightTask.fork();

            // wait for the subtasks to get complete
            int leftResult = leftTask.join();
            int rightResult = rightTask.join();

            // combine the results
            return leftResult + rightResult;
        }

    }
}
