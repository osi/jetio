package org.jetio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetio.lifecycle.Lifecycle;
import org.jetlang.channels.Channel;
import org.jetlang.core.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage active connections for writeability in a {@link Selector}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class WriteSelector implements Callback<Event>, Runnable, Lifecycle {
    private static final Logger logger = LoggerFactory.getLogger( WriteSelector.class );

    private final List<Session> toAdd = Collections.synchronizedList( new ArrayList<Session>() );

    private final Channel<DataEvent<IOException>> failed;
    private final Configuration config;
    private final Thread thread;
    private final Selector selector;

    WriteSelector( Channel<DataEvent<IOException>> failed, Configuration config )
        throws IOException
    {
        this.failed = failed;
        this.config = config;

        this.selector = Selector.open();
        this.thread = new Thread( this, "write " + this.config.getName() + "[" + config.getCounter() + "]" );
    }

    @Override
    public void onMessage( Event event ) {
        Session session = event.session();

        logger.debug( "adding {} to write selector queue", session );

        toAdd.add( session );
        selector.wakeup();
    }

    private void addToSelector( Session session ) throws IOException {
        logger.debug( "adding {} to write selector", session );

        session.setNonBlocking();
        session.setWriteKey( session.channel().register( selector, SelectionKey.OP_WRITE, session ) );
    }

    @Override
    public void run() {
        while ( !Thread.interrupted() ) {
            try {
                selector.select();
            } catch( IOException e ) {
                logger.error( "Error while selecting for writes", e );
                break;
            }

            if ( Thread.interrupted() ) {
                logger.debug( "interrupted..." );
                break;
            }

            processAddQueue();

            for ( SelectionKey key : selector.selectedKeys() ) {
                write( (Session) key.attachment() );
            }
        }

        logger.debug( "Write selector thread exiting..." );
    }

    private void write( Session session ) {
        SocketChannel channel = session.channel();
        ByteBuffer[] buffers = session.writeQueue();
        long written;

        // TODO should we synchronize on the blocking lock? what if there is an attempt to change the blocking mode while we are writing?
        try {
            written = channel.write( buffers );
        } catch( IOException e ) {
            failed.publish( new DataEvent<IOException>( session, e ) );
            return;
        }

        int cleared = 0;

        for ( ByteBuffer buffer : buffers ) {
            if ( buffer.remaining() == 0 ) {
                cleared++;
            } else {
                break;
            }
        }

        session.written( cleared );

        logger.debug( "wrote {} bytes to {}", written, session );
    }

    private void processAddQueue() {
        synchronized( toAdd ) {
            for ( Session session : toAdd ) {
                try {
                    addToSelector( session );
                } catch( IOException e ) {
                    failed.publish( new DataEvent<IOException>( session, e ) );
                }
            }

            toAdd.clear();
        }
    }

    @Override
    public void start() throws IOException {
        thread.start();
    }

    @Override
    public void dispose() {
        thread.interrupt();

        try {
            thread.join( config.getDisposalWaitTime() );
        } catch( InterruptedException e ) {
            logger.error( "Interrupted while waiting for selector thread to complete", e );

            Thread.currentThread().interrupt();
        }
    }
}