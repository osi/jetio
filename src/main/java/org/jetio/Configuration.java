package org.jetio;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration parameters for JetIO
 *
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class Configuration {
    private static final AtomicInteger COUNTER = new AtomicInteger( 0 );

    private String name;
    private int counter = COUNTER.getAndIncrement();
    private SocketAddress bindAddress;
    private int backlog;
    private long disposalWaitTime = TimeUnit.SECONDS.toMillis( 5 );
    private boolean readUponConnect;
    private int workerThreadCount = 100;
    private int bufferSlizeSize = 4096;
    private int bufferAllocationSize = 1048576;

    /**
     * Get the name of this instance
     *
     * @return Instance name
     */
    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Get the address to bind to
     *
     * @return Address to bind to
     */
    public SocketAddress getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress( SocketAddress bindAddress ) {
        this.bindAddress = bindAddress;
    }

    /**
     * Get the connection backlog for the accepting socket.
     *
     * @return Connection backlog. 0 is the system default
     */
    public int getBacklog() {
        return backlog;
    }

    public void setBacklog( int backlog ) {
        this.backlog = backlog;
    }

    /**
     * Get the number of milliseconds to wait for threads to terminate when disposing
     *
     * @return Number of milliseconds to wait
     */
    public long getDisposalWaitTime() {
        return disposalWaitTime;
    }

    public void setDisposalWaitTime( long disposalWaitTime ) {
        this.disposalWaitTime = disposalWaitTime;
    }

    /**
     * Should new connections be tried for a read immediately, or first go into a read selector
     *
     * @return True if new connections should be read immediately
     */
    public boolean isReadUponConnect() {
        return readUponConnect;
    }

    public void setReadUponConnect( boolean readUponConnect ) {
        this.readUponConnect = readUponConnect;
    }

    /**
     * Get the number of worker threads to use to process connections.
     *
     * @return Number of worker threads
     */
    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    public void setWorkerThreadCount( int workerThreadCount ) {
        this.workerThreadCount = workerThreadCount;
    }

    int getCounter() {
        return counter;
    }

    public int getBufferSlizeSize() {
        return bufferSlizeSize;
    }

    public void setBufferSlizeSize( int bufferSlizeSize ) {
        this.bufferSlizeSize = bufferSlizeSize;
    }

    public int getBufferAllocationSize() {
        return bufferAllocationSize;
    }

    public void setBufferAllocationSize( int bufferAllocationSize ) {
        this.bufferAllocationSize = bufferAllocationSize;
    }
}
