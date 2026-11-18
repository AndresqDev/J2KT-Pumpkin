package dev.j2kt;

import java.util.Set;

/** Small Kotlin naming helpers. */
public final class NameUtils {
    // Only hard keywords must be escaped. Soft keywords such as `value`, `field`,
    // `get`, `set`, `out` and `in` are valid identifiers in most positions, and
    // escaping them makes generated code harder to read.
    private static final Set<String> KEYWORDS = Set.of(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if",
            "in", "interface", "is", "null", "object", "package", "return", "super", "this",
            "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while"
    );

    private NameUtils() {
    }

    public static String escape(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        if (KEYWORDS.contains(name)) {
            return "`" + name + "`";
        }
        return name;
    }

    public static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public static String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    public static String stringLiteral(String rawContent) {
        return "\"" + rawContent.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}
