package org.jetio.util;

import java.util.concurrent.Executor;

import org.jetlang.core.BatchExecutor;

/**
 * a {@link BatchExecutor} that delegates each item to a {@link Executor}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class ExecutorBatchExecutor implements BatchExecutor {
    private final Executor executor;

    public ExecutorBatchExecutor( Executor executor ) {
        this.executor = executor;
    }

    @Override
    public void execute( Runnable[] toExecute ) {
        for ( Runnable runnable : toExecute ) {
            executor.execute( runnable );
        }
    }
}
