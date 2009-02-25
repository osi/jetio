package org.jetio;

/**
 * Generic event involving a {@link Session}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class Event {
    private final Session session;

    public Event( Session session ) {
        this.session = session;
    }

    public Session session() {
        return session;
    }
}
