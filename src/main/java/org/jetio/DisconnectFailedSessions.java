package org.jetio;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import org.jetlang.core.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for the failed sessions channel that logs and then closes sessions that have had an {@link IOException}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class DisconnectFailedSessions implements Callback<DataEvent<IOException>> {
    private static final Logger logger = LoggerFactory.getLogger( DisconnectFailedSessions.class );

    @Override
    public void onMessage( DataEvent<IOException> message ) {
        Session session = message.session();
        IOException exception = message.data();

        if ( !session.isClosed() ) {
            if ( exception instanceof EOFException || exception instanceof ClosedChannelException ) {
                logger.info( "closing " + session );
            } else {
                logger.info( "closing " + session + " due to", exception );
            }

            session.close();
        } else if ( logger.isTraceEnabled() ) {
            logger.info( "received exception after " + session + " was closed", exception );
        }
    }
}
