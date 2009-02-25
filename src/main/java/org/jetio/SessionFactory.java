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

    SessionFactory( Publisher<Event> addToWriteSelector, Publisher<DataEvent<IOException>> failed ) {
        this.addToWriteSelector = addToWriteSelector;
        this.failed = failed;
    }

    Session create( SocketChannel channel ) {
        return new Session( channel, addToWriteSelector, failed );
    }
}
