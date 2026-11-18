package edge;

public class EdgeLoops {
    public int sumEven(int n) {
        int total = 0;
        for (int i = 0; i < n; i += 2) {
            total += i;
        }
        return total;
    }

    public int countdown(int n) {
        int total = 0;
        for (int i = n; i > 0; i--) {
            total += i;
        }
        return total;
    }

    public int inclusive(int n) {
        int total = 0;
        for (int i = 0; i <= n; i++) {
            total += i;
        }
        return total;
    }

    public void labeled() {
        outer:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 1) continue outer;
                if (i == 2) break outer;
            }
        }
    }
}
