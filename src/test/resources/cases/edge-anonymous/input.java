package edge;

import java.util.Comparator;

public class EdgeAnonymous {
    public Runnable task() {
        return new Runnable() {
            @Override
            public void run() {
                System.out.println("x");
            }
        };
    }

    public Comparator<String> comparator() {
        return new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.compareTo(b);
            }
        };
    }
}
