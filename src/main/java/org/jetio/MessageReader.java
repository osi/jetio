package org.jetio;

import java.io.IOException;

/**
 * Component that reads single messages off of the session.
 *
 * This class is the main class a user has to provide. It is responsible for reading at least a
 * <b>SINGLE MESSAGE</b> off of the session. If the provided data spans multiple messages, it should
 * read all messages that are contained.
 *
 * It should be re-entrant and <b>WILL</b> be called by multiple threads.
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public interface MessageReader {

    void readMessage( Session session, byte[] initialData ) throws IOException;
}