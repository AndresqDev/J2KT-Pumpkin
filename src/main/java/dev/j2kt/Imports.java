package dev.j2kt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal import manager. It keeps original imports that are still reachable,
 * adds imports discovered while emitting, and falls back to a fully-qualified
 * name when two types share a simple name.
 */
public final class Imports {
    private static final Set<String> DEFAULT_PACKAGES = Set.of(
            "kotlin",
            "kotlin.annotation",
            "kotlin.collections",
            "kotlin.comparisons",
            "kotlin.io",
            "kotlin.ranges",
            "kotlin.sequences",
            "kotlin.text"
    );

    private final String currentPackage;
    private final Map<String, String> simpleToFq = new LinkedHashMap<>();
    private final Set<String> usedFq = new LinkedHashSet<>();
    private final Set<String> rawImports = new LinkedHashSet<>();
    private final Set<String> wildcards = new LinkedHashSet<>();

    public Imports(String currentPackage) {
        this.currentPackage = currentPackage == null ? "" : currentPackage;
    }

    /** Registers an import and returns the name to use in source. */
    public String ref(String fqName) {
        if (fqName == null || fqName.isBlank()) {
            return "Any?";
        }
        if (fqName.startsWith("kotlin.") && DEFAULT_PACKAGES.contains(packageOf(fqName))) {
            return simpleName(fqName);
        }
        if (fqName.startsWith("java.lang.")) {
            return simpleName(fqName);
        }
        if (packageOf(fqName).equals(currentPackage)) {
            return simpleName(fqName);
        }
        if (!currentPackage.isEmpty() && fqName.startsWith(currentPackage + ".")) {
            String rest = fqName.substring(currentPackage.length() + 1);
            int dot = rest.indexOf('.');
            String firstSegment = dot < 0 ? rest : rest.substring(0, dot);
            // Same-package top-level/nested class (com.example.Outer.Inner); do not import.
            if (!firstSegment.isEmpty() && Character.isUpperCase(firstSegment.charAt(0))) {
                return simpleName(fqName);
            }
        }
        String simple = simpleName(fqName);
        String existing = simpleToFq.get(simple);
        if (existing == null) {
            simpleToFq.put(simple, fqName);
            existing = fqName;
        }
        if (!existing.equals(fqName)) {
            return fqName;
        }
        usedFq.add(fqName);
        return simple;
    }

    /** Preserves an import that the emitter does not model as a type. */
    public void preserve(String fqName) {
        if (fqName == null || fqName.isBlank() || fqName.endsWith(".*")) {
            return;
        }
        if (fqName.startsWith("java.lang.")) {
            return;
        }
        simpleToFq.putIfAbsent(simpleName(fqName), fqName);
        usedFq.add(fqName);
    }

    /** Adds a verbatim import line such as an extension or a static member import. */
    public void raw(String fqName) {
        if (fqName == null || fqName.isBlank()) {
            return;
        }
        rawImports.add(fqName);
    }

    public void wildcard(String packageName) {
        if (packageName != null && !packageName.isBlank()) {
            wildcards.add(packageName);
        }
    }

    /** Records a simple-name resolution without emitting an import. */
    public void alias(String simpleName, String fqName) {
        if (simpleName != null && fqName != null && !simpleName.isBlank() && !fqName.isBlank()) {
            simpleToFq.putIfAbsent(simpleName, fqName);
        }
    }

    /** Returns the fully-qualified name already known for a simple name, if any. */
    public String lookup(String simpleName) {
        return simpleToFq.get(simpleName);
    }

    public String currentPackage() {
        return currentPackage;
    }

    /** Qualifies a simple source name using known imports or the current package. */
    public String qualify(String name) {
        if (name == null || name.isBlank() || name.contains(".") || name.contains("<")) {
            return name;
        }
        String known = simpleToFq.get(name);
        if (known != null) {
            return known;
        }
        return currentPackage.isEmpty() ? name : currentPackage + "." + name;
    }

    public List<String> render() {
        Set<String> lines = new LinkedHashSet<>();
        for (String fq : usedFq) {
            if (skip(fq)) {
                continue;
            }
            lines.add("import " + fq);
        }
        for (String fq : rawImports) {
            if (fq.startsWith("import ")) {
                lines.add(fq);
            } else {
                lines.add("import " + fq);
            }
        }
        for (String pkg : wildcards) {
            lines.add("import " + pkg + ".*");
        }
        List<String> sorted = new ArrayList<>(lines);
        sorted.sort(String::compareTo);
        return sorted;
    }

    private boolean skip(String fq) {
        String pkg = packageOf(fq);
        if (DEFAULT_PACKAGES.contains(pkg)) {
            return true;
        }
        if (pkg.equals("java.lang")) {
            return true;
        }
        return pkg.equals(currentPackage);
    }

    public static String simpleName(String fq) {
        int dot = fq.lastIndexOf('.');
        return dot < 0 ? fq : fq.substring(dot + 1);
    }

    public static String packageOf(String fq) {
        int dot = fq.lastIndexOf('.');
        return dot < 0 ? "" : fq.substring(0, dot);
    }
}
