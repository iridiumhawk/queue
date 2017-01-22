import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class MostRecentlyInsertedQueue<E> extends AbstractQueue<E> {

    private final int maxQueueCapacity;
    private int currentQueueSize = 0;

    private QueueItem<E> head = null;
    private QueueItem<E> tail = null;
    private QueueItem<E> current = null;

    public MostRecentlyInsertedQueue(int capacity) {

        this.maxQueueCapacity = capacity;
    }

    private Iterator<E> iterator = new Iterator<E>() {


        @Override
        public boolean hasNext() {

            if (current == null) return head != null;
            else return current.getNext() != null;
        }

        @Override
        public E next() {

            if (current == null) current = head;
            else current = current.getNext();

            if (current != null) return current.getObject();
            else throw new NoSuchElementException();

        }

        @Override
        public void remove() {

            throw new UnsupportedOperationException();

        }
    };

    /**
     * Returns an iterator over the elements contained in this collection.
     *
     * @return an iterator over the elements contained in this collection
     */
    @Override
    public Iterator<E> iterator() {

        return iterator;
    }


    @Override
    public int size() {

        return currentQueueSize;
    }

    public void sizeIncrease() {

        this.currentQueueSize++;
    }

    public void sizeDecrease() {

        this.currentQueueSize--;
    }

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions.
     * When using a capacity-restricted queue, this method is generally
     * preferable to {@link #add}, which can fail to insert an element only
     * by throwing an exception.
     *
     * @param e the element to add
     * @return <tt>true</tt> if the element was added to this queue, else
     * <tt>false</tt>
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null and
     *                                  this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *                                  prevents it from being added to this queue
     */
    @Override
    public boolean offer(E e) {

        if (e == null) throw new NullPointerException();

        try {
            if (size() < maxQueueCapacity) {

                QueueItem<E> item = new QueueItem<>();

                item.setObject(e);

                if (head == null) {
                    head = item;
                } else {
                    tail.setNext(item);
                }

                tail = item;

                sizeIncrease();

                return true;

            } else {

                poll();
                offer(e);
            }

            return true;

        } catch (ClassCastException error) {
            error.printStackTrace();
        }

        return false;
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns <tt>null</tt> if this queue is empty.
     *
     * @return the head of this queue, or <tt>null</tt> if this queue is empty
     */
    @Override
    public E poll() {

        if (size() == 0) {
            return null;
        }

        E item = head.getObject();

        head = head.getNext();

        if (head == null) {
            tail = null;
        }

        sizeDecrease();

        return item;
    }

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns <tt>null</tt> if this queue is empty.
     *
     * @return the head of this queue, or <tt>null</tt> if this queue is empty
     */
    @Override
    public E peek() {

        if (size() == 0) {
            return null;
        }

        E item = head.getObject();

        return item;
    }

    private class QueueItem<T> {
        private T item;
        private QueueItem<T> next;

        public T getObject() {

            return item;
        }

        public void setObject(T item) {

            this.item = item;
        }

        public QueueItem<T> getNext() {

            return next;
        }

        public void setNext(QueueItem<T> next) {

            this.next = next;
        }
    }

    @Override
    public String toString() {

        StringBuffer outputSting = new StringBuffer();

        Iterator<E> it = iterator();
        while (it.hasNext()) {

            outputSting.append(it.next().toString());
        }
        return "MostRecentlyInsertedQueue{" +
                "currentQueueSize=" + currentQueueSize + " content: " + outputSting +
                '}';
    }

}
