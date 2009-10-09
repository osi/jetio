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
    public void readMessage( Session session, byte[] initialData ) throws IOException {
        reader.readMessage( session, createInputStream( session.channel(), initialData ) );
    }

    private InputStream createInputStream( SocketChannel channel, byte[] data ) throws IOException {
        PushbackInputStream in = new PushbackInputStream( channel.socket().getInputStream(), data.length );

        in.unread( data );

        return in;
    }

}
