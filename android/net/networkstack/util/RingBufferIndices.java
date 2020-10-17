package android.net.networkstack.util;

public class RingBufferIndices {
    private final int mCapacity;
    private int mSize;
    private int mStart;

    public RingBufferIndices(int i) {
        this.mCapacity = i;
    }

    public int add() {
        int i = this.mSize;
        int i2 = this.mCapacity;
        if (i < i2) {
            this.mSize = i + 1;
            return i;
        }
        int i3 = this.mStart;
        this.mStart = i3 + 1;
        if (this.mStart == i2) {
            this.mStart = 0;
        }
        return i3;
    }

    public int size() {
        return this.mSize;
    }

    public int indexOf(int i) {
        int i2 = this.mStart + i;
        int i3 = this.mCapacity;
        return i2 >= i3 ? i2 - i3 : i2;
    }
}
