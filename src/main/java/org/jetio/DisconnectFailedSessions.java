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

    @Override
    public void onMessage( DataEvent<IOException> message ) {
        Session session = message.session();

        logger.info( "closing " + session + " due to", message.data() );

        session.close();
    }
}
