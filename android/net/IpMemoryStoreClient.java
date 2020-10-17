package android.net;

import android.content.Context;

public abstract class IpMemoryStoreClient {
    private final Context mContext;

    public IpMemoryStoreClient(Context context) {
        if (context != null) {
            this.mContext = context;
            return;
        }
        throw new IllegalArgumentException("missing context");
    }
}
