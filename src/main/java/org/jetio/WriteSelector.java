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
}