package org.jetio;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.jetio.lifecycle.Lifecycle;
import org.jetlang.channels.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accept new connections
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class Acceptor implements Runnable, Lifecycle {
    private static final Logger logger = LoggerFactory.getLogger( Acceptor.class );

    private final Configuration config;
    private final SessionFactory sessionFactory;
    private final Thread thread;
    private final ServerSocketChannel ssc;
    private final Publisher<Event> newConnections;

    Acceptor( Configuration config, Publisher<Event> newConnections, SessionFactory sessionFactory )
        throws IOException
    {
        if ( null == config.getBindAddress() ) {
            throw new IllegalArgumentException( "configuration must specify a bindAddress" );
        }

        this.config = config;
        this.newConnections = newConnections;
        this.sessionFactory = sessionFactory;

        this.ssc = ServerSocketChannel.open();
        this.thread = new Thread( this, "accept " + this.config.getName() + "-" + config.getCounter() );
    }

    @Override
    public void start() throws IOException {
        ssc.socket().bind( config.getBindAddress(), config.getBacklog() );

        thread.start();
    }

    @Override
    public void dispose() {
        // TODO when we do dispose, all close sessions need to shut down as well
        thread.interrupt();

        try {
            thread.join( config.getDisposalWaitTime() );
        } catch( InterruptedException e ) {
            logger.error( "Interrupted while waiting for acceptor thread to complete", e );

            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        try {
            while ( !Thread.interrupted() ) {
                try {
                    SocketChannel channel = ssc.accept();

                    logger.debug( "accepted {}", channel );

                    newConnections.publish( new Event( sessionFactory.create( channel ) ) );
                } catch( ClosedByInterruptException e ) {
                    logger.debug( "Thread interrupted, exiting", e );
                }
            }
        } catch( IOException e ) {
            logger.error( "Exception accepting new connection", e );
        }
    }
}
