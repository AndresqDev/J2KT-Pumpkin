package edge;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EdgeFactory {
    public List<String> names() {
        return List.of("a", "b");
    }

    public Set<String> unique() {
        return Set.of("a", "b");
    }

    public Map<String, Integer> counts() {
        return Map.of("a", 1, "b", 2);
    }
}
