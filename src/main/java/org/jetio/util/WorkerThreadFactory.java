package org.jetio.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * a {@link ThreadFactory} that names its workers
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class WorkerThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber;
    private final String suffix;

    public WorkerThreadFactory( String name, int id ) {
        threadNumber = new AtomicInteger( 1 );
        suffix = " " + name + "[" + id + "]";
    }

    @Override
    public Thread newThread( Runnable r ) {
        return new Thread( r, "worker-" + threadNumber.getAndIncrement() + suffix );
    }
}
