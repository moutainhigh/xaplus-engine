package org.xaplus;

import org.xaplus.exceptions.XAPlusCommitException;
import org.xaplus.exceptions.XAPlusRollbackException;
import org.xaplus.exceptions.XAPlusTimeoutException;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class XAPlusFuture {

    private final SynchronousQueue<XAPlusResult> container;

    XAPlusFuture() {
        container = new SynchronousQueue<>();
    }

    public boolean get()
            throws InterruptedException, XAPlusCommitException, XAPlusRollbackException, XAPlusTimeoutException {
        XAPlusResult result = container.poll();
        return result.get();
    }

    public boolean get(long timeout, TimeUnit unit)
            throws InterruptedException, XAPlusCommitException, XAPlusRollbackException, XAPlusTimeoutException {
        XAPlusResult result = container.poll();
        return result.get();
    }

    void put(XAPlusResult result) throws InterruptedException {
        container.put(result);
    }
}