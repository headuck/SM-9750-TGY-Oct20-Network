package android.net;

import android.content.Context;

public class NetworkStackIpMemoryStore extends IpMemoryStoreClient {
    private final IIpMemoryStore mService;

    public NetworkStackIpMemoryStore(Context context, IIpMemoryStore iIpMemoryStore) {
        super(context);
        this.mService = iIpMemoryStore;
    }
}
