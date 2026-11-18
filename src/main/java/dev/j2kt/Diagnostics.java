package dev.j2kt;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Collects diagnostics and de-duplicates identical messages. */
public final class Diagnostics {
    private final List<Diagnostic> entries = new ArrayList<>();

    public void add(Diagnostic.Severity severity, String code, String message, Node node, String hint) {
        int line = 0;
        int column = 0;
        if (node != null) {
            Position begin = node.getBegin().orElse(null);
            if (begin != null) {
                line = begin.line;
                column = begin.column;
            }
        }
        entries.add(new Diagnostic(severity, code, message, line, column, hint));
    }

    public void error(String code, String message, Node node) {
        add(Diagnostic.Severity.ERROR, code, message, node, null);
    }

    public void error(String code, String message, Node node, String hint) {
        add(Diagnostic.Severity.ERROR, code, message, node, hint);
    }

    public void warn(String code, String message, Node node) {
        add(Diagnostic.Severity.WARNING, code, message, node, null);
    }

    public void warn(String code, String message, Node node, String hint) {
        add(Diagnostic.Severity.WARNING, code, message, node, hint);
    }

    public void info(String code, String message, Node node) {
        add(Diagnostic.Severity.INFO, code, message, node, null);
    }

    public List<Diagnostic> all() {
        return entries.stream()
                .sorted(Comparator.comparingInt(Diagnostic::line).thenComparingInt(Diagnostic::column))
                .toList();
    }

    public boolean hasErrors() {
        return entries.stream().anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
    }

    public int errorCount() {
        return (int) entries.stream().filter(d -> d.severity() == Diagnostic.Severity.ERROR).count();
    }

    public int warningCount() {
        return (int) entries.stream().filter(d -> d.severity() == Diagnostic.Severity.WARNING).count();
    }

    public void addAll(Diagnostics other) {
        if (other != null) {
            entries.addAll(other.entries);
        }
    }
}
