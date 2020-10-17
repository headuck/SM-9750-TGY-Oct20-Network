package android.net.shared;

import android.net.InetAddresses;
import android.net.InitialConfigurationParcelable;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.RouteInfo;
import android.text.TextUtils;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class InitialConfiguration {
    public static final InetAddress INET6_ANY = InetAddresses.parseNumericAddress("::");
    public final Set<IpPrefix> directlyConnectedRoutes = new HashSet();
    public final Set<InetAddress> dnsServers = new HashSet();
    public final Set<LinkAddress> ipAddresses = new HashSet();

    private static boolean isCompliantIPv6PrefixLength(int i) {
        return 48 <= i && i <= 64;
    }

    public static InitialConfiguration copy(InitialConfiguration initialConfiguration) {
        if (initialConfiguration == null) {
            return null;
        }
        InitialConfiguration initialConfiguration2 = new InitialConfiguration();
        initialConfiguration2.ipAddresses.addAll(initialConfiguration.ipAddresses);
        initialConfiguration2.directlyConnectedRoutes.addAll(initialConfiguration.directlyConnectedRoutes);
        initialConfiguration2.dnsServers.addAll(initialConfiguration.dnsServers);
        return initialConfiguration2;
    }

    public String toString() {
        return String.format("InitialConfiguration(IPs: {%s}, prefixes: {%s}, DNS: {%s})", TextUtils.join(", ", this.ipAddresses), TextUtils.join(", ", this.directlyConnectedRoutes), TextUtils.join(", ", this.dnsServers));
    }

    public boolean isValid() {
        if (this.ipAddresses.isEmpty()) {
            return false;
        }
        for (LinkAddress linkAddress : this.ipAddresses) {
            if (!any(this.directlyConnectedRoutes, new Predicate(linkAddress) {
                /* class android.net.shared.$$Lambda$InitialConfiguration$me_MsIS7iJUFBnz7VXb_bkuZ1g */
                private final /* synthetic */ LinkAddress f$0;

                {
                    this.f$0 = r1;
                }

                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ((IpPrefix) obj).contains(this.f$0.getAddress());
                }
            })) {
                return false;
            }
        }
        for (InetAddress inetAddress : this.dnsServers) {
            if (!any(this.directlyConnectedRoutes, new Predicate(inetAddress) {
                /* class android.net.shared.$$Lambda$InitialConfiguration$fPcApZyObbcs0b_t0iDOb7uyyVo */
                private final /* synthetic */ InetAddress f$0;

                {
                    this.f$0 = r1;
                }

                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ((IpPrefix) obj).contains(this.f$0);
                }
            })) {
                return false;
            }
        }
        if (any(this.ipAddresses, not($$Lambda$InitialConfiguration$V8CoLWaph9IN8YQg4mdBlxzUFkI.INSTANCE))) {
            return false;
        }
        if ((!any(this.directlyConnectedRoutes, $$Lambda$InitialConfiguration$p_tYzrU5UtVU6wwyLn0miHqz00.INSTANCE) || !all(this.ipAddresses, not($$Lambda$InitialConfiguration$w5pXtjcZU54QER7TNMAvC4NSrfg.INSTANCE))) && !any(this.directlyConnectedRoutes, not($$Lambda$InitialConfiguration$GEWT0gKtlkvLtgqIAII3RLQt3xQ.INSTANCE)) && this.ipAddresses.stream().filter($$Lambda$InitialConfiguration$58qOz8A9XDsHfGLzFKkSYaJR6w.INSTANCE).count() <= 1) {
            return true;
        }
        return false;
    }

    public boolean isProvisionedBy(List<LinkAddress> list, List<RouteInfo> list2) {
        if (this.ipAddresses.isEmpty()) {
            return false;
        }
        for (LinkAddress linkAddress : this.ipAddresses) {
            if (!any(list, new Predicate(linkAddress) {
                /* class android.net.shared.$$Lambda$InitialConfiguration$52iZTx1bKOBJkSzJZ2XL1b1D8g */
                private final /* synthetic */ LinkAddress f$0;

                {
                    this.f$0 = r1;
                }

                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return this.f$0.isSameAddressAs((LinkAddress) obj);
                }
            })) {
                return false;
            }
        }
        if (list2 == null) {
            return true;
        }
        for (IpPrefix ipPrefix : this.directlyConnectedRoutes) {
            if (!any(list2, new Predicate(ipPrefix) {
                /* class android.net.shared.$$Lambda$InitialConfiguration$4Dtg_2trFtjFoeKOpr0rSIEUXu8 */
                private final /* synthetic */ IpPrefix f$0;

                {
                    this.f$0 = r1;
                }

                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return InitialConfiguration.isDirectlyConnectedRoute((RouteInfo) obj, this.f$0);
                }
            })) {
                return false;
            }
        }
        return true;
    }

    public static InitialConfiguration fromStableParcelable(InitialConfigurationParcelable initialConfigurationParcelable) {
        if (initialConfigurationParcelable == null) {
            return null;
        }
        InitialConfiguration initialConfiguration = new InitialConfiguration();
        initialConfiguration.ipAddresses.addAll(Arrays.asList(initialConfigurationParcelable.ipAddresses));
        initialConfiguration.directlyConnectedRoutes.addAll(Arrays.asList(initialConfigurationParcelable.directlyConnectedRoutes));
        initialConfiguration.dnsServers.addAll(ParcelableUtil.fromParcelableArray(initialConfigurationParcelable.dnsServers, $$Lambda$SYWvjOUPlAZ_O2Z6yfFU9np1858.INSTANCE));
        return initialConfiguration;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof InitialConfiguration)) {
            return false;
        }
        InitialConfiguration initialConfiguration = (InitialConfiguration) obj;
        if (!this.ipAddresses.equals(initialConfiguration.ipAddresses) || !this.directlyConnectedRoutes.equals(initialConfiguration.directlyConnectedRoutes) || !this.dnsServers.equals(initialConfiguration.dnsServers)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public static boolean isDirectlyConnectedRoute(RouteInfo routeInfo, IpPrefix ipPrefix) {
        return !routeInfo.hasGateway() && ipPrefix.equals(routeInfo.getDestination());
    }

    /* access modifiers changed from: private */
    public static boolean isPrefixLengthCompliant(LinkAddress linkAddress) {
        return isIPv4(linkAddress) || isCompliantIPv6PrefixLength(linkAddress.getPrefixLength());
    }

    /* access modifiers changed from: private */
    public static boolean isPrefixLengthCompliant(IpPrefix ipPrefix) {
        return isIPv4(ipPrefix) || isCompliantIPv6PrefixLength(ipPrefix.getPrefixLength());
    }

    private static boolean isIPv4(IpPrefix ipPrefix) {
        return ipPrefix.getAddress() instanceof Inet4Address;
    }

    /* access modifiers changed from: private */
    public static boolean isIPv4(LinkAddress linkAddress) {
        return linkAddress.getAddress() instanceof Inet4Address;
    }

    /* access modifiers changed from: private */
    public static boolean isIPv6DefaultRoute(IpPrefix ipPrefix) {
        return ipPrefix.getAddress().equals(INET6_ANY);
    }

    /* access modifiers changed from: private */
    public static boolean isIPv6GUA(LinkAddress linkAddress) {
        return linkAddress.isIpv6() && linkAddress.isGlobalPreferred();
    }

    public static <T> boolean any(Iterable<T> iterable, Predicate<T> predicate) {
        for (T t : iterable) {
            if (predicate.test(t)) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean all(Iterable<T> iterable, Predicate<T> predicate) {
        return !any(iterable, not(predicate));
    }

    static /* synthetic */ boolean lambda$not$4(Predicate predicate, Object obj) {
        return !predicate.test(obj);
    }

    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return new Predicate(predicate) {
            /* class android.net.shared.$$Lambda$InitialConfiguration$MwQ3SqOgt4uewKbIAElg1lpdfTI */
            private final /* synthetic */ Predicate f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return InitialConfiguration.lambda$not$4(this.f$0, obj);
            }
        };
    }
}
