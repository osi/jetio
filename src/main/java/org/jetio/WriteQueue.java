package org.jetio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetlang.channels.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the {@link ByteBuffer}s to write for a {@link Session}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class WriteQueue {
    private static final Logger logger = LoggerFactory.getLogger( WriteQueue.class );

    private final List<ByteBuffer> queue = Collections.synchronizedList( new ArrayList<ByteBuffer>() );
    private final Publisher<Event> addToWriteSelector;
    private final Publisher<DataEvent<IOException>> failed;
    private final Session session;

    WriteQueue( Session session, Publisher<Event> addToWriteSelector, Publisher<DataEvent<IOException>> failed ) {
        this.session = session;
        this.addToWriteSelector = addToWriteSelector;
        this.failed = failed;
    }

    // TODO return a WriteFuture-like thing
    void add( ByteBuffer[] buffers ) {
        SocketChannel channel = session.channel();

        synchronized( channel.blockingLock() ) {
            try {
                boolean empty = queue.isEmpty();

                if ( channel.isBlocking() ) {
                    if ( !empty ) {
                        process();
                    }

                    write( buffers );
                } else if ( empty ) {
                    // Shamelessly borrow this idea from Netty!
                    int cleared = write( buffers );

                    if ( cleared < buffers.length ) {
                        queue.addAll( Arrays.asList( buffers ).subList( cleared, buffers.length ) );
                        addToWriteSelector.publish( new Event( session ) );
                    }
                } else {
                    queue.addAll( Arrays.asList( buffers ) );
                }
            } catch( IOException e ) {
                failed.publish( new DataEvent<IOException>( session, e ) );
                // TODO return FAIL on the future, when that occurs
            }
        }
    }

    void process() throws IOException {
        written( write( writeQueue() ) );
    }

    private int write( ByteBuffer[] buffers ) throws IOException {
        SocketChannel channel = session.channel();
        long written;

        synchronized( channel.blockingLock() ) {
            // Do not change blocking mode while writing!
            written = channel.write( buffers );
        }

        int cleared = 0;

        for ( ByteBuffer buffer : buffers ) {
            if ( buffer.remaining() == 0 ) {
                cleared++;
            } else {
                break;
            }
        }

        logger.debug( "wrote {} bytes to {}", written, this );

        return cleared;
    }

    private ByteBuffer[] writeQueue() {
        synchronized( queue ) {
            return queue.toArray( new ByteBuffer[queue.size()] );
        }
    }

    private void written( int count ) {
        synchronized( queue ) {
            queue.subList( 0, count ).clear();

            // TODO thinking about ensuring that the queue's underlying list doesn't get too big. ArrayList.trimToSize

            if ( queue.isEmpty() ) {
                session.selectionKeys().cancel( SelectionOp.Write );
            }
        }
    }
}