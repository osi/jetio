package org.jetio.lifecycle;

import java.io.IOException;

/**
 * Components that need to be started prior to being useful
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public interface Startable {
    void start() throws IOException;
}
