package dev.j2kt;

/** Indentation-aware text builder. */
public final class KotlinWriter {
    private final StringBuilder builder = new StringBuilder();
    private int indent;
    private boolean atLineStart = true;
    private final String unit;

    public KotlinWriter() {
        this("    ");
    }

    public KotlinWriter(String unit) {
        this.unit = unit;
    }

    public KotlinWriter(String unit, int initialIndent) {
        this.unit = unit;
        this.indent = Math.max(0, initialIndent);
    }

    public KotlinWriter line(String text) {
        if (text == null) {
            text = "";
        }
        if (!text.isEmpty()) {
            writeIndent();
            builder.append(text);
        }
        builder.append('\n');
        atLineStart = true;
        return this;
    }

    public KotlinWriter line() {
        builder.append('\n');
        atLineStart = true;
        return this;
    }

    public KotlinWriter append(String text) {
        if (text == null) {
            return this;
        }
        if (atLineStart && !text.isEmpty()) {
            writeIndent();
        }
        builder.append(text);
        atLineStart = false;
        return this;
    }

    public KotlinWriter indent() {
        indent++;
        return this;
    }

    public KotlinWriter dedent() {
        if (indent > 0) {
            indent--;
        }
        return this;
    }

    public int indentLevel() {
        return indent;
    }

    public void blankLine() {
        if (builder.length() == 0) {
            return;
        }
        int length = builder.length();
        if (length >= 2 && builder.charAt(length - 1) == '\n' && builder.charAt(length - 2) == '\n') {
            return;
        }
        builder.append('\n');
        atLineStart = true;
    }

    private void writeIndent() {
        builder.append(unit.repeat(Math.max(0, indent)));
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
