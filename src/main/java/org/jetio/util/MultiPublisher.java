package org.jetio.util;

import org.jetlang.channels.Publisher;

/**
 * a {@link Publisher} that multicasts its messages to other publishers.
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class MultiPublisher<T> implements Publisher<T> {
    private final Publisher<T>[] publishers;

    public MultiPublisher( Publisher<T>... publishers ) {
        this.publishers = publishers;
    }

    @Override
    public void publish( T msg ) {
        for ( Publisher<T> publisher : publishers ) {
            publisher.publish( msg );
        }
    }
}
