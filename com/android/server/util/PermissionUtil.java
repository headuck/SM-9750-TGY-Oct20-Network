package com.android.server.util;

import android.os.Binder;
import android.os.UserHandle;
import java.util.concurrent.atomic.AtomicInteger;

public final class PermissionUtil {
    private static final AtomicInteger sSystemPid = new AtomicInteger(-1);

    public static void checkNetworkStackCallingPermission() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1000) {
            checkConsistentSystemPid();
        } else if (UserHandle.getAppId(callingUid) != 1002) {
            throw new SecurityException("Invalid caller: " + callingUid);
        }
    }

    private static void checkConsistentSystemPid() {
        int callingPid = Binder.getCallingPid();
        if (!sSystemPid.compareAndSet(-1, callingPid) && sSystemPid.get() != callingPid) {
            throw new SecurityException("Invalid PID for the system server, expected " + sSystemPid.get() + " but was called from " + callingPid);
        }
    }

    public static void checkDumpPermission() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000 && callingUid != 0 && callingUid != 2000) {
            throw new SecurityException("No dump permissions for caller: " + callingUid);
        }
    }
}
