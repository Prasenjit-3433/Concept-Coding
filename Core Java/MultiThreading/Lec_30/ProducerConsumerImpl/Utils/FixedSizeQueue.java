package MultiThreading.Lec_30.ProducerConsumerImpl.Utils;

public class FixedSizeQueue {
    private final int size;
    private final Message[] arr;
    private int currSize; // needed, as circular movement of start, end
    private int start;
    private int end;

    public FixedSizeQueue(int size) {
        this.size = size;
        this.arr = new Message[size];
        this.currSize = 0;
        this.start = -1;
        this.end = -1;
    }

    public void push(Message message) {
        if (currSize == size) {
            System.out.println("Queue is full!");
            return;
        }

        // when empty
        if (currSize == 0) {
            end = 0;
            start = 0;
        } else {
            // edge case: when end ptr goes "Out of Bound" - circular
            end = (end + 1) % size;
        }

        arr[end] = message;
        currSize++;
    }

    public Message pop() {
        if (currSize == 0) {
            System.out.println("Queue is empty!");
            return null;
        }


        Message el = arr[start];

        // when 1 element left
        if (currSize == 1) {
            start = -1;
            end = -1;
        } else {
            start = (start + 1) % size;
        }

        currSize--;
        return el;
    }

    public Message top() {
        if (currSize == 0) {
            System.out.println("Queue is empty!");
            return null;
        }

        return arr[end];
    }

    public int size() {
        return currSize;
    }
}
