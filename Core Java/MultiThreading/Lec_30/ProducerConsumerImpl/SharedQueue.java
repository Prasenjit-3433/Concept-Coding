package MultiThreading.Lec_30.ProducerConsumerImpl;


import MultiThreading.Lec_30.ProducerConsumerImpl.Utils.FixedSizeQueue;
import MultiThreading.Lec_30.ProducerConsumerImpl.Utils.Message;

public class SharedQueue {
    private final int size;
    private final FixedSizeQueue queue;

    public SharedQueue(int size) {
        this.size = size;
        this.queue = new FixedSizeQueue(size);
    }

    private int genUniqueMessageID() {
        return (int) (Math.random() * 1000);
    }

    public synchronized void publish(String data) {

        // queue is empty
        while (queue.size() == size) {
            System.out.println("queue is full...Producer thread: " + Thread.currentThread().getName() + " waiting...");

            try {
                wait(); // releases the monitor lock
            } catch (Exception e) {
                // exception handing
            }
        }

        // create new message
        Message newMessage = new Message(data, genUniqueMessageID());

        // push into the queue
        queue.push(newMessage);

        System.out.printf(
                "{ID: '%d', data: '%s'} published by producer thread: " + Thread.currentThread().getName() + "%n",
                newMessage.ID(),
                newMessage.data()
        );

        // to wake-up consumer threads (if any waiting)
        notifyAll();
    }

    public synchronized void consume() {

        // when queue is empty
        while (queue.size() == 0) {
            System.out.println("Queue is empty!...Consumer thread: " + Thread.currentThread().getName() + " waiting...");

            try {
                wait(); // releases the monitor lock
            } catch (Exception e) {
                // exception handling
            }
        }

        Message message = queue.pop();

        System.out.printf(
                "{ID: '%d', data: '%s'} consumed by consumer thread: " + Thread.currentThread().getName() + "%n",
                message.ID(),
                message.data()
        );

        // to wake-up producer threads (if any waiting)
        notifyAll();
    }
}
