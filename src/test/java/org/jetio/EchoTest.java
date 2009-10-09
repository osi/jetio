package org.jetio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * @author Apache Mina Project (dev@mina.apache.org)
 * @version $Rev: $, $Date:  $
 */
public class EchoTest {
    private final Charset charset = Charset.forName( "UTF-8" );
    private ScheduledExecutorService scheduler;
    private JetIO io;
    private int port;

    public void start( StreamMessageReader reader ) throws IOException {
        port = 10748;

        Configuration config = new Configuration();

        config.setName( getClass().getSimpleName() );
        config.setBindAddress( new InetSocketAddress( "localhost", port ) );
        config.setReadUponConnect( true );

        io = new JetIO( new StreamMessageReaderAdapter( reader ), config );
        io.start();
    }

    private void echo( int count ) throws IOException {
        Socket socket = new Socket( "localhost", port );
        Writer out = new OutputStreamWriter( socket.getOutputStream(), "UTF-8" );
        BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream(), "UTF-8" ) );
        String s = "hello world";

        out.write( s );
        out.write( "\n" );
        for ( int i = 0; i < 4085; i++ ) {
            out.write( "\n" );
        }
        out.flush();

        for ( int i = 0; i < count; i++ ) {
            assertEquals( s, in.readLine() );
        }

        socket.close();
    }

    private void echo( StreamMessageReader reader, int count ) throws IOException {
        start( reader );
        echo( count );
    }

    @After
    public void dispose() {
        if ( null != io ) {
            io.dispose();
        }

        if ( null != scheduler ) {
            scheduler.shutdownNow();
        }
    }

    @Test //( timeout = 2000L )
    public void echoOnce() throws Exception {
        echo( new StreamMessageReader() {
            @Override
            public void readMessage( Session session, InputStream in ) throws IOException {
                System.out.println( in );
                System.out.println( in.available() );
                // TODO this (oddly) fails only when we may have read everything that there is to read already, which is strange.

                // It is guaranteed, however, that if a channel is in blocking mode and there is at least one byte
                // remaining in the buffer then this method will block until at least one byte is read.
                BufferedReader reader = new BufferedReader( new InputStreamReader( in, "UTF-8" ), 11 );

                session.write( charset.encode( reader.readLine() ) );
                session.write( ByteBuffer.wrap( new byte[]{ '\n' } ) );
            }
        },
              1 );
    }

    @Test //( timeout = 2000L )
    public void delayedEcho() throws Exception {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        final int count = 10;

        echo( new StreamMessageReader() {
            @Override
            public void readMessage( final Session session, InputStream in ) throws IOException {
                final String s = new BufferedReader( new InputStreamReader( in, "UTF-8" ) ).readLine();
                final AtomicInteger remaining = new AtomicInteger( count );

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ByteBuffer buffer = session.buffers().acquire();
                            buffer.put( charset.encode( s ) );
                            buffer.put( (byte) '\n' );
                            buffer.flip();

                            session.write( buffer );

                            if ( remaining.decrementAndGet() > 0 ) {
                                scheduler.schedule( this, 100, TimeUnit.MILLISECONDS );
                            }
                        } catch( Exception e ) {
                            e.printStackTrace();
                        }
                    }
                };

                runnable.run();
            }
        },
              count );
    }
}
