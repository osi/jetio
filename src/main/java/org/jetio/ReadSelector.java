package org.jetio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.jetlang.channels.Channel;

/**
 * Manage active connections for readability in a {@link Selector}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class ReadSelector extends AbstractSelector {
    private final Channel<Event> readNext;

    ReadSelector( Channel<Event> readNext, Channel<DataEvent<IOException>> failed, Configuration config )
        throws IOException
    {
        super( SelectionOp.Read, config, failed );

        this.readNext = readNext;
    }

    @Override
    protected void selected( SelectionKey key, Session session ) {
        // Take the key out of the selector, since we will (likely) be handling reads blocking-style for now.
        // It is probably necessary to remove the key here, since we will do the read in another thread, and
        // do not want to end up firing two events for it. (In case this loop is fast enough)
        key.cancel();

        readNext.publish( new Event( session ) );
    }
}