package org.jetio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.jetlang.channels.Channel;

/**
 * Manage active connections for writeability in a {@link Selector}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class WriteSelector extends AbstractSelector {

    WriteSelector( Channel<DataEvent<IOException>> failed, Configuration config ) throws IOException {
        super( SelectionOp.Write, config, failed );
    }

    @Override
    protected void selected( SelectionKey key, Session session ) throws IOException {
        session.processWriteQueue();
    }

    @Override
    protected void addToSelector( Session session ) throws IOException {
        SelectionKeys keys = session.selectionKeys();

        if ( !session.isBlocking() && !keys.hasKey( SelectionOp.Write ) ) {
            logger.debug( "adding {} to selector", session, op );

            // We don't want this to collide with a call to change the blocking mode
            // Acquire the lock, and if we are still non-blocking, then go ahead and schedule ourselves.
            synchronized( session.channel().blockingLock() ) {
                if ( !session.channel().isBlocking() ) {
                    keys.set( op, session.channel().register( selector, op.op(), session ) );
                }
            }
        }
    }
}