package edge;

import java.util.Map;

public class EdgeMapEntry {
    public Map.Entry<String, Integer> first(Map<String, Integer> values) {
        return values.entrySet().iterator().next();
    }

    public int sum(Map<String, Integer> values) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            total += entry.getValue();
        }
        return total;
    }
}
