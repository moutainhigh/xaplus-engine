package com.crionuke.xaplus;

import com.crionuke.bolts.Bolt;
import com.crionuke.xaplus.events.XAPlusTickEvent;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class XAPlusTickServiceTest extends XAPlusServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(XAPlusTickServiceTest.class);

    XAPlusTickService xaPlusTickService;

    BlockingQueue<XAPlusTickEvent> tickEvents;
    TickEventConsumer tickEventConsumer;

    @Before
    public void beforeTest() {
        createXAPlusComponents();

        xaPlusTickService = new XAPlusTickService(threadPool, dispatcher);
        xaPlusTickService.postConstruct();

        tickEvents = new LinkedBlockingQueue<>(QUEUE_SIZE);
        tickEventConsumer = new TickEventConsumer(tickEvents, threadPool, dispatcher);
        tickEventConsumer.postConstruct();
    }

    @After
    public void afterTest() {
        xaPlusTickService.finish();
        tickEventConsumer.finish();
    }

    @Test
    public void testTickEvents() throws InterruptedException {
        XAPlusTickEvent tick1 = tickEvents.poll(POLL_TIMIOUT_MS, TimeUnit.MILLISECONDS);
        XAPlusTickEvent tick2 = tickEvents.poll(POLL_TIMIOUT_MS, TimeUnit.MILLISECONDS);
        XAPlusTickEvent tick3 = tickEvents.poll(POLL_TIMIOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(tick1);
        assertEquals(tick1.getIndex(), 1);
        assertNotNull(tick2);
        assertEquals(tick2.getIndex(), 2);
        assertNotNull(tick3);
        assertEquals(tick3.getIndex(), 3);
    }

    private class TickEventConsumer extends Bolt
            implements XAPlusTickEvent.Handler {

        private final BlockingQueue<XAPlusTickEvent> tickEvents;
        private final XAPlusThreadPool xaPlusThreadPool;
        private final XAPlusDispatcher xaPlusDispatcher;

        TickEventConsumer(BlockingQueue<XAPlusTickEvent> tickEvents, XAPlusThreadPool xaPlusThreadPool,
                          XAPlusDispatcher xaPlusDispatcher) {
            super("tick-event-consumer", QUEUE_SIZE);
            this.tickEvents = tickEvents;
            this.xaPlusThreadPool = xaPlusThreadPool;
            this.xaPlusDispatcher = xaPlusDispatcher;
        }

        @Override
        public void handleTick(XAPlusTickEvent event) throws InterruptedException {
            tickEvents.put(event);
        }

        void postConstruct() {
            xaPlusThreadPool.execute(this);
            xaPlusDispatcher.subscribe(this, XAPlusTickEvent.class);
        }
    }
}
