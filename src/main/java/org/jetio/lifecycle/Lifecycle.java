package org.jetio.lifecycle;

/**
 * Simple aggregation of {@link Startable} and {@link Disposable}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public interface Lifecycle extends Startable, Disposable {
}
