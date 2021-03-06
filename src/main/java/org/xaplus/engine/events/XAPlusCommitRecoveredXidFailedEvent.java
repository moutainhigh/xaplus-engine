package org.xaplus.engine.events;

import com.crionuke.bolts.Event;
import org.xaplus.engine.XAPlusXid;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class XAPlusCommitRecoveredXidFailedEvent extends Event<XAPlusCommitRecoveredXidFailedEvent.Handler> {

    private final XAPlusXid xid;
    private final Exception exception;

    public XAPlusCommitRecoveredXidFailedEvent(XAPlusXid xid, Exception exception) {
        super();
        if (xid == null) {
            throw new NullPointerException("xid is null");
        }
        if (exception == null) {
            throw new NullPointerException("exception is null");
        }
        this.xid = xid;
        this.exception = exception;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleCommitRecoveredXidFailed(this);
    }

    public XAPlusXid getXid() {
        return xid;
    }

    public Exception getException() {
        return exception;
    }

    public interface Handler {
        void handleCommitRecoveredXidFailed(XAPlusCommitRecoveredXidFailedEvent event) throws InterruptedException;
    }
}