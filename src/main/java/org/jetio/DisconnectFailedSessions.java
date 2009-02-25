package org.jetio;

import java.io.IOException;

import org.jetlang.channels.Publisher;
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

    private final Publisher<Event> closed;

    DisconnectFailedSessions( Publisher<Event> closed ) {
        this.closed = closed;
    }

    @Override
    public void onMessage( DataEvent<IOException> message ) {
        Session session = message.session();

        logger.info( "closing " + session, message.data() );

        session.cancelKeys();

        try {
            session.channel().close();
        } catch( IOException e ) {
            logger.info( "Exception closing " + session, e );
        }

        closed.publish( new Event( session ) );
    }
}
