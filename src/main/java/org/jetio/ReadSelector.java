package org.jetio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetio.lifecycle.Lifecycle;
import org.jetlang.channels.Channel;
import org.jetlang.core.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage active connections for readability in a {@link Selector}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class ReadSelector implements Callback<Event>, Runnable, Lifecycle {
    private static final Logger logger = LoggerFactory.getLogger( ReadSelector.class );

    private final List<Session> toAdd = Collections.synchronizedList( new ArrayList<Session>() );

    private final Channel<Event> readNext;
    private final Channel<DataEvent<IOException>> failed;
    private final Configuration config;
    private final Thread thread;
    private final Selector selector;

    ReadSelector( Channel<Event> readNext, Channel<DataEvent<IOException>> failed, Configuration config )
        throws IOException
    {
        this.readNext = readNext;
        this.failed = failed;
        this.config = config;

        this.selector = Selector.open();
        this.thread = new Thread( this, "read " + this.config.getName() + "[" + this.config.getCounter() + "]" );
    }

    @Override
    public void onMessage( Event event ) {
        Session session = event.session();

        logger.debug( "adding {} to read selector queue", session );

        toAdd.add( session );
        selector.wakeup();
    }

    private void addToSelector( Session session ) throws IOException {
        logger.debug( "adding {} to read selector", session );

        session.setNonBlocking();
        session.setReadKey( session.channel().register( selector, SelectionKey.OP_READ, session ) );
    }

    @Override
    public void run() {
        while ( !Thread.interrupted() ) {
            try {
                selector.select();
            } catch( IOException e ) {
                logger.error( "Error while selecting for reads", e );
                break;
            }

            if ( Thread.interrupted() ) {
                logger.debug( "interrupted..." );
                break;
            }

            processAddQueue();

            for ( SelectionKey key : selector.selectedKeys() ) {
                // Take the key out of the selector, since we will (likely) be handling reads blocking-style for now.
                // It is probably necessary to remove the key here, since we will do the read in another thread, and
                // do not want to end up firing two events for it. (In case this loop is fast enough)
                key.cancel();

                readNext.publish( new Event( (Session) key.attachment() ) );
            }
        }

        logger.debug( "Read selector thread exiting..." );
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
            logger.error( "Interrupted while waiting for selecter thread to complete", e );

            Thread.currentThread().interrupt();
        }
    }
}