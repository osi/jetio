package org.jetio;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

/** @author <a href="mailto:peter.royal@pobox.com">peter royal</a> */
class SessionBufferSource implements BufferSource {
    private final Queue<ByteBuffer> queue;
    private final BufferSource source;

    SessionBufferSource( BufferSource source ) {
        this.source = source;
        this.queue = new ArrayDeque<ByteBuffer>();
    }

    @Override
    public ByteBuffer acquire() {
        ByteBuffer buffer;

        synchronized( queue ) {
            buffer = queue.poll();
        }

        if ( null == buffer ) {
            buffer = source.acquire();
        }

        buffer.clear();

        return buffer;
    }

    void release() {
        synchronized( queue ) {
            source.release( queue );
            queue.clear();
        }
    }

    @Override
    public void release( Collection<ByteBuffer> buffers ) {
        synchronized( queue ) {
            queue.addAll( buffers );
        }
    }
}
