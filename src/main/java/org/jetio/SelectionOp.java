package org.jetio;

import java.nio.channels.SelectionKey;

/** @author <a href="mailto:peter.royal@pobox.com">peter royal</a> */
enum SelectionOp {
    Read( SelectionKey.OP_READ ),
    Write( SelectionKey.OP_WRITE );

    private final int op;
    private final String name = name().toLowerCase();

    SelectionOp( int op ) {
        this.op = op;
    }

    int op() {
        return op;
    }

    @Override
    public String toString() {
        return name;
    }
}
