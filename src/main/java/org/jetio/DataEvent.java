package org.jetio;

/**
 * An {@link Event} that carries an additional payload
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class DataEvent<T> extends Event {
    private final T data;

    public DataEvent( Session session, T data ) {
        super( session );
        this.data = data;
    }

    public T data() {
        return data;
    }
}
