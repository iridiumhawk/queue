import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.NoSuchElementException;
import java.util.Queue;

import static org.junit.Assert.*;

public class MostRecentlyInsertedQueueTest {
    private Queue<Integer> queue;
    private final int capacity = 10;


    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {

        queue = new MostRecentlyInsertedQueue<>(capacity);
    }

    @Test
    public void iterator() throws Exception {

        assertEquals(false, queue.iterator().hasNext());

        queue.offer(1);
        queue.offer(2);
        assertEquals(true, queue.iterator().hasNext());
        assertSame(1, queue.iterator().next());
        assertSame(2, queue.iterator().next());

        queue.poll();
        assertEquals(false, queue.iterator().hasNext());

    }

    @Test
    public void iteratorFailUnsupportedOperationException() throws Exception {

        queue.offer(1);
        queue.iterator().next();

        exception.expect(UnsupportedOperationException.class);

        queue.iterator().remove();

    }

    @Test
    public void iteratorFailNoSuchElementException() throws Exception {

        queue.offer(1);
        queue.iterator().next();

        exception.expect(NoSuchElementException.class);

        queue.iterator().next();

    }

    @Test
    public void size() throws Exception {

        assertEquals(0, queue.size());

        queue.offer(1);
        assertEquals(1, queue.size());

        queue.poll();
        assertEquals(0, queue.size());

        for (int i = 0; i < capacity + 1; i++) {
            queue.offer(1);
        }

        assertEquals(capacity, queue.size());
    }


    @Test
    public void offer() throws Exception {

        for (int i = 0; i < capacity + 1; i++) {
            assertEquals(true, queue.offer(i));
        }
        assertSame(1, queue.poll());

    }

    @Test
    public void offerFailNullPointerException() throws Exception {

        exception.expect(NullPointerException.class);
        queue.offer(null);

    }

    @Test
    public void poll() throws Exception {

        queue.offer(1);
        assertSame(1, queue.poll());
        assertSame(null, queue.poll());
    }

    @Test
    public void peek() throws Exception {

        queue.offer(1);
        assertSame(1, queue.peek());
    }

}