import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ConcurrentMostRecentlyInsertedQueueTest {

    private Queue<Integer> queue;
    private final int capacityQueue = 1000;
    private final int threadCounter = 10;
// for stress test
//    private final TimeUnit timeUnit = TimeUnit.MINUTES;
//    private final int repeatInTimeUnits = 1;

    private final int repeatCounter = 1000000; //time of 1 million repeat about 9 seconds

    private final int progressDivider = 10; //haw often print progress on console

    private static ArrayList<ResultsOfOneTest> testResults = new ArrayList<>();
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {

        queue = new ConcurrentMostRecentlyInsertedQueue<>(capacityQueue);
    }

    @AfterClass
    public static void down() throws Exception {
        // print test results

        for (ResultsOfOneTest tr : testResults) {

            System.out.println("----------------------------------");
            System.out.println("Test method: " + tr.name);

            for (ResultsOfOneThreadTest te : tr.threadList) {
                System.out.println(te.name + " - exectime: " + te.timeLong + "ms, errors: " + te.errorCounter + ", repeats: " + te.repeatCounter + ", success: " + te.successCounter + ", mediumQueueSize: " + te.mediumQueueSize);
            }
        }
    }


    @Test
    public void remove() throws Exception {

        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        queue.remove(1);

//        assertSame(1, queue.poll());
        assertSame(2, queue.poll());
        assertSame(3, queue.poll());
    }

    @Test
    public void removeThreads() throws Exception {
        runThreads(new TestExpressionRemove(), "remove");
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
    private void runThreads(final TestExecution execution, String testName) throws Exception {

        final ResultsOfOneTest currentTest = new ResultsOfOneTest(testName);
        testResults.add(currentTest);
        currentTest.start();

        final CountDownLatch latch = new CountDownLatch(threadCounter);

        Runnable runner = new Runnable() {
            @Override
            public void run() {

                ResultsOfOneThreadTest currentThreadTest = new ResultsOfOneThreadTest(Thread.currentThread().getName());
                currentTest.addToList(currentThreadTest);

                currentThreadTest.start();

                int counter = 0;
//                int progressCounter = repeatCounter / progressDivider;

                for (int i = 0; i < repeatCounter; i++) {
                    try {
                        queue.offer(i);

                        if (execution.test()) {
                            currentThreadTest.success();
                        }

                        //write progress for one thread in shared field of test
                        if (i % (repeatCounter / progressDivider) == 0) {
                            currentTest.addProgressPercent(100 / progressDivider);
                        }


                        counter++;
                    } catch (Exception e) {
                        currentThreadTest.error();
                    }

                    currentThreadTest.calculateQueueSize(queue.size());
                }
                currentThreadTest.end();
                currentThreadTest.setRepeatCounter(counter);

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


    interface TestExecution {
        boolean test();
    }

    private class ResultsOfOneThreadTest {
        protected String name;
        private Long timeBeginTest;
        private Long timeEndTest;
        private Long timeLong;
        private int repeatCounter;
        private int successCounter;
        private int errorCounter;
        private int mediumQueueSize;

        public ResultsOfOneThreadTest(String name) {

            this.name = name;
        }

        private ResultsOfOneThreadTest() {

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
    private class TestExpressionPeek implements TestExecution {
        @Override
        public boolean test() {
            return queue.peek() != null;
        }
    }

    private class TestExpressionToString implements TestExecution {
        @Override
        public boolean test() {
            return queue.toString() != null;
        }
    }

    private class TestExpressionPoll implements TestExecution {
        @Override
        public boolean test() {
            return queue.poll() != null;
        }
    }


    private class TestExpressionIterator implements TestExecution {
//        @SuppressWarnings("WhileLoopReplaceableByForEach")
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

    private class TestExpressionRemove implements TestExecution {
        private int innerCounter;
        private Random rand = new Random();

        @Override
        public boolean test() {

            innerCounter++;
            if (innerCounter % 10 == 0) {
                return queue.remove(rand.nextInt(innerCounter)+1);
            }
            return false;
        }
    }

    private class TestExpressionOffer implements TestExecution {
        @Override
        public boolean test() {
            return true;
        }
    }

    private class TestExpressionClear implements TestExecution {
        private int innerCounter;

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

    private class ResultsOfOneTest extends ResultsOfOneThreadTest {
        private volatile ArrayList<ResultsOfOneThreadTest> threadList = new ArrayList<>();
        private AtomicInteger progressPercent = new AtomicInteger(0);

        public ResultsOfOneTest(String testName) {

            this.name = testName;
            System.out.println("Test " + this.name + " progress: ");


            //print progress of test in %
            Thread monitor = new TestProgressMonitor(threadCounter,progressDivider,progressPercent);
            monitor.setDaemon(true);
            monitor.start();

        }

        public void addToList(ResultsOfOneThreadTest testEntity) {

            threadList.add(testEntity);
        }

        public void addProgressPercent(int delta) {

            progressPercent.getAndAdd(delta);
        }
    }

    private class TestProgressMonitor extends Thread {
        int threadCounter;
        int progressDivider;
        AtomicInteger progressPercent;

        public TestProgressMonitor(int threadCounter, int progressDivider, AtomicInteger progressPercent) {
            this.threadCounter = threadCounter;
            this.progressDivider = progressDivider;
            this.progressPercent = progressPercent;
        }

        @Override
            public void run() {

                int currentProgress = 0;
                try {
                    while (true) {

                        if (progressPercent.get() / threadCounter > 100 / progressDivider + currentProgress) {
                            currentProgress += (100 / progressDivider);
                            System.out.println(currentProgress+"%");
                        }

                        Thread.sleep(100);
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


}

