package edge;

public class EdgeAssign {
    public byte bump(byte b) {
        b += 1;
        return b;
    }

    public int addLong(int i) {
        i += 2L;
        return i;
    }

    public char next(char c) {
        c += 1;
        return c;
    }

    public int clearLowBit(int x) {
        x &= ~1;
        return x;
    }

    public int shift(int x) {
        x <<= 2;
        return x;
    }
}
