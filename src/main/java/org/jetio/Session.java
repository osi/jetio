package org.jetio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    private final List<ByteBuffer> toWrite = Collections.synchronizedList( new ArrayList<ByteBuffer>() );
    private final SocketChannel channel;
    private final Publisher<Event> addToWriteSelector;
    private final Publisher<DataEvent<IOException>> failed;

    Session( SocketChannel channel, Publisher<Event> addToWriteSelector, Publisher<DataEvent<IOException>> failed ) {
        this.channel = channel;
        this.addToWriteSelector = addToWriteSelector;
        this.failed = failed;

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
     * @param buffer {@link ByteBuffer} containing message to write
     */
    public void write( ByteBuffer buffer ) {
        // TODO return a WriteFuture-like thing
        // TODO only "write right now"
        synchronized( channel.blockingLock() ) {
            if ( channel.isBlocking() ) {
                // TODO what if there is stuff in the queue and we've cancelled the key?
                try {
                    channel.write( buffer );
                } catch( IOException e ) {
                    failed.publish( new DataEvent<IOException>( this, e ) );

                    // TODO return FAIL on the future
                }
            } else {
                boolean empty = toWrite.isEmpty();

                toWrite.add( buffer );

                if ( empty ) {
                    addToWriteSelector.publish( new Event( this ) );
                }
            }
        }
    }

    ByteBuffer[] writeQueue() {
        synchronized( toWrite ) {
            return toWrite.toArray( new ByteBuffer[toWrite.size()] );
        }
    }

    void written( int count ) {
        synchronized( toWrite ) {
            toWrite.subList( 0, count ).clear();

            if ( toWrite.isEmpty() ) {
                cancelKey( keys.get( SelectionOp.Write ) );
            }
        }
    }
}
