package android.net.apf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ApfGenerator {
    private final Instruction mDropLabel = new Instruction(this, Opcodes.LABEL);
    private boolean mGenerated;
    private final ArrayList<Instruction> mInstructions = new ArrayList<>();
    private final HashMap<String, Instruction> mLabels = new HashMap<>();
    private final Instruction mPassLabel = new Instruction(this, Opcodes.LABEL);
    private final int mVersion;

    public static boolean supportsVersion(int i) {
        return i >= 2;
    }

    public static class IllegalInstructionException extends Exception {
        IllegalInstructionException(String str) {
            super(str);
        }
    }

    /* access modifiers changed from: private */
    public enum Opcodes {
        LABEL(-1),
        LDB(1),
        LDH(2),
        LDW(3),
        LDBX(4),
        LDHX(5),
        LDWX(6),
        ADD(7),
        MUL(8),
        DIV(9),
        AND(10),
        OR(11),
        SH(12),
        LI(13),
        JMP(14),
        JEQ(15),
        JNE(16),
        JGT(17),
        JLT(18),
        JSET(19),
        JNEBS(20),
        EXT(21),
        LDDW(22),
        STDW(23);
        
        final int value;

        private Opcodes(int i) {
            this.value = i;
        }
    }

    private enum ExtendedOpcodes {
        LDM(0),
        STM(16),
        NOT(32),
        NEG(33),
        SWAP(34),
        MOVE(35);
        
        final int value;

        private ExtendedOpcodes(int i) {
            this.value = i;
        }
    }

    public enum Register {
        R0(0),
        R1(1);
        
        final int value;

        private Register(int i) {
            this.value = i;
        }
    }

    /* access modifiers changed from: private */
    public class Instruction {
        private byte[] mCompareBytes;
        private boolean mHasImm;
        private int mImm;
        private boolean mImmSigned;
        private byte mImmSize;
        private String mLabel;
        private final byte mOpcode;
        private final byte mRegister;
        private String mTargetLabel;
        private byte mTargetLabelSize;
        int offset;

        private byte calculateImmSize(int i, boolean z) {
            if (i == 0) {
                return 0;
            }
            if (z && i >= -128 && i <= 127) {
                return 1;
            }
            if (!z && i >= 0 && i <= 255) {
                return 1;
            }
            if (!z || i < -32768 || i > 32767) {
                return (z || i < 0 || i > 65535) ? (byte) 4 : 2;
            }
            return 2;
        }

        Instruction(Opcodes opcodes, Register register) {
            this.mOpcode = (byte) opcodes.value;
            this.mRegister = (byte) register.value;
        }

        Instruction(ApfGenerator apfGenerator, Opcodes opcodes) {
            this(opcodes, Register.R0);
        }

        /* access modifiers changed from: package-private */
        public void setImm(int i, boolean z) {
            this.mHasImm = true;
            this.mImm = i;
            this.mImmSigned = z;
            this.mImmSize = calculateImmSize(i, z);
        }

        /* access modifiers changed from: package-private */
        public void setUnsignedImm(int i) {
            setImm(i, false);
        }

        /* access modifiers changed from: package-private */
        public void setSignedImm(int i) {
            setImm(i, true);
        }

        /* access modifiers changed from: package-private */
        public void setLabel(String str) throws IllegalInstructionException {
            if (ApfGenerator.this.mLabels.containsKey(str)) {
                throw new IllegalInstructionException("duplicate label " + str);
            } else if (this.mOpcode == Opcodes.LABEL.value) {
                this.mLabel = str;
                ApfGenerator.this.mLabels.put(str, this);
            } else {
                throw new IllegalStateException("adding label to non-label instruction");
            }
        }

        /* access modifiers changed from: package-private */
        public void setTargetLabel(String str) {
            this.mTargetLabel = str;
            this.mTargetLabelSize = 4;
        }

        /* access modifiers changed from: package-private */
        public void setCompareBytes(byte[] bArr) {
            if (this.mOpcode == Opcodes.JNEBS.value) {
                this.mCompareBytes = bArr;
                return;
            }
            throw new IllegalStateException("adding compare bytes to non-JNEBS instruction");
        }

        /* access modifiers changed from: package-private */
        public int size() {
            if (this.mOpcode == Opcodes.LABEL.value) {
                return 0;
            }
            int i = 1;
            if (this.mHasImm) {
                i = 1 + generatedImmSize();
            }
            if (this.mTargetLabel != null) {
                i += generatedImmSize();
            }
            byte[] bArr = this.mCompareBytes;
            return bArr != null ? i + bArr.length : i;
        }

        /* access modifiers changed from: package-private */
        public boolean shrink() throws IllegalInstructionException {
            if (this.mTargetLabel == null) {
                return false;
            }
            int size = size();
            byte b = this.mTargetLabelSize;
            this.mTargetLabelSize = calculateImmSize(calculateTargetLabelOffset(), false);
            if (this.mTargetLabelSize > b) {
                throw new IllegalStateException("instruction grew");
            } else if (size() < size) {
                return true;
            } else {
                return false;
            }
        }

        private byte generateImmSizeField() {
            byte generatedImmSize = generatedImmSize();
            if (generatedImmSize == 4) {
                return 3;
            }
            return generatedImmSize;
        }

        private byte generateInstructionByte() {
            return (byte) ((generateImmSizeField() << 1) | (this.mOpcode << 3) | this.mRegister);
        }

        private int writeValue(int i, byte[] bArr, int i2) {
            int generatedImmSize = generatedImmSize() - 1;
            while (generatedImmSize >= 0) {
                bArr[i2] = (byte) ((i >> (generatedImmSize * 8)) & 255);
                generatedImmSize--;
                i2++;
            }
            return i2;
        }

        /* access modifiers changed from: package-private */
        public void generate(byte[] bArr) throws IllegalInstructionException {
            if (this.mOpcode != Opcodes.LABEL.value) {
                int i = this.offset;
                int i2 = i + 1;
                bArr[i] = generateInstructionByte();
                if (this.mTargetLabel != null) {
                    i2 = writeValue(calculateTargetLabelOffset(), bArr, i2);
                }
                if (this.mHasImm) {
                    i2 = writeValue(this.mImm, bArr, i2);
                }
                byte[] bArr2 = this.mCompareBytes;
                if (bArr2 != null) {
                    System.arraycopy(bArr2, 0, bArr, i2, bArr2.length);
                    i2 += this.mCompareBytes.length;
                }
                if (i2 - this.offset != size()) {
                    throw new IllegalStateException("wrote " + (i2 - this.offset) + " but should have written " + size());
                }
            }
        }

        private byte generatedImmSize() {
            byte b = this.mImmSize;
            byte b2 = this.mTargetLabelSize;
            return b > b2 ? b : b2;
        }

        private int calculateTargetLabelOffset() throws IllegalInstructionException {
            Instruction instruction;
            String str = this.mTargetLabel;
            if (str == "__DROP__") {
                instruction = ApfGenerator.this.mDropLabel;
            } else if (str == "__PASS__") {
                instruction = ApfGenerator.this.mPassLabel;
            } else {
                instruction = (Instruction) ApfGenerator.this.mLabels.get(this.mTargetLabel);
            }
            if (instruction != null) {
                int size = instruction.offset - (this.offset + size());
                if (size >= 0) {
                    return size;
                }
                throw new IllegalInstructionException("backward branches disallowed; label: " + this.mTargetLabel);
            }
            throw new IllegalInstructionException("label not found: " + this.mTargetLabel);
        }
    }

    ApfGenerator(int i) throws IllegalInstructionException {
        this.mVersion = i;
        requireApfVersion(2);
    }

    private void requireApfVersion(int i) throws IllegalInstructionException {
        if (this.mVersion < i) {
            throw new IllegalInstructionException("Requires APF >= " + i);
        }
    }

    private void addInstruction(Instruction instruction) {
        if (!this.mGenerated) {
            this.mInstructions.add(instruction);
            return;
        }
        throw new IllegalStateException("Program already generated");
    }

    public ApfGenerator defineLabel(String str) throws IllegalInstructionException {
        Instruction instruction = new Instruction(this, Opcodes.LABEL);
        instruction.setLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJump(String str) {
        Instruction instruction = new Instruction(this, Opcodes.JMP);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoad8(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LDB, register);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoad16(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LDH, register);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoad32(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LDW, register);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoad8Indexed(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LDBX, register);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoad16Indexed(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LDHX, register);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addAdd(int i) {
        Instruction instruction = new Instruction(this, Opcodes.ADD);
        instruction.setSignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addAnd(int i) {
        Instruction instruction = new Instruction(this, Opcodes.AND);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addRightShift(int i) {
        Instruction instruction = new Instruction(this, Opcodes.SH);
        instruction.setSignedImm(-i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addAddR1() {
        addInstruction(new Instruction(Opcodes.ADD, Register.R1));
        return this;
    }

    public ApfGenerator addLoadImmediate(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LI, register);
        instruction.setSignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfR0Equals(int i, String str) {
        Instruction instruction = new Instruction(this, Opcodes.JEQ);
        instruction.setUnsignedImm(i);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfR0NotEquals(int i, String str) {
        Instruction instruction = new Instruction(this, Opcodes.JNE);
        instruction.setUnsignedImm(i);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfR0GreaterThan(int i, String str) {
        Instruction instruction = new Instruction(this, Opcodes.JGT);
        instruction.setUnsignedImm(i);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfR0LessThan(int i, String str) {
        Instruction instruction = new Instruction(this, Opcodes.JLT);
        instruction.setUnsignedImm(i);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfR0AnyBitsSet(int i, String str) {
        Instruction instruction = new Instruction(this, Opcodes.JSET);
        instruction.setUnsignedImm(i);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfBytesNotEqual(Register register, byte[] bArr, String str) throws IllegalInstructionException {
        if (register != Register.R1) {
            Instruction instruction = new Instruction(Opcodes.JNEBS, register);
            instruction.setUnsignedImm(bArr.length);
            instruction.setTargetLabel(str);
            instruction.setCompareBytes(bArr);
            addInstruction(instruction);
            return this;
        }
        throw new IllegalInstructionException("JNEBS fails with R1");
    }

    public ApfGenerator addLoadFromMemory(Register register, int i) throws IllegalInstructionException {
        if (i < 0 || i > 15) {
            throw new IllegalInstructionException("illegal memory slot number: " + i);
        }
        Instruction instruction = new Instruction(Opcodes.EXT, register);
        instruction.setUnsignedImm(ExtendedOpcodes.LDM.value + i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addNeg(Register register) {
        Instruction instruction = new Instruction(Opcodes.EXT, register);
        instruction.setUnsignedImm(ExtendedOpcodes.NEG.value);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addSwap() {
        Instruction instruction = new Instruction(this, Opcodes.EXT);
        instruction.setUnsignedImm(ExtendedOpcodes.SWAP.value);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoadData(Register register, int i) throws IllegalInstructionException {
        requireApfVersion(3);
        Instruction instruction = new Instruction(Opcodes.LDDW, register);
        instruction.setSignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addStoreData(Register register, int i) throws IllegalInstructionException {
        requireApfVersion(3);
        Instruction instruction = new Instruction(Opcodes.STDW, register);
        instruction.setSignedImm(i);
        addInstruction(instruction);
        return this;
    }

    private int updateInstructionOffsets() {
        Iterator<Instruction> it = this.mInstructions.iterator();
        int i = 0;
        while (it.hasNext()) {
            Instruction next = it.next();
            next.offset = i;
            i += next.size();
        }
        return i;
    }

    public int programLengthOverEstimate() {
        return updateInstructionOffsets();
    }

    public byte[] generate() throws IllegalInstructionException {
        int updateInstructionOffsets;
        if (!this.mGenerated) {
            this.mGenerated = true;
            int i = 10;
            while (true) {
                updateInstructionOffsets = updateInstructionOffsets();
                this.mDropLabel.offset = updateInstructionOffsets + 1;
                this.mPassLabel.offset = updateInstructionOffsets;
                int i2 = i - 1;
                if (i == 0) {
                    break;
                }
                boolean z = false;
                Iterator<Instruction> it = this.mInstructions.iterator();
                while (it.hasNext()) {
                    if (it.next().shrink()) {
                        z = true;
                    }
                }
                if (!z) {
                    break;
                }
                i = i2;
            }
            byte[] bArr = new byte[updateInstructionOffsets];
            Iterator<Instruction> it2 = this.mInstructions.iterator();
            while (it2.hasNext()) {
                it2.next().generate(bArr);
            }
            return bArr;
        }
        throw new IllegalStateException("Can only generate() once!");
    }
}
