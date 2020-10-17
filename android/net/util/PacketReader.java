package android.net.util;

import android.os.Handler;
import android.system.Os;
import java.io.FileDescriptor;

public abstract class PacketReader extends FdEventsReader<byte[]> {
    protected PacketReader(Handler handler, int i) {
        super(handler, new byte[Math.max(i, 2048)]);
    }

    /* access modifiers changed from: protected */
    public int readPacket(FileDescriptor fileDescriptor, byte[] bArr) throws Exception {
        return Os.read(fileDescriptor, bArr, 0, bArr.length);
    }
}
