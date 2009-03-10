package org.jetio;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.jetlang.channels.Publisher;

/**
 * Factory for new {@link Session} instances
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class SessionFactory {
    private final Publisher<Event> addToWriteSelector;
    private final Publisher<DataEvent<IOException>> failed;
    private final Publisher<Event> closed;
    private final BufferSource buffers;

    SessionFactory( BufferSource buffers,
                    Publisher<Event> addToWriteSelector,
                    Publisher<DataEvent<IOException>> failed,
                    Publisher<Event> closed ) {
        this.buffers = buffers;
        this.addToWriteSelector = addToWriteSelector;
        this.failed = failed;
        this.closed = closed;
    }

    Session create( SocketChannel channel ) {
        return new Session( channel, addToWriteSelector, failed, closed, new SessionBufferSource( buffers ) );
    }
}
