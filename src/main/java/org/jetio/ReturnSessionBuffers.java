package org.jetio;

import org.jetlang.core.Callback;

/** @author <a href="mailto:peter.royal@pobox.com">peter royal</a> */
class ReturnSessionBuffers implements Callback<Event> {
    @Override
    public void onMessage( Event message ) {
        ( (SessionBufferSource) message.session().buffers() ).release();
    }
}
