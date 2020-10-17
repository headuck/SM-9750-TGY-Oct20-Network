package android.net.ipmemorystore;

import java.util.Objects;

public class SameL3NetworkResponse {
    public final float confidence;
    public final String l2Key1;
    public final String l2Key2;

    public final int getNetworkSameness() {
        float f = this.confidence;
        if (((double) f) > 1.0d || ((double) f) < 0.0d) {
            return 3;
        }
        return ((double) f) > 0.5d ? 1 : 2;
    }

    public SameL3NetworkResponse(String str, String str2, float f) {
        this.l2Key1 = str;
        this.l2Key2 = str2;
        this.confidence = f;
    }

    public SameL3NetworkResponse(SameL3NetworkResponseParcelable sameL3NetworkResponseParcelable) {
        this(sameL3NetworkResponseParcelable.l2Key1, sameL3NetworkResponseParcelable.l2Key2, sameL3NetworkResponseParcelable.confidence);
    }

    public SameL3NetworkResponseParcelable toParcelable() {
        SameL3NetworkResponseParcelable sameL3NetworkResponseParcelable = new SameL3NetworkResponseParcelable();
        sameL3NetworkResponseParcelable.l2Key1 = this.l2Key1;
        sameL3NetworkResponseParcelable.l2Key2 = this.l2Key2;
        sameL3NetworkResponseParcelable.confidence = this.confidence;
        return sameL3NetworkResponseParcelable;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SameL3NetworkResponse)) {
            return false;
        }
        SameL3NetworkResponse sameL3NetworkResponse = (SameL3NetworkResponse) obj;
        if (!this.l2Key1.equals(sameL3NetworkResponse.l2Key1) || !this.l2Key2.equals(sameL3NetworkResponse.l2Key2) || this.confidence != sameL3NetworkResponse.confidence) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return Objects.hash(this.l2Key1, this.l2Key2, Float.valueOf(this.confidence));
    }

    public String toString() {
        int networkSameness = getNetworkSameness();
        if (networkSameness == 1) {
            return "\"" + this.l2Key1 + "\" same L3 network as \"" + this.l2Key2 + "\"";
        } else if (networkSameness == 2) {
            return "\"" + this.l2Key1 + "\" different L3 network from \"" + this.l2Key2 + "\"";
        } else if (networkSameness != 3) {
            return "Buggy sameness value ? \"" + this.l2Key1 + "\", \"" + this.l2Key2 + "\"";
        } else {
            return "\"" + this.l2Key1 + "\" can't be tested against \"" + this.l2Key2 + "\"";
        }
    }
}
