import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class ConcurrentMostRecentlyInsertedQueueTest {

    private Queue<Integer> queue;
    private final int capacityQueue = 100;
    private final int threadCounter = 10;
// for stress test
//    private final TimeUnit timeUnit = TimeUnit.MINUTES;
//    private final int repeatInTimeUnits = 1;

    private final int repeatCounter = 1000000;

    private static ArrayList<TestResults> testResults = new ArrayList<>();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {

        queue = new ConcurrentMostRecentlyInsertedQueue<>(capacityQueue);
    }

    @AfterClass
    public static void down() throws Exception {
    // print test results

        for (TestResults tr : testResults) {
            System.out.println("-----------------------");
            System.out.println("Test method: " + tr.name);
            for (TestEntity te : tr.threadList) {
                System.out.println(te.name + " - exectime: " + te.timeLong + "ms, errors: " + te.errorCounter + ", repeats: " + te.repeatCounter + ", success: " + te.successCounter + ", mediumQueueSize: " + te.mediumQueueSize);
            }
        }
    }


    @Test
    public void remove() throws Exception {

        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        queue.offer(4);
        queue.remove(4);

//        queue.clear();
//        assertTrue(queue.isEmpty());
//        queue.remove(null);

//        System.out.println(queue.toString());

        assertSame(1, queue.poll());
        assertSame(2, queue.poll());
        assertSame(3, queue.poll());
    }

    //todo make as another tests
    @Test
    public void removeThreads() throws Exception {

        TestExpression testRemove = new TestExpression() {
            private int innerCounter = 0;
            private Random rand = new Random();

            @Override
            public boolean test() {

                innerCounter++;
                if (innerCounter % 10 == 0) {
                    return queue.remove(innerCounter);
                }
                return false;
            }
        };

        runThreads(testRemove, "remove");
    }

    @Test
    public void removeAll() throws Exception {
        //todo
    }


    @Test
    public void clear() throws Exception {

        queue.offer(1);
        queue.offer(2);
        queue.clear();

        assertSame(null, queue.poll());
    }


    @Test
    public void clearThreads() throws Exception {

        runThreads(new TestExpressionClear(), "clear");
    }


    @Test
    public void toStringTest() throws Exception {

        runThreads(new TestExpressionToString(), "toString");
    }

    @Test
    public void iterator() throws Exception {

        assertEquals(false, queue.iterator().hasNext());

        queue.offer(1);
        assertEquals(true, queue.iterator().hasNext());

        queue.offer(2);
        Iterator<Integer> it = queue.iterator();

        assertEquals(true, it.hasNext());
        assertSame(1, it.next());
        assertSame(2, it.next());

        assertEquals(false, it.hasNext());


        exception.expect(NoSuchElementException.class);
        it.next();

        assertEquals(false, it.hasNext());

        queue.offer(1);
        queue.poll();
        assertEquals(false, queue.iterator().hasNext());
    }

    @Test
    public void iteratorThreads() throws Exception {

        runThreads(new TestExpressionIterator(), "iterator");
    }

/*    @Test
    @Ignore
    public void iteratorFailUnsupportedOperationException() throws Exception {

        queue.offer(1);
        Iterator<Integer> it = queue.iterator();

        it.next();

        exception.expect(UnsupportedOperationException.class);

        it.remove();

    }*/

    @Test
    public void size() throws Exception {

        assertEquals(0, queue.size());

        queue.offer(1);
        assertEquals(1, queue.size());

        queue.poll();
        assertEquals(0, queue.size());

        for (int i = 0; i < capacityQueue + 1; i++) {
            queue.offer(1);
        }

        assertEquals(capacityQueue, queue.size());
    }


    @Test
    public void offer() throws Exception {

        for (int i = 0; i < capacityQueue + 1; i++) {
            assertEquals(true, queue.offer(i));
        }
        assertSame(1, queue.poll());
    }

    @Test
    public void offerThreads() throws Exception {
        runThreads(new TestExpressionOffer(), "offer");
    }


    @Test
    public void poll() throws Exception {

        queue.offer(1);
        assertSame(1, queue.poll());
        assertSame(null, queue.poll());
    }


    @Test
    public void pollThreads() throws Exception {
        runThreads(new TestExpressionPoll(), "poll");
    }

    @Test
    public void peek() throws Exception {

        queue.offer(1);
        assertSame(1, queue.peek());
    }


    @Test
    public void peekThreads() throws Exception {
        runThreads(new TestExpressionPeek(), "peak");
    }

    @Test
    public void offerFailNullPointerException() throws Exception {

        exception.expect(NullPointerException.class);
        queue.offer(null);
    }

    //threads executor
    private void runThreads(final TestExpression expression, String testName) throws Exception {

        final TestResults currentTest = new TestResults(testName);
        testResults.add(currentTest);
        currentTest.start();

        final CountDownLatch latch = new CountDownLatch(threadCounter);

        Runnable runner = new Runnable() {
            @Override
            public void run() {

                TestEntity currentTestThread = new TestEntity(Thread.currentThread().getName());
                currentTest.addToList(currentTestThread);

                currentTestThread.start();

                int counter = 0;

                for (int i = 0; i < repeatCounter; i++) {
                    try {
                        queue.offer(i);

                        if (expression.test()) {
                            currentTestThread.success();
                        }

                        counter++;
                    } catch (Exception e) {
                        currentTestThread.error();
                    }

                    currentTestThread.calculateQueueSize(queue.size());
                }
                currentTestThread.end();
                currentTestThread.setRepeatCounter(counter);

                assertEquals(counter, repeatCounter);
                latch.countDown();
            }
        };

        ExecutorService service = Executors.newFixedThreadPool(threadCounter);

        for (int i = 0; i < threadCounter; i++) {
            service.execute(runner);
        }

        latch.await();
        currentTest.end();
    }


    interface TestExpression {
        boolean test();
    }

    private class TestEntity {
        protected String name;
        private Long timeBeginTest;
        private Long timeEndTest;
        private Long timeLong;
        private int repeatCounter;
        private int successCounter;
        private int errorCounter;
        private int mediumQueueSize;

        public TestEntity(String name) {

            this.name = name;
        }

        private TestEntity() {

        }

        public void calculateQueueSize(int size) {

            this.mediumQueueSize = mediumQueueSize == 0 ? size : (mediumQueueSize + size) / 2;
        }

        public void success() {

            this.successCounter++;
        }


        public void setRepeatCounter(int repeatCounter) {

            this.repeatCounter = repeatCounter;
        }


        public void start() {

            this.timeBeginTest = System.currentTimeMillis();
        }

        public void end() {

            this.timeEndTest = System.currentTimeMillis();
            this.timeLong = timeEndTest - timeBeginTest;
        }

        public void error() {

            this.errorCounter++;
        }

    }

    //todo change to lambda in java8
    private class TestExpressionPeek implements TestExpression {
        @Override
        public boolean test() {

            return queue.peek() != null;

        }
    }

    private class TestExpressionToString implements TestExpression {
        @Override
        public boolean test() {

            return queue.toString() != null;

        }
    }

    private class TestExpressionPoll implements TestExpression {
        @Override
        public boolean test() {

            return queue.poll() != null;
        }
    }


    private class TestExpressionIterator implements TestExpression {
        @SuppressWarnings("WhileLoopReplaceableByForEach")
        @Override
        public boolean test() {

            boolean result = false;

            Iterator<Integer> it = queue.iterator();

            while (it.hasNext()) {
                result = it.next() != null;
            }

            return result;
        }
    }


    private class TestExpressionOffer implements TestExpression {
        @Override
        public boolean test() {

            return true;
        }
    }


    private class TestExpressionClear implements TestExpression {
        private int innerCounter = 0;

        @Override
        public boolean test() {

            innerCounter++;
            if (innerCounter % 200 == 0) {
                queue.clear();
                return queue.isEmpty();
            }

            return false;
        }
    }

    private class TestResults extends TestEntity {
        private volatile ArrayList<TestEntity> threadList = new ArrayList<>();

        public TestResults(String testName) {

            this.name = testName;

        }

        public void addToList(TestEntity testEntity) {

            threadList.add(testEntity);
        }
    }
}

