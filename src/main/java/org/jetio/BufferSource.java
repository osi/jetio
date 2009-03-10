package org.jetio;

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * A place to get {@link ByteBuffer} instances from
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public interface BufferSource {
    ByteBuffer acquire();

    void release( Collection<ByteBuffer> buffers );
}
