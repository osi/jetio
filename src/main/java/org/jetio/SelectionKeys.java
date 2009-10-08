package org.jetio;

import java.nio.channels.SelectionKey;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manage the Read and Write {@link SelectionOp} keys
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class SelectionKeys {
    private final Map<SelectionOp, AtomicReference<SelectionKey>> keys =
        new EnumMap<SelectionOp, AtomicReference<SelectionKey>>( SelectionOp.class );

    private final Session session;

    SelectionKeys( Session session ) {
        this.session = session;
        for ( SelectionOp op : SelectionOp.values() ) {
            keys.put( op, new AtomicReference<SelectionKey>() );
        }
    }

    /**
     * Set the {@link SelectionKey} for the specified operation
     *
     * @param op  {@link SelectionOp} to set the key for
     * @param key {@link SelectionKey} to set
     *
     * @throws IllegalStateException if there is already a key for the specified op
     */
    void set( SelectionOp op, SelectionKey key ) {
        if ( !keys.get( op ).compareAndSet( null, key ) ) {
            throw new IllegalStateException( "already have a " + op + " key for " + session );
        }
    }

    boolean hasKey( SelectionOp op ) {
        return null != keys.get( op ).get();
    }

    /** Cancel all keys */
    void cancel() {
        for ( AtomicReference<SelectionKey> reference : keys.values() ) {
            cancelKey( reference );
        }
    }

    /**
     * Cancel the key for the specified operation, if one exists
     *
     * @param op {@link SelectionOp} to cancel the key for
     */
    void cancel( SelectionOp op ) {
        cancelKey( keys.get( op ) );
    }

    private void cancelKey( AtomicReference<SelectionKey> reference ) {
        SelectionKey key = reference.getAndSet( null );

        if ( null != key ) {
            key.cancel();
        }
    }
}
