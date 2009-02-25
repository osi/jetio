package org.jetio;

import java.io.IOException;
import java.io.InputStream;

/**
 * Read a single message using the stream paradigm
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public interface StreamMessageReader {

    void readMessage( Session session, InputStream in ) throws IOException;
}