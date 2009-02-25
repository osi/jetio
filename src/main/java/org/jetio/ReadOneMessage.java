package org.jetio;

import java.io.IOException;

import org.jetlang.channels.Channel;
import org.jetlang.channels.Publisher;
import org.jetlang.core.Callback;

/**
 * Invoke a {@link MessageReader} to read a single message, then spin it back around to possibly read again.
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class ReadOneMessage implements Callback<DataEvent<Byte>> {
    private final MessageReader messageReader;
    private final Publisher<Event> readAgain;
    private final Channel<DataEvent<IOException>> failed;

    ReadOneMessage( MessageReader messageReader, Publisher<Event> readAgain, Channel<DataEvent<IOException>> failed ) {
        this.messageReader = messageReader;
        this.readAgain = readAgain;
        this.failed = failed;
    }

    @Override
    public void onMessage( DataEvent<Byte> message ) {
        Session session = message.session();

        try {
            messageReader.readMessage( session, message.data() );
        } catch( IOException e ) {
            failed.publish( new DataEvent<IOException>( session, e ) );

            return;
        }

        readAgain.publish( new Event( session ) );
    }
}
