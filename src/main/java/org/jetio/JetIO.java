package org.jetio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetio.lifecycle.Lifecycle;
import org.jetio.lifecycle.Startable;
import org.jetio.util.ExecutorBatchExecutor;
import org.jetio.util.MultiPublisher;
import org.jetio.util.ProducerThreadSubscription;
import org.jetio.util.WorkerThreadFactory;
import org.jetlang.channels.Channel;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.channels.Subscriber;
import org.jetlang.core.Disposable;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.PoolFiberFactory;

/**
 * Entry point to JetIO.
 *
 * JetIO is a low-latency socket server designed for passing messages of deterministic size
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class JetIO implements Disposable, Startable {
    private final Channel<Event> opened = new MemoryChannel<Event>();
    private final Channel<Event> closed = new MemoryChannel<Event>();

    private final Channel<DataEvent<IOException>> failed = new MemoryChannel<DataEvent<IOException>>();

    private final Channel<Event> readNext = new MemoryChannel<Event>();
    private final Channel<Event> readAgain = new MemoryChannel<Event>();

    private final Channel<DataEvent<Byte>> read = new MemoryChannel<DataEvent<Byte>>();

    private final Channel<Event> addToReadSelector = new MemoryChannel<Event>();
    private final Channel<Event> addToWriteSelector = new MemoryChannel<Event>();

    private final List<Lifecycle> components = new ArrayList<Lifecycle>();
    private final List<Fiber> fibers = new ArrayList<Fiber>();

    private final PoolFiberFactory fiberFactory;
    private final ExecutorService workers;

    public JetIO( MessageReader messageReader, Configuration config ) throws IOException {
        if ( null == messageReader ) {
            throw new IllegalArgumentException( "messageReader is required" );
        } else if ( null == config.getName() ) {
            throw new IllegalArgumentException( "configuration must specify a name" );
        }

        workers = Executors.newFixedThreadPool( config.getWorkerThreadCount(),
                                                new WorkerThreadFactory( config.getName(), config.getCounter() ) );
        fiberFactory = new PoolFiberFactory( workers );

        register( new Acceptor(
            config,
            new MultiPublisher<Event>( opened, config.isReadUponConnect() ? readNext : addToReadSelector ),
            new SessionFactory( addToWriteSelector, failed ) ) );

        addToReadSelector.subscribe( newFiber(), register( new ReadSelector( readNext, failed, config ) ) );

        CheckForReadReadiness reader = new CheckForReadReadiness( addToReadSelector, read, failed );
        readNext.subscribe( register( fiberFactory.create( new ExecutorBatchExecutor( workers ) ) ), reader );
        readAgain.subscribe( new ProducerThreadSubscription<Event>( newFiber(), reader ) );

        read.subscribe( new ProducerThreadSubscription<DataEvent<Byte>>(
            newFiber(),
            new ReadOneMessage( messageReader, readAgain, failed ) ) );

        addToWriteSelector.subscribe( newFiber(), register( new WriteSelector( failed, config ) ) );

        failed.subscribe( newFiber(), new DisconnectFailedSessions( closed ) );
    }

    @Override
    public void start() throws IOException {
        for ( Fiber fiber : fibers ) {
            fiber.start();
        }

        for ( Lifecycle component : components ) {
            component.start();
        }
    }

    @Override
    public void dispose() {
        for ( Fiber fiber : fibers ) {
            fiber.dispose();
        }

        for ( Lifecycle component : components ) {
            component.dispose();
        }

        fiberFactory.dispose();

        workers.shutdownNow();
    }

    /**
     * Channel that represents sessions that have been opened.
     *
     * @return {@link Subscriber} that can be used to subscribe to new session notifications
     */
    public Subscriber<Event> opened() {
        return opened;
    }

    /**
     * Channel that represents sessions that have been closed.
     *
     * @return {@link Subscriber} that can be used to subscribe to closed session notifications
     */
    public Subscriber<Event> closed() {
        return closed;
    }

    private Fiber newFiber() {
        return register( fiberFactory.create() );
    }

    private Fiber register( Fiber fiber ) {
        fibers.add( fiber );
        return fiber;
    }

    private <T extends Lifecycle> T register( T t ) {
        components.add( t );
        return t;
    }
}
