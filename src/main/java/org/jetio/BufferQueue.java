package org.jetio;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A queue for managing slices of a direct {@link ByteBuffer}
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class BufferQueue implements BufferSource {
    private static final Logger logger = LoggerFactory.getLogger( BufferQueue.class );

    private final BlockingQueue<ByteBuffer> available = new LinkedBlockingQueue<ByteBuffer>();
    private final Object createLock = new Object();

    private final int sliceSize;
    private final int allocationSize;
    private final int slices;

    BufferQueue( int sliceSize, int allocationSize ) {
        if ( allocationSize % sliceSize != 0 ) {
            throw new IllegalArgumentException( "Allocation size is not an even multiple of slice size" );
        }

        this.sliceSize = sliceSize;
        this.allocationSize = allocationSize;
        this.slices = allocationSize / sliceSize;
    }

    @Override
    public ByteBuffer acquire() {
        ByteBuffer buffer = available.poll();

        if ( null != buffer ) {
            logger.debug( "acquired buffer from queue" );

            return buffer;
        }

        synchronized( createLock ) {
            while ( true ) {
                buffer = available.poll();

                if ( null != buffer ) {
                    logger.debug( "acquired buffer while in create lock" );

                    return buffer;
                }

                logger.debug( "creating more buffers" );

                ByteBuffer directBuffer = ByteBuffer.allocateDirect( allocationSize );

                for ( int i = 0; i < slices; i++ ) {
                    directBuffer.position( sliceSize * i );
                    directBuffer.limit( directBuffer.position() + sliceSize );

                    available.add( directBuffer.slice() );
                }
            }
        }
    }

    @Override
    public void release( Collection<ByteBuffer> buffers ) {
        available.addAll( buffers );
    }
}
