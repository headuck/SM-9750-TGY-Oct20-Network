package android.net.networkstack.util;

import java.io.PrintWriter;
import java.io.Writer;

public class IndentingPrintWriter extends PrintWriter {
    private char[] mCurrentIndent;
    private int mCurrentLength;
    private boolean mEmptyLine;
    private StringBuilder mIndentBuilder;
    private char[] mSingleChar;
    private final String mSingleIndent;
    private final int mWrapLength;

    public IndentingPrintWriter(Writer writer, String str) {
        this(writer, str, -1);
    }

    public IndentingPrintWriter(Writer writer, String str, int i) {
        super(writer);
        this.mIndentBuilder = new StringBuilder();
        this.mEmptyLine = true;
        this.mSingleChar = new char[1];
        this.mSingleIndent = str;
        this.mWrapLength = i;
    }

    public IndentingPrintWriter increaseIndent() {
        this.mIndentBuilder.append(this.mSingleIndent);
        this.mCurrentIndent = null;
        return this;
    }

    public IndentingPrintWriter decreaseIndent() {
        this.mIndentBuilder.delete(0, this.mSingleIndent.length());
        this.mCurrentIndent = null;
        return this;
    }

    public void println() {
        write(10);
    }

    @Override // java.io.PrintWriter, java.io.Writer
    public void write(int i) {
        char[] cArr = this.mSingleChar;
        cArr[0] = (char) i;
        write(cArr, 0, 1);
    }

    @Override // java.io.PrintWriter, java.io.Writer
    public void write(String str, int i, int i2) {
        char[] cArr = new char[i2];
        str.getChars(i, i2 - i, cArr, 0);
        write(cArr, 0, i2);
    }

    @Override // java.io.PrintWriter, java.io.Writer
    public void write(char[] cArr, int i, int i2) {
        int length = this.mIndentBuilder.length();
        int i3 = i2 + i;
        int i4 = i;
        while (i < i3) {
            int i5 = i + 1;
            char c = cArr[i];
            this.mCurrentLength++;
            if (c == '\n') {
                maybeWriteIndent();
                super.write(cArr, i4, i5 - i4);
                this.mEmptyLine = true;
                this.mCurrentLength = 0;
                i4 = i5;
            }
            int i6 = this.mWrapLength;
            if (i6 > 0 && this.mCurrentLength >= i6 - length) {
                if (!this.mEmptyLine) {
                    super.write(10);
                    this.mEmptyLine = true;
                    this.mCurrentLength = i5 - i4;
                } else {
                    maybeWriteIndent();
                    super.write(cArr, i4, i5 - i4);
                    super.write(10);
                    this.mEmptyLine = true;
                    this.mCurrentLength = 0;
                    i4 = i5;
                }
            }
            i = i5;
        }
        if (i4 != i) {
            maybeWriteIndent();
            super.write(cArr, i4, i - i4);
        }
    }

    private void maybeWriteIndent() {
        if (this.mEmptyLine) {
            this.mEmptyLine = false;
            if (this.mIndentBuilder.length() != 0) {
                if (this.mCurrentIndent == null) {
                    this.mCurrentIndent = this.mIndentBuilder.toString().toCharArray();
                }
                char[] cArr = this.mCurrentIndent;
                super.write(cArr, 0, cArr.length);
            }
        }
    }
}
