package com.google.protobuf.nano;

import com.google.protobuf.nano.ExtendableMessageNano;
import java.io.IOException;

public class Extension<M extends ExtendableMessageNano<M>, T> {
    protected final Class<T> clazz;

    /* access modifiers changed from: package-private */
    public int computeSerializedSize(Object obj) {
        throw null;
    }

    /* access modifiers changed from: package-private */
    public void writeTo(Object obj, CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
        throw null;
    }
}
