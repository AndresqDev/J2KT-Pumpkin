package edge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EdgeCollections {
    private final List<String> names = new ArrayList<>();
    private final Map<String, Integer> counts = new HashMap<>();

    public void add(String name) {
        names.add(name);
        counts.put(name, counts.getOrDefault(name, 0) + 1);
    }

    public String first() {
        return names.get(0);
    }

    public void removeFirst() {
        names.remove(0);
    }

    public int size() {
        return names.size();
    }

    public int[] copy(int[] values) {
        return java.util.Arrays.copyOf(values, values.length);
    }
}
