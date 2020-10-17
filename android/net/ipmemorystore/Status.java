package android.net.ipmemorystore;

public class Status {
    public final int resultCode;

    public Status(int i) {
        this.resultCode = i;
    }

    public Status(StatusParcelable statusParcelable) {
        this(statusParcelable.resultCode);
    }

    public StatusParcelable toParcelable() {
        StatusParcelable statusParcelable = new StatusParcelable();
        statusParcelable.resultCode = this.resultCode;
        return statusParcelable;
    }

    public boolean isSuccess() {
        return this.resultCode == 0;
    }

    public String toString() {
        int i = this.resultCode;
        if (i == -4) {
            return "DATABASE STORAGE ERROR";
        }
        if (i == -3) {
            return "DATABASE CANNOT BE OPENED";
        }
        if (i == -2) {
            return "ILLEGAL ARGUMENT";
        }
        if (i != -1) {
            return i != 0 ? "Unknown value ?!" : "SUCCESS";
        }
        return "GENERIC ERROR";
    }
}
