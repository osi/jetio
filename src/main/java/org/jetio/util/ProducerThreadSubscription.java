package org.jetio.util;

import org.jetlang.channels.BaseSubscription;
import org.jetlang.channels.Subscribable;
import org.jetlang.core.Callback;
import org.jetlang.core.DisposingExecutor;
import org.jetlang.core.Filter;

/**
 * A {@link Subscribable} that invokes the callback on the message producer's thread.
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class ProducerThreadSubscription<T> extends BaseSubscription<T> {
    private final Callback<T> receiveMethod;

    public ProducerThreadSubscription( DisposingExecutor fiber, Callback<T> receiveMethod ) {
        this( fiber, receiveMethod, null );
    }

    public ProducerThreadSubscription( DisposingExecutor fiber, Callback<T> receiveMethod, Filter<T> filter ) {
        super( fiber, filter );
        this.receiveMethod = receiveMethod;
    }

    @Override
    protected void onMessageOnProducerThread( T msg ) {
        receiveMethod.onMessage( msg );
    }
}
