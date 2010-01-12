package org.jetio;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jetlang.channels.Publisher;
import org.jetlang.core.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test a session to see if it is ready to have a message read
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class CheckForReadReadiness implements Callback<Event> {
    private static final Logger logger = LoggerFactory.getLogger( CheckForReadReadiness.class );

    private final ThreadLocal<ByteBuffer> readTestBuffer;
    private final Publisher<Event> addToReadSelector;
    private final Publisher<DataEvent<byte[]>> read;
    private final Publisher<DataEvent<IOException>> failed;

    CheckForReadReadiness( Publisher<Event> addToReadSelector,
                           Publisher<DataEvent<byte[]>> read,
                           Publisher<DataEvent<IOException>> failed,
                           final BufferSource buffers )
    {
        this.addToReadSelector = addToReadSelector;
        this.read = read;
        this.failed = failed;
        this.readTestBuffer = new ThreadLocal<ByteBuffer>() {
            @Override
            protected ByteBuffer initialValue() {
                return buffers.acquire();
            }
        };
    }

    @Override
    public void onMessage( Event event ) {
        Session session = event.session();

        try {
            if ( !read( session ) ) {
                addToReadSelector.publish( event );
            }
        } catch( IOException e ) {
            failed.publish( new DataEvent<IOException>( session, e ) );
        }
    }

    private boolean read( Session session ) throws IOException {
        logger.debug( "attempting read on {}", session );

        ByteBuffer buffer = readTestBuffer.get();
        int count = performRead( session, buffer );

        switch( count ) {
            case 0: // nothing yet
                return false;
            case -1: // EOF
                throw new EOFException();
            default:
                publishReadEvent( session, buffer, count );

                return true;
        }
    }

    private static int performRead( Session session, ByteBuffer buffer ) throws IOException {
        session.setNonBlocking();

        buffer.clear();

        return session.channel().read( buffer );
    }

    private static byte[] toByteArray( ByteBuffer buffer, int count ) {
        byte[] data = new byte[count];

        buffer.flip();
        buffer.get( data, 0, count);

        return data;
    }

    private void publishReadEvent( Session session, ByteBuffer buffer, int count ) throws IOException {
        logger.debug( "{} is ready to read, {} bytes available", session, count );

        session.setBlocking();

        read.publish( new DataEvent<byte[]>( session, toByteArray( buffer, count ) ) );
    }
}
