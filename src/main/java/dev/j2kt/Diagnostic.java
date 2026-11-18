package dev.j2kt;

import java.nio.file.Path;

/** A single problem found while translating a Java source file. */
public record Diagnostic(
        Severity severity,
        String code,
        String message,
        int line,
        int column,
        String hint
) {
    public enum Severity { ERROR, WARNING, INFO }

    public String location(Path file) {
        String name = file == null ? "<input>" : file.toString();
        if (line <= 0) {
            return name;
        }
        return name + ":" + line + (column > 0 ? ":" + column : "");
    }

    public String format(Path file) {
        String loc = location(file);
        String suffix = hint == null || hint.isBlank() ? "" : " Hint: " + hint;
        return "[" + severity + "] " + loc + " " + code + ": " + message + suffix;
    }

    public String markdown(Path file) {
        String loc = location(file);
        String hintText = hint == null || hint.isBlank() ? "" : " — " + hint;
        return "- **" + severity + "** `" + code + "` at `" + loc + "`: " + message + hintText;
    }
}
