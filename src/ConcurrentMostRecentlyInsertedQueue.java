import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentMostRecentlyInsertedQueue<E> extends AbstractQueue<E> {

    private final Object lock = new Object();

    private final int maxQueueCapacity;
    private AtomicInteger currentQueueSize = new AtomicInteger(0);

    private volatile QueueItem<E> head;
    private volatile QueueItem<E> tail;


    public ConcurrentMostRecentlyInsertedQueue(int capacity) {

        this.maxQueueCapacity = capacity;
    }

    public boolean isEmpty() {

        return head == null;
    }


    public class QueueIterator<E> implements Iterator<E> {
        private QueueItem<E> head;
        private QueueItem<E> tail;

        private QueueItem<E> prev;
        private QueueItem<E> current;

        public QueueIterator(QueueItem<E> headQueue, QueueItem<E> tailQueue) {

            this.head = headQueue;
            this.tail = tailQueue;
            this.current = new QueueItem<>();
            this.current.setNext(head);
            this.prev = new QueueItem<>();
            this.prev.setNext(current);

        }

        @Override
        public boolean hasNext() {

            if (current == tail) return false;
           /* if (current.getNext() == head) return head != null;
            else return*/
            return current.getNext() != null;
        }

        @Override
        public E next() {

            synchronized (lock) {
                if (current.getNext() != null) {
                    prev = current;
                    current = current.getNext();
                    return current.getObject();
                } else throw new NoSuchElementException();
            }
        }


        @Override
        public void remove() {

            synchronized (lock) {
                if (current.getNext() != null) {
                    prev.setNext(current.getNext());
                    current = current.getNext();
                } else {
                    prev.setNext(null);
                    current.setNext(null);
                }
            }


//            throw new UnsupportedOperationException();

        }
    }

    /**
     * Returns an iterator over the elements contained in this collection.
     *
     * @return an iterator over the elements contained in this collection
     */
    @Override
    public Iterator<E> iterator() {

        return new QueueIterator<>(head, tail);
    }


    @Override
    public int size() {

        return currentQueueSize.get();
    }

    public void sizeIncrease() {

        currentQueueSize.getAndIncrement();
    }

    public void sizeDecrease() {

        currentQueueSize.getAndDecrement();
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
            synchronized (lock) {
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
            }
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
        synchronized (lock) {
            E item = head.getObject();

            head = head.getNext();

            if (head == null) {
                tail = null;
            }

            sizeDecrease();

            return item;
        }
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

        synchronized (lock) {
            while (it.hasNext()) {

                outputSting.append(it.next().toString());
            }
        }
        return "MostRecentlyInsertedQueue{" +
                "currentQueueSize=" + currentQueueSize.get() + " content: " + outputSting +
                '}';
    }

    /**
     * Constructor for use by subclasses.
     */
    protected ConcurrentMostRecentlyInsertedQueue() {

        super();
        maxQueueCapacity = 0;
    }

    /**
     * Inserts the specified element into this queue if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * <tt>true</tt> upon success and throwing an <tt>IllegalStateException</tt>
     * if no space is currently available.
     * <p>
     * <p>This implementation returns <tt>true</tt> if <tt>offer</tt> succeeds,
     * else throws an <tt>IllegalStateException</tt>.
     *
     * @param e the element to add
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     * @throws IllegalStateException    if the element cannot be added at this
     *                                  time due to capacity restrictions
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null and
     *                                  this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *                                  prevents it from being added to this queue
     */
    @Override
    public boolean add(E e) {

        return super.add(e);
    }

    /**
     * Retrieves and removes the head of this queue.  This method differs
     * from {@link #poll poll} only in that it throws an exception if this
     * queue is empty.
     * <p>
     * <p>This implementation returns the result of <tt>poll</tt>
     * unless the queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    @Override
    public E remove() {

        return super.remove();
    }

    /**
     * Retrieves, but does not remove, the head of this queue.  This method
     * differs from {@link #peek peek} only in that it throws an exception if
     * this queue is empty.
     * <p>
     * <p>This implementation returns the result of <tt>peek</tt>
     * unless the queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    @Override
    public E element() {

        return super.element();
    }

    /**
     * Removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     * <p>
     * <p>This implementation repeatedly invokes {@link #poll poll} until it
     * returns <tt>null</tt>.
     */
    @Override
    public void clear() {

        synchronized (lock) {

            super.clear();
        }
    }

    /**
     * Adds all of the elements in the specified collection to this
     * queue.  Attempts to addAll of a queue to itself result in
     * <tt>IllegalArgumentException</tt>. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     * <p>
     * <p>This implementation iterates over the specified collection,
     * and adds each element returned by the iterator to this
     * queue, in turn.  A runtime exception encountered while
     * trying to add an element (including, in particular, a
     * <tt>null</tt> element) may result in only some of the elements
     * having been successfully added when the associated exception is
     * thrown.
     *
     * @param c collection containing elements to be added to this queue
     * @return <tt>true</tt> if this queue changed as a result of the call
     * @throws ClassCastException       if the class of an element of the specified
     *                                  collection prevents it from being added to this queue
     * @throws NullPointerException     if the specified collection contains a
     *                                  null element and this queue does not permit null elements,
     *                                  or if the specified collection is null
     * @throws IllegalArgumentException if some property of an element of the
     *                                  specified collection prevents it from being added to this
     *                                  queue, or if the specified collection is this queue
     * @throws IllegalStateException    if not all the elements can be added at
     *                                  this time due to insertion restrictions
     * @see #add(Object)
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {

        synchronized (lock) {

            return super.addAll(c);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>This implementation iterates over the elements in the collection,
     * checking each element in turn for equality with the specified element.
     *
     * @param o
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean contains(Object o) {

        synchronized (lock) {

            return super.contains(o);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>This implementation returns an array containing all the elements
     * returned by this collection's iterator, in the same order, stored in
     * consecutive elements of the array, starting with index {@code 0}.
     * The length of the returned array is equal to the number of elements
     * returned by the iterator, even if the size of this collection changes
     * during iteration, as might happen if the collection permits
     * concurrent modification during iteration.  The {@code size} method is
     * called only as an optimization hint; the correct result is returned
     * even if the iterator returns a different number of elements.
     * <p>
     * <p>This method is equivalent to:
     * <p>
     * <pre> {@code
     * List<E> list = new ArrayList<E>(size());
     * for (E e : this)
     *     list.add(e);
     * return list.toArray();
     * }</pre>
     */
    @Override
    public Object[] toArray() {

        synchronized (lock) {

            return super.toArray();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>This implementation returns an array containing all the elements
     * returned by this collection's iterator in the same order, stored in
     * consecutive elements of the array, starting with index {@code 0}.
     * If the number of elements returned by the iterator is too large to
     * fit into the specified array, then the elements are returned in a
     * newly allocated array with length equal to the number of elements
     * returned by the iterator, even if the size of this collection
     * changes during iteration, as might happen if the collection permits
     * concurrent modification during iteration.  The {@code size} method is
     * called only as an optimization hint; the correct result is returned
     * even if the iterator returns a different number of elements.
     * <p>
     * <p>This method is equivalent to:
     * <p>
     * <pre> {@code
     * List<E> list = new ArrayList<E>(size());
     * for (E e : this)
     *     list.add(e);
     * return list.toArray(a);
     * }</pre>
     *
     * @param a
     * @throws ArrayStoreException  {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public <T> T[] toArray(T[] a) {

        synchronized (lock) {

            return super.toArray(a);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>This implementation iterates over the collection looking for the
     * specified element.  If it finds the element, it removes the element
     * from the collection using the iterator's remove method.
     * <p>
     * <p>Note that this implementation throws an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by this
     * collection's iterator method does not implement the <tt>remove</tt>
     * method and this collection contains the specified object.
     *
     * @param o
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
    @Override
    public boolean remove(Object o) {

        synchronized (lock) {

            return super.remove(o);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>This implementation iterates over the specified collection,
     * checking each element returned by the iterator in turn to see
     * if it's contained in this collection.  If all elements are so
     * contained <tt>true</tt> is returned, otherwise <tt>false</tt>.
     *
     * @param c
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @see #contains(Object)
     */
    @Override
    public boolean containsAll(Collection<?> c) {

        synchronized (lock) {

            return super.containsAll(c);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>This implementation iterates over this collection, checking each
     * element returned by the iterator in turn to see if it's contained
     * in the specified collection.  If it's so contained, it's removed from
     * this collection with the iterator's <tt>remove</tt> method.
     * <p>
     * <p>Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by the
     * <tt>iterator</tt> method does not implement the <tt>remove</tt> method
     * and this collection contains one or more elements in common with the
     * specified collection.
     *
     * @param c
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @Override
    public boolean removeAll(Collection<?> c) {

        synchronized (lock) {

            return super.removeAll(c);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>This implementation iterates over this collection, checking each
     * element returned by the iterator in turn to see if it's contained
     * in the specified collection.  If it's not so contained, it's removed
     * from this collection with the iterator's <tt>remove</tt> method.
     * <p>
     * <p>Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by the
     * <tt>iterator</tt> method does not implement the <tt>remove</tt> method
     * and this collection contains one or more elements not present in the
     * specified collection.
     *
     * @param c
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @Override
    public boolean retainAll(Collection<?> c) {

        synchronized (lock) {

            return super.retainAll(c);
        }
    }
}

