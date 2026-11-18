package edge;

import java.util.Optional;

public class EdgeOptional {
    public String name(Optional<String> value) {
        return value.orElse("unknown");
    }

    public boolean present(Optional<String> value) {
        return value.isPresent();
    }

    public String upper(Optional<String> value) {
        return value.map(String::toUpperCase).orElse("");
    }
}
