package org.jetio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetlang.channels.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A session represents an active connection
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class Session {
    private static final Logger logger = LoggerFactory.getLogger( Session.class );

    private final ConcurrentMap<Object, Object> properties = new ConcurrentHashMap<Object, Object>();
    private final AtomicBoolean sentClosedEvent = new AtomicBoolean( false );

    private final WriteQueue writeQueue;
    private final SelectionKeys selectionKeys;
    private final SocketChannel channel;
    private final Publisher<Event> closed;
    private final BufferSource buffers;
    /**
     * Store our own copy of the blocking status of the channel,
     * as checking it on the Channel requires acquiring a lock
     */
    private volatile boolean blocking;

    Session( SocketChannel channel,
             Publisher<Event> addToWriteSelector,
             Publisher<DataEvent<IOException>> failed,
             Publisher<Event> closed,
             BufferSource buffers )
    {
        this.channel = channel;
        this.closed = closed;
        this.buffers = buffers;
        this.writeQueue = new WriteQueue( this, addToWriteSelector, failed, buffers );
        this.selectionKeys = new SelectionKeys( this );
        this.blocking = channel.isBlocking();
    }

    /**
     * Get the underlyng {@link SocketChannel} for this session. Handle with care.
     *
     * @return Underlying SocketChannel
     */
    public SocketChannel channel() {
        return channel;
    }

    SelectionKeys selectionKeys() {
        return selectionKeys;
    }

    boolean isBlocking() {
        return blocking;
    }

    void setBlocking() throws IOException {
        selectionKeys.cancel();

        if ( !configureBlocking( true ) ) {
            logger.debug( "{} switched to blocking mode", this );
        }
    }

    private boolean configureBlocking( boolean state ) throws IOException {
        boolean before;

        synchronized( channel.blockingLock() ) {
            before = channel.isBlocking();

            channel.configureBlocking( state );

            blocking = state;
        }

        return before;
    }

    void setNonBlocking() throws IOException {
        if ( configureBlocking( false ) ) {
            logger.debug( "{} switched to non-blocking mode", this );
        }
    }

    /**
     * Write a message to this session.
     *
     * If the session is in blocking mode, it will be written immediately.
     *
     * If the session is in non-blocking mode, and there are no pending writes, it will attempt to write what it can
     * immediately, and queue the rest.
     *
     * Otherwise, data is added to the write queue.
     *
     * @param buffers {@link ByteBuffer}s containing messages to write
     */
    public void write( ByteBuffer... buffers ) {
        writeQueue.add( buffers );
    }

    void processWriteQueue() throws IOException {
        writeQueue.process();
    }

    /**
     * Check to see if this session is closed
     *
     * @return True if the session is closed
     */
    public boolean isClosed() {
        return sentClosedEvent.get();
    }

    /**
     * Close this session
     *
     * First call will close it, subsequent calls have no effect.
     */
    public void close() {
        if ( sentClosedEvent.compareAndSet( false, true ) ) {
            selectionKeys.cancel();

            try {
                channel.close();
            } catch( IOException e ) {
                logger.info( "Exception closing " + this, e );
            }

            closed.publish( new Event( this ) );
        }
    }

    /**
     * Get the {@link ConcurrentMap} of user-defined properties associated with this session
     *
     * @return The map of user-defined properties
     */
    public ConcurrentMap<Object, Object> properties() {
        return properties;
    }

    /**
     * Get the source of <em>write</em> buffers associated with this session.
     *
     * @return {@link BufferSource} for this session
     */
    public BufferSource buffers() {
        return buffers;
    }
}
