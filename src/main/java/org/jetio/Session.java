package org.jetio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    private final Map<SelectionOp, AtomicReference<SelectionKey>> keys =
        new EnumMap<SelectionOp, AtomicReference<SelectionKey>>( SelectionOp.class );
    private final ConcurrentMap<Object, Object> properties = new ConcurrentHashMap<Object, Object>();
    private final AtomicBoolean sentClosedEvent = new AtomicBoolean( false );

    private final WriteQueue writeQueue;
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

        for ( SelectionOp op : SelectionOp.values() ) {
            keys.put( op, new AtomicReference<SelectionKey>() );
        }
    }

    /**
     * Get the underlyng {@link SocketChannel} for this session. Handle with care.
     *
     * @return Underlying SocketChannel
     */
    public SocketChannel channel() {
        return channel;
    }

    void setKey( SelectionOp op, SelectionKey key ) {
        if ( !keys.get( op ).compareAndSet( null, key ) ) {
            throw new IllegalStateException( "already have a " + op + " key for " + this );
        }
    }

    void cancelKeys() {
        for ( AtomicReference<SelectionKey> reference : keys.values() ) {
            cancelKey( reference );
        }
    }

    void cancelWriteKey() {
        cancelKey( keys.get( SelectionOp.Write ) );
    }

    private void cancelKey( AtomicReference<SelectionKey> reference ) {
        SelectionKey key = reference.getAndSet( null );

        if ( null != key ) {
            key.cancel();
        }
    }

    void setBlocking() throws IOException {
        cancelKeys();

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
            cancelKeys();

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
