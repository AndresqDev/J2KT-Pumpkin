package dev.j2kt;

import java.util.Objects;

/**
 * A small Kotlin type model. It intentionally carries only the information the
 * emitter needs: rendered name, Java origin, nullability, and container shape.
 */
public final class KType {
    public enum Category {
        UNIT, BOOLEAN, CHAR, NUMBER, STRING, COLLECTION, MAP, ARRAY, OBJECT, NULL, UNKNOWN, FUNCTION
    }

    private final String base;
    private final String javaName;
    private final Category category;
    private final boolean nullable;
    private final KType element;
    private final KType key;
    private final boolean mutable;

    private KType(String base, String javaName, Category category, boolean nullable,
                  KType element, KType key, boolean mutable) {
        this.base = base;
        this.javaName = javaName;
        this.category = category;
        this.nullable = nullable;
        this.element = element;
        this.key = key;
        this.mutable = mutable;
    }

    public static KType simple(String base, String javaName, Category category) {
        return new KType(base, javaName, category, false, null, null, false);
    }

    public static KType collection(String base, String javaName, KType element, boolean mutable) {
        return new KType(base, javaName, Category.COLLECTION, false, element, null, mutable);
    }

    public static KType map(String base, String javaName, KType key, KType value, boolean mutable) {
        return new KType(base, javaName, Category.MAP, false, value, key, mutable);
    }

    public static KType array(String base, String javaName, KType element) {
        return new KType(base, javaName, Category.ARRAY, false, element, null, false);
    }

    public static KType function(String base, String javaName) {
        return new KType(base, javaName, Category.FUNCTION, false, null, null, false);
    }

    public static KType unknown(String hint) {
        return new KType("Any?", hint == null ? "<unknown>" : hint, Category.UNKNOWN, true, null, null, false);
    }

    public static final KType UNIT = simple("Unit", "void", Category.UNIT);
    public static final KType BOOLEAN = simple("Boolean", "boolean", Category.BOOLEAN);
    public static final KType CHAR = simple("Char", "char", Category.CHAR);
    public static final KType INT = simple("Int", "int", Category.NUMBER);
    public static final KType LONG = simple("Long", "long", Category.NUMBER);
    public static final KType DOUBLE = simple("Double", "double", Category.NUMBER);
    public static final KType FLOAT = simple("Float", "float", Category.NUMBER);
    public static final KType SHORT = simple("Short", "short", Category.NUMBER);
    public static final KType BYTE = simple("Byte", "byte", Category.NUMBER);
    public static final KType STRING = simple("String", "java.lang.String", Category.STRING);
    public static final KType NULL = new KType("Any?", "null", Category.NULL, true, null, null, false);

    public String base() { return base; }
    public String javaName() { return javaName; }
    public Category category() { return category; }
    public boolean nullable() { return nullable; }
    public KType element() { return element; }
    public KType key() { return key; }
    public boolean mutable() { return mutable; }

    public String render() {
        return base + (nullable && !base.endsWith("?") ? "?" : "");
    }

    public KType withNullable(boolean value) {
        if (value == nullable) {
            return this;
        }
        return new KType(base, javaName, category, value, element, key, mutable);
    }

    public KType withJavaName(String value) {
        return new KType(base, value, category, nullable, element, key, mutable);
    }

    public KType asNullable() { return withNullable(true); }
    public KType asNonNull() { return withNullable(false); }

    public boolean isNullable() { return nullable || category == Category.NULL; }
    public boolean isNumeric() { return category == Category.NUMBER || category == Category.CHAR; }
    public boolean isIntegral() {
        return category == Category.CHAR
                || "Int".equals(base) || "Long".equals(base) || "Short".equals(base) || "Byte".equals(base);
    }
    public boolean isString() { return category == Category.STRING; }
    public boolean isCollection() { return category == Category.COLLECTION; }
    public boolean isMap() { return category == Category.MAP; }
    public boolean isArray() { return category == Category.ARRAY; }
    public boolean isBoolean() { return category == Category.BOOLEAN; }
    public boolean isUnit() { return category == Category.UNIT; }
    public boolean isUnknown() { return category == Category.UNKNOWN; }

    /** Kotlin reference types participate in identity equality like Java objects. */
    public boolean isReferenceType() {
        return switch (category) {
            case NUMBER, BOOLEAN, CHAR, UNIT -> false;
            default -> true;
        };
    }

    public boolean isKotlinPrimitive() {
        return switch (category) {
            case NUMBER, BOOLEAN, CHAR -> true;
            default -> false;
        };
    }

    public boolean sameAs(KType other) {
        if (other == null) {
            return false;
        }
        return base.equals(other.base) && nullable == other.nullable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KType other)) return false;
        return nullable == other.nullable
                && mutable == other.mutable
                && base.equals(other.base)
                && category == other.category
                && Objects.equals(element, other.element)
                && Objects.equals(key, other.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, category, nullable, element, key, mutable);
    }

    @Override
    public String toString() {
        return render();
    }
}
