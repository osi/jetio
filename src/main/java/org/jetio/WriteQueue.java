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
    private final BufferSource buffers;
    private final Session session;

    WriteQueue( Session session,
                Publisher<Event> addToWriteSelector,
                Publisher<DataEvent<IOException>> failed,
                BufferSource buffers )
    {
        this.session = session;
        this.addToWriteSelector = addToWriteSelector;
        this.failed = failed;
        this.buffers = buffers;
    }

    // TODO return a WriteFuture-like thing
    void add( ByteBuffer[] buffers ) {
        try {
            if ( session.isBlocking() ) {
                if ( !queue.isEmpty() ) {
                    process();
                }

                write( buffers );

                this.buffers.release( Arrays.asList( buffers ) );
            } else {
                boolean empty;

                synchronized( queue ) {
                    empty = queue.isEmpty();
                    queue.addAll( Arrays.asList( buffers ) );
                }

                // If it was empty when we showed up, we're responsible for attempting to flush
                // and scheduling for later writing if we can't fully flush now
                if ( empty && !process() ) {
                    addToWriteSelector.publish( new Event( session ) );
                }
            }
        } catch( IOException e ) {
            failed.publish( new DataEvent<IOException>( session, e ) );
            // TODO return FAIL on the future, when that occurs
        }
    }

    synchronized boolean process() throws IOException {
        ByteBuffer[] buffers = writeQueue();

        if ( buffers.length == 0 ) {
            return true;
        }

        write( buffers );

        return releaseBuffers( countClearedBuffers( buffers ) );
    }

    private long write( ByteBuffer[] buffers ) throws IOException {
        SocketChannel channel = session.channel();
        long written;

        synchronized( channel.blockingLock() ) {
            // Do not change blocking mode while writing!
            written = channel.write( buffers );
        }

        logger.debug( "wrote {} bytes to {}", written, this );

        return written;
    }

    private int countClearedBuffers( ByteBuffer[] buffers ) {
        int cleared = 0;

        for ( ByteBuffer buffer : buffers ) {
            if ( buffer.remaining() == 0 ) {
                cleared++;
            } else {
                break;
            }
        }

        return cleared;
    }

    private ByteBuffer[] writeQueue() {
        synchronized( queue ) {
            return queue.toArray( new ByteBuffer[queue.size()] );
        }
    }

    /**
     * Remove written buffers from the queue
     *
     * @param count Number of buffers that were consumed
     *
     * @return True if the queue is now empty
     */
    private boolean releaseBuffers( int count ) {
        synchronized( queue ) {
            List<ByteBuffer> written = queue.subList( 0, count );

            this.buffers.release( written );

            written.clear();

            // TODO thinking about ensuring that the queue's underlying list doesn't get too big. ArrayList.trimToSize

            if ( queue.isEmpty() ) {
                session.selectionKeys().cancel( SelectionOp.Write );

                return true;
            }

            return false;
        }
    }
}
