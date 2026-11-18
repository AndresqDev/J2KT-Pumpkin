package edge;

public class EdgeNumeric {
    public long widen(int a, long b) {
        return a + b;
    }

    public int chars(char a, char b) {
        return a + b;
    }

    public byte bump(byte b) {
        return (byte) (b + 1);
    }

    public int bits(int a, int b) {
        return (a & b) | (a << 2) ^ (b >>> 1);
    }

    public boolean same(String a, String b) {
        return a == b;
    }

    public boolean sameValue(String a, String b) {
        return a.equals(b);
    }

    public double mix(int a, double b) {
        return a * b + 1;
    }

    public char next(char c) {
        return (char) (c + 1);
    }
}
