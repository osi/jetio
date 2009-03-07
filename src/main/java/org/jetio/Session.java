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

    Session( SocketChannel channel,
             Publisher<Event> addToWriteSelector,
             Publisher<DataEvent<IOException>> failed,
             Publisher<Event> closed )
    {
        this.channel = channel;
        this.closed = closed;
        this.writeQueue = new WriteQueue( this, addToWriteSelector, failed );
        this.selectionKeys = new SelectionKeys( this );
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

    void setBlocking() throws IOException {
        selectionKeys.cancel();

        boolean before = channel.isBlocking();

        channel.configureBlocking( true );

        if ( !before ) {
            logger.debug( "{} switched to blocking mode", this );
        }
    }

    void setNonBlocking() throws IOException {
        boolean before = channel.isBlocking();

        channel.configureBlocking( false );

        if ( before ) {
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

    public boolean isClosed() {
        return sentClosedEvent.get();
    }

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

    public ConcurrentMap<Object, Object> properties() {
        return properties;
    }
}
