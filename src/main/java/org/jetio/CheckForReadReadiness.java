package org.jetio;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

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

    private final ThreadLocal<ByteBuffer> readTestBuffer = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return ByteBuffer.allocateDirect( 1 );
        }
    };
    private final Publisher<Event> addToReadSelector;
    private final Publisher<DataEvent<Byte>> read;
    private final Publisher<DataEvent<IOException>> failed;

    CheckForReadReadiness( Publisher<Event> addToReadSelector,
                           Publisher<DataEvent<Byte>> read,
                           Publisher<DataEvent<IOException>> failed )
    {
        this.addToReadSelector = addToReadSelector;
        this.read = read;
        this.failed = failed;
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

        SocketChannel channel = session.channel();

        session.setNonBlocking();

        ByteBuffer buffer = readTestBuffer.get();
        buffer.position( 0 ).limit( 1 );
        int count = channel.read( buffer );

        switch( count ) {
            case 0: // nothing yet
                return false;
            case -1: // EOF
                throw new EOFException();
            default:
                logger.debug( "{} is ready to read", session );

                session.setBlocking();

                read.publish( new DataEvent<Byte>( session, buffer.get( 0 ) ) );

                return true;
        }
    }
}
