package org.jetio.lifecycle;

/**
 * Components that need to be disposed when they are done being useful
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public interface Disposable {
    void dispose();
}
