package org.xaplus.engine;

import com.crionuke.bolts.Bolt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xaplus.engine.events.*;
import org.xaplus.engine.events.twopc.XAPlus2pcDoneEvent;
import org.xaplus.engine.events.twopc.XAPlus2pcFailedEvent;
import org.xaplus.engine.events.twopc.XAPlus2pcRequestEvent;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class XAPlusTimerService extends Bolt implements
        XAPlus2pcRequestEvent.Handler,
        XAPlusRollbackRequestEvent.Handler,
        XAPlusTickEvent.Handler,
        XAPlus2pcDoneEvent.Handler,
        XAPlusRollbackDoneEvent.Handler,
        XAPlus2pcFailedEvent.Handler,
        XAPlusRollbackFailedEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(XAPlusTimerService.class);

    private final XAPlusThreadPool threadPool;
    private final XAPlusDispatcher dispatcher;
    private final XAPlusTimerState state;

    XAPlusTimerService(XAPlusProperties properties, XAPlusThreadPool threadPool, XAPlusDispatcher dispatcher) {
        super("timer", properties.getQueueSize());
        this.threadPool = threadPool;
        this.dispatcher = dispatcher;
        state = new XAPlusTimerState();
    }

    @Override
    public void handle2pcRequest(XAPlus2pcRequestEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        XAPlusTransaction transaction = event.getTransaction();
        if (state.track(transaction)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Track timeout for xid={}", transaction.getXid());
            }
        }
    }

    @Override
    public void handleRollbackRequest(XAPlusRollbackRequestEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        XAPlusTransaction transaction = event.getTransaction();
        if (state.track(transaction)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Track timeout for xid={}", transaction.getXid());
            }
        }
    }

    @Override
    public void handleTick(XAPlusTickEvent event) throws InterruptedException {
        List<XAPlusTransaction> expiredTransaction = state.removeExpiredTransactions();
        for (XAPlusTransaction transaction : expiredTransaction) {
            dispatcher.dispatch(new XAPlusTimeoutEvent(transaction));
        }
    }

    @Override
    public void handle2pcDone(XAPlus2pcDoneEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        XAPlusTransaction transaction = event.getTransaction();
        if (state.remove(transaction.getXid())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Timeout tracking for xid={} cancelled as 2pc procotol done", transaction.getXid());
            }
            dispatcher.dispatch(new XAPlusTimerCancelledEvent(transaction));
        }
    }

    @Override
    public void handleRollbackDone(XAPlusRollbackDoneEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        XAPlusTransaction transaction = event.getTransaction();
        XAPlusXid xid = transaction.getXid();
        if (state.remove(xid)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Timeout tracking for xid={} cancelled as rollback protocol done", xid);
            }
            dispatcher.dispatch(new XAPlusTimerCancelledEvent(transaction));
        }
    }

    @Override
    public void handle2pcFailed(XAPlus2pcFailedEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        XAPlusTransaction transaction = event.getTransaction();
        XAPlusXid xid = transaction.getXid();
        if (state.remove(xid)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Timeout tracking for xid={} cancelled as 2pc protocol failed", xid);
            }
            dispatcher.dispatch(new XAPlusTimerCancelledEvent(transaction));
        }
    }

    @Override
    public void handleRollbackFailed(XAPlusRollbackFailedEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        XAPlusTransaction transaction = event.getTransaction();
        XAPlusXid xid = transaction.getXid();
        if (state.remove(xid)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Timeout tracking for xid={} cancelled as rollback protocol failed", xid);
            }
            dispatcher.dispatch(new XAPlusTimerCancelledEvent(transaction));
        }
    }

    @PostConstruct
    void postConstruct() {
        threadPool.execute(this);
        dispatcher.subscribe(this, XAPlus2pcRequestEvent.class);
        dispatcher.subscribe(this, XAPlusRollbackRequestEvent.class);
        dispatcher.subscribe(this, XAPlusTickEvent.class);
        dispatcher.subscribe(this, XAPlus2pcDoneEvent.class);
        dispatcher.subscribe(this, XAPlusRollbackDoneEvent.class);
        dispatcher.subscribe(this, XAPlus2pcFailedEvent.class);
        dispatcher.subscribe(this, XAPlusRollbackFailedEvent.class);
    }
}