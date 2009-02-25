package org.jetio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

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
        SocketChannel channel = session.channel();
        ByteBuffer[] buffers = session.writeQueue();
        // TODO should we synchronize on the blocking lock? what if there is an attempt to change the blocking mode while we are writing?
        long written = channel.write( buffers );
        int cleared = 0;

        for ( ByteBuffer buffer : buffers ) {
            if ( buffer.remaining() == 0 ) {
                cleared++;
            } else {
                break;
            }
        }

        session.written( cleared );

        logger.debug( "wrote {} bytes to {}", written, session );
    }

}