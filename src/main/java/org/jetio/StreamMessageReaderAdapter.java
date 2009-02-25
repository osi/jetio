package org.jetio;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.channels.SocketChannel;

/**
 * Adapt a {@link StreamMessageReader} to a {@link MessageReader}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class StreamMessageReaderAdapter implements MessageReader {
    private final StreamMessageReader reader;

    public StreamMessageReaderAdapter( StreamMessageReader reader ) {
        this.reader = reader;
    }

    @Override
    public void readMessage( Session session, byte initialByte ) throws IOException {
        reader.readMessage( session, createInputStream( session.channel(), initialByte ) );
    }

    private InputStream createInputStream( SocketChannel channel, byte b ) throws IOException {
        PushbackInputStream in = new PushbackInputStream( channel.socket().getInputStream(), 1 );

        in.unread( b );

        return in;
    }

}
