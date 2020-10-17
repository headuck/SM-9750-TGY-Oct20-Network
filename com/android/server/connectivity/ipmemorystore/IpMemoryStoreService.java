package com.android.server.connectivity.ipmemorystore;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.IIpMemoryStore;
import android.net.ipmemorystore.Blob;
import android.net.ipmemorystore.IOnBlobRetrievedListener;
import android.net.ipmemorystore.IOnL2KeyResponseListener;
import android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener;
import android.net.ipmemorystore.IOnSameL3NetworkResponseListener;
import android.net.ipmemorystore.IOnStatusListener;
import android.net.ipmemorystore.NetworkAttributes;
import android.net.ipmemorystore.NetworkAttributesParcelable;
import android.net.ipmemorystore.SameL3NetworkResponse;
import android.net.ipmemorystore.Status;
import android.net.ipmemorystore.StatusParcelable;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.connectivity.ipmemorystore.IpMemoryStoreDatabase;
import com.android.server.connectivity.ipmemorystore.RegularMaintenanceJobService;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IpMemoryStoreService extends IIpMemoryStore.Stub {
    private static final String TAG = "IpMemoryStoreService";
    final Context mContext;
    final SQLiteDatabase mDb;
    final ExecutorService mExecutor;

    /* access modifiers changed from: protected */
    public int getDbSizeThreshold() {
        return 10485760;
    }

    @Override // android.net.IIpMemoryStore
    public int getInterfaceVersion() {
        return 3;
    }

    public IpMemoryStoreService(Context context) {
        this.mContext = context;
        IpMemoryStoreDatabase.DbHelper dbHelper = new IpMemoryStoreDatabase.DbHelper(context);
        SQLiteDatabase sQLiteDatabase = null;
        try {
            SQLiteDatabase writableDatabase = dbHelper.getWritableDatabase();
            if (writableDatabase == null) {
                Log.e(TAG, "Unexpected null return of getWriteableDatabase");
            }
            sQLiteDatabase = writableDatabase;
        } catch (SQLException e) {
            Log.e(TAG, "Can't open the Ip Memory Store database", e);
        } catch (Exception e2) {
            Log.wtf(TAG, "Impossible exception Ip Memory Store database", e2);
        }
        this.mDb = sQLiteDatabase;
        this.mExecutor = Executors.newSingleThreadExecutor();
        RegularMaintenanceJobService.schedule(this.mContext, this);
    }

    private StatusParcelable makeStatus(int i) {
        return new Status(i).toParcelable();
    }

    @Override // android.net.IIpMemoryStore
    public void storeNetworkAttributes(String str, NetworkAttributesParcelable networkAttributesParcelable, IOnStatusListener iOnStatusListener) {
        this.mExecutor.execute(new Runnable(str, networkAttributesParcelable == null ? null : new NetworkAttributes(networkAttributesParcelable), iOnStatusListener) {
            /* class com.android.server.connectivity.ipmemorystore.$$Lambda$IpMemoryStoreService$D__o7MlN7G4Ah9N9Rd76bfswx_s */
            private final /* synthetic */ String f$1;
            private final /* synthetic */ NetworkAttributes f$2;
            private final /* synthetic */ IOnStatusListener f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void run() {
                IpMemoryStoreService.this.lambda$storeNetworkAttributes$0$IpMemoryStoreService(this.f$1, this.f$2, this.f$3);
            }
        });
    }

    public /* synthetic */ void lambda$storeNetworkAttributes$0$IpMemoryStoreService(String str, NetworkAttributes networkAttributes, IOnStatusListener iOnStatusListener) {
        try {
            int storeNetworkAttributesAndBlobSync = storeNetworkAttributesAndBlobSync(str, networkAttributes, null, null, null);
            if (iOnStatusListener != null) {
                iOnStatusListener.onComplete(makeStatus(storeNetworkAttributesAndBlobSync));
            }
        } catch (RemoteException unused) {
        }
    }

    @Override // android.net.IIpMemoryStore
    public void storeBlob(String str, String str2, String str3, Blob blob, IOnStatusListener iOnStatusListener) {
        this.mExecutor.execute(new Runnable(str, str2, str3, blob == null ? null : blob.data, iOnStatusListener) {
            /* class com.android.server.connectivity.ipmemorystore.$$Lambda$IpMemoryStoreService$g37dRFQvkn5qEnGTEimTUj8WdfQ */
            private final /* synthetic */ String f$1;
            private final /* synthetic */ String f$2;
            private final /* synthetic */ String f$3;
            private final /* synthetic */ byte[] f$4;
            private final /* synthetic */ IOnStatusListener f$5;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
                this.f$5 = r6;
            }

            public final void run() {
                IpMemoryStoreService.this.lambda$storeBlob$1$IpMemoryStoreService(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5);
            }
        });
    }

    public /* synthetic */ void lambda$storeBlob$1$IpMemoryStoreService(String str, String str2, String str3, byte[] bArr, IOnStatusListener iOnStatusListener) {
        try {
            int storeNetworkAttributesAndBlobSync = storeNetworkAttributesAndBlobSync(str, null, str2, str3, bArr);
            if (iOnStatusListener != null) {
                iOnStatusListener.onComplete(makeStatus(storeNetworkAttributesAndBlobSync));
            }
        } catch (RemoteException unused) {
        }
    }

    private int storeNetworkAttributesAndBlobSync(String str, NetworkAttributes networkAttributes, String str2, String str3, byte[] bArr) {
        if (str == null) {
            return -2;
        }
        if (networkAttributes == null && bArr == null) {
            return -2;
        }
        if (bArr != null && (str2 == null || str3 == null)) {
            return -2;
        }
        SQLiteDatabase sQLiteDatabase = this.mDb;
        if (sQLiteDatabase == null) {
            return -3;
        }
        try {
            long expiry = IpMemoryStoreDatabase.getExpiry(sQLiteDatabase, str);
            if (expiry == -1) {
                expiry = System.currentTimeMillis();
            }
            int storeNetworkAttributes = IpMemoryStoreDatabase.storeNetworkAttributes(this.mDb, str, RelevanceUtils.bumpExpiryDate(expiry), networkAttributes);
            if (bArr == null) {
                return storeNetworkAttributes;
            }
            return IpMemoryStoreDatabase.storeBlob(this.mDb, str, str2, str3, bArr);
        } catch (Exception e) {
            String str4 = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Exception while storing for key {");
            sb.append(str);
            sb.append("} ; NetworkAttributes {");
            String str5 = "null";
            if (networkAttributes == null) {
                networkAttributes = str5;
            }
            sb.append(networkAttributes);
            sb.append("} ; clientId {");
            if (str2 == null) {
                str2 = str5;
            }
            sb.append(str2);
            sb.append("} ; name {");
            if (str3 != null) {
                str5 = str3;
            }
            sb.append(str5);
            sb.append("} ; data {");
            sb.append(Utils.byteArrayToString(bArr));
            sb.append("}");
            Log.e(str4, sb.toString(), e);
            return -1;
        }
    }

    @Override // android.net.IIpMemoryStore
    public void findL2Key(NetworkAttributesParcelable networkAttributesParcelable, IOnL2KeyResponseListener iOnL2KeyResponseListener) {
        if (iOnL2KeyResponseListener != null) {
            this.mExecutor.execute(new Runnable(networkAttributesParcelable, iOnL2KeyResponseListener) {
                /* class com.android.server.connectivity.ipmemorystore.$$Lambda$IpMemoryStoreService$uTiEAFhCJuFbV05gJVEPnOqKxRM */
                private final /* synthetic */ NetworkAttributesParcelable f$1;
                private final /* synthetic */ IOnL2KeyResponseListener f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    IpMemoryStoreService.this.lambda$findL2Key$2$IpMemoryStoreService(this.f$1, this.f$2);
                }
            });
        }
    }

    public /* synthetic */ void lambda$findL2Key$2$IpMemoryStoreService(NetworkAttributesParcelable networkAttributesParcelable, IOnL2KeyResponseListener iOnL2KeyResponseListener) {
        if (networkAttributesParcelable == null) {
            try {
                iOnL2KeyResponseListener.onL2KeyResponse(makeStatus(-2), null);
            } catch (RemoteException unused) {
            }
        } else if (this.mDb == null) {
            iOnL2KeyResponseListener.onL2KeyResponse(makeStatus(-2), null);
        } else {
            iOnL2KeyResponseListener.onL2KeyResponse(makeStatus(0), IpMemoryStoreDatabase.findClosestAttributes(this.mDb, new NetworkAttributes(networkAttributesParcelable)));
        }
    }

    @Override // android.net.IIpMemoryStore
    public void isSameNetwork(String str, String str2, IOnSameL3NetworkResponseListener iOnSameL3NetworkResponseListener) {
        if (iOnSameL3NetworkResponseListener != null) {
            this.mExecutor.execute(new Runnable(str, str2, iOnSameL3NetworkResponseListener) {
                /* class com.android.server.connectivity.ipmemorystore.$$Lambda$IpMemoryStoreService$r8E_mM1Sg3uIrSkqsaZn21YDRJ0 */
                private final /* synthetic */ String f$1;
                private final /* synthetic */ String f$2;
                private final /* synthetic */ IOnSameL3NetworkResponseListener f$3;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                }

                public final void run() {
                    IpMemoryStoreService.this.lambda$isSameNetwork$3$IpMemoryStoreService(this.f$1, this.f$2, this.f$3);
                }
            });
        }
    }

    public /* synthetic */ void lambda$isSameNetwork$3$IpMemoryStoreService(String str, String str2, IOnSameL3NetworkResponseListener iOnSameL3NetworkResponseListener) {
        if (str == null || str2 == null) {
            iOnSameL3NetworkResponseListener.onSameL3NetworkResponse(makeStatus(-2), null);
            return;
        }
        try {
            if (this.mDb == null) {
                iOnSameL3NetworkResponseListener.onSameL3NetworkResponse(makeStatus(-2), null);
                return;
            }
            try {
                NetworkAttributes retrieveNetworkAttributes = IpMemoryStoreDatabase.retrieveNetworkAttributes(this.mDb, str);
                NetworkAttributes retrieveNetworkAttributes2 = IpMemoryStoreDatabase.retrieveNetworkAttributes(this.mDb, str2);
                if (retrieveNetworkAttributes != null) {
                    if (retrieveNetworkAttributes2 != null) {
                        iOnSameL3NetworkResponseListener.onSameL3NetworkResponse(makeStatus(0), new SameL3NetworkResponse(str, str2, retrieveNetworkAttributes.getNetworkGroupSamenessConfidence(retrieveNetworkAttributes2)).toParcelable());
                        return;
                    }
                }
                iOnSameL3NetworkResponseListener.onSameL3NetworkResponse(makeStatus(0), new SameL3NetworkResponse(str, str2, -1.0f).toParcelable());
            } catch (Exception unused) {
                iOnSameL3NetworkResponseListener.onSameL3NetworkResponse(makeStatus(-1), null);
            }
        } catch (RemoteException unused2) {
        }
    }

    @Override // android.net.IIpMemoryStore
    public void retrieveNetworkAttributes(String str, IOnNetworkAttributesRetrievedListener iOnNetworkAttributesRetrievedListener) {
        if (iOnNetworkAttributesRetrievedListener != null) {
            this.mExecutor.execute(new Runnable(str, iOnNetworkAttributesRetrievedListener) {
                /* class com.android.server.connectivity.ipmemorystore.$$Lambda$IpMemoryStoreService$VAgKYLJkveVgUJIbqtwqZy9r5Nk */
                private final /* synthetic */ String f$1;
                private final /* synthetic */ IOnNetworkAttributesRetrievedListener f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    IpMemoryStoreService.this.lambda$retrieveNetworkAttributes$4$IpMemoryStoreService(this.f$1, this.f$2);
                }
            });
        }
    }

    public /* synthetic */ void lambda$retrieveNetworkAttributes$4$IpMemoryStoreService(String str, IOnNetworkAttributesRetrievedListener iOnNetworkAttributesRetrievedListener) {
        NetworkAttributesParcelable networkAttributesParcelable;
        if (str == null) {
            try {
                iOnNetworkAttributesRetrievedListener.onNetworkAttributesRetrieved(makeStatus(-2), str, null);
            } catch (RemoteException unused) {
            }
        } else if (this.mDb == null) {
            iOnNetworkAttributesRetrievedListener.onNetworkAttributesRetrieved(makeStatus(-3), str, null);
        } else {
            try {
                NetworkAttributes retrieveNetworkAttributes = IpMemoryStoreDatabase.retrieveNetworkAttributes(this.mDb, str);
                StatusParcelable makeStatus = makeStatus(0);
                if (retrieveNetworkAttributes == null) {
                    networkAttributesParcelable = null;
                } else {
                    networkAttributesParcelable = retrieveNetworkAttributes.toParcelable();
                }
                iOnNetworkAttributesRetrievedListener.onNetworkAttributesRetrieved(makeStatus, str, networkAttributesParcelable);
            } catch (Exception unused2) {
                iOnNetworkAttributesRetrievedListener.onNetworkAttributesRetrieved(makeStatus(-1), str, null);
            }
        }
    }

    @Override // android.net.IIpMemoryStore
    public void retrieveBlob(String str, String str2, String str3, IOnBlobRetrievedListener iOnBlobRetrievedListener) {
        if (iOnBlobRetrievedListener != null) {
            this.mExecutor.execute(new Runnable(str, iOnBlobRetrievedListener, str3, str2) {
                /* class com.android.server.connectivity.ipmemorystore.$$Lambda$IpMemoryStoreService$2zXvadnWYjrsixRPJ8GeHSEmox4 */
                private final /* synthetic */ String f$1;
                private final /* synthetic */ IOnBlobRetrievedListener f$2;
                private final /* synthetic */ String f$3;
                private final /* synthetic */ String f$4;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                }

                public final void run() {
                    IpMemoryStoreService.this.lambda$retrieveBlob$5$IpMemoryStoreService(this.f$1, this.f$2, this.f$3, this.f$4);
                }
            });
        }
    }

    public /* synthetic */ void lambda$retrieveBlob$5$IpMemoryStoreService(String str, IOnBlobRetrievedListener iOnBlobRetrievedListener, String str2, String str3) {
        if (str == null) {
            try {
                iOnBlobRetrievedListener.onBlobRetrieved(makeStatus(-2), str, str2, null);
            } catch (RemoteException unused) {
            }
        } else if (this.mDb == null) {
            iOnBlobRetrievedListener.onBlobRetrieved(makeStatus(-3), str, str2, null);
        } else {
            try {
                Blob blob = new Blob();
                blob.data = IpMemoryStoreDatabase.retrieveBlob(this.mDb, str, str3, str2);
                iOnBlobRetrievedListener.onBlobRetrieved(makeStatus(0), str, str2, blob);
            } catch (Exception unused2) {
                iOnBlobRetrievedListener.onBlobRetrieved(makeStatus(-1), str, str2, null);
            }
        }
    }

    @Override // android.net.IIpMemoryStore
    public void factoryReset() {
        this.mExecutor.execute(new Runnable() {
            /* class com.android.server.connectivity.ipmemorystore.$$Lambda$IpMemoryStoreService$UaGqWMfOWxdnYWKS4ge4zAZ4wU */

            public final void run() {
                IpMemoryStoreService.this.lambda$factoryReset$6$IpMemoryStoreService();
            }
        });
    }

    public /* synthetic */ void lambda$factoryReset$6$IpMemoryStoreService() {
        IpMemoryStoreDatabase.wipeDataUponNetworkReset(this.mDb);
    }

    private long getDbSize() {
        try {
            return new File(this.mDb.getPath()).length();
        } catch (SecurityException e) {
            Log.e(TAG, "Read db size access deny.", e);
            return 0;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isDbSizeOverThreshold() {
        return getDbSize() > ((long) getDbSizeThreshold());
    }

    /* access modifiers changed from: package-private */
    public void fullMaintenance(IOnStatusListener iOnStatusListener, RegularMaintenanceJobService.InterruptMaintenance interruptMaintenance) {
        this.mExecutor.execute(new Runnable(iOnStatusListener, interruptMaintenance) {
            /* class com.android.server.connectivity.ipmemorystore.$$Lambda$IpMemoryStoreService$g6ct0gEBtRd2rm1ffZuoQekbOM */
            private final /* synthetic */ IOnStatusListener f$1;
            private final /* synthetic */ RegularMaintenanceJobService.InterruptMaintenance f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                IpMemoryStoreService.this.lambda$fullMaintenance$7$IpMemoryStoreService(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$fullMaintenance$7$IpMemoryStoreService(IOnStatusListener iOnStatusListener, RegularMaintenanceJobService.InterruptMaintenance interruptMaintenance) {
        float f;
        try {
            if (this.mDb == null) {
                iOnStatusListener.onComplete(makeStatus(-3));
            } else if (!checkForInterrupt(iOnStatusListener, interruptMaintenance)) {
                int dropAllExpiredRecords = IpMemoryStoreDatabase.dropAllExpiredRecords(this.mDb);
                if (!checkForInterrupt(iOnStatusListener, interruptMaintenance)) {
                    int i = 0;
                    while (isDbSizeOverThreshold() && i < 500) {
                        if (!checkForInterrupt(iOnStatusListener, interruptMaintenance)) {
                            int totalRecordNumber = IpMemoryStoreDatabase.getTotalRecordNumber(this.mDb);
                            long dbSize = getDbSize();
                            if (dbSize == 0) {
                                f = 0.0f;
                            } else {
                                f = ((float) (dbSize - ((long) getDbSizeThreshold()))) / ((float) dbSize);
                            }
                            dropAllExpiredRecords = IpMemoryStoreDatabase.dropNumberOfRecords(this.mDb, Math.max((int) (((float) totalRecordNumber) * f), 5));
                            if (!checkForInterrupt(iOnStatusListener, interruptMaintenance)) {
                                i++;
                            } else {
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                    iOnStatusListener.onComplete(makeStatus(dropAllExpiredRecords));
                }
            }
        } catch (RemoteException unused) {
        }
    }

    private boolean checkForInterrupt(IOnStatusListener iOnStatusListener, RegularMaintenanceJobService.InterruptMaintenance interruptMaintenance) throws RemoteException {
        if (!interruptMaintenance.isInterrupted()) {
            return false;
        }
        iOnStatusListener.onComplete(makeStatus(-1000000001));
        return true;
    }
}
