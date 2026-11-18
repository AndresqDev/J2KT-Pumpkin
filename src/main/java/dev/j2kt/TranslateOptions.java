package dev.j2kt;

import java.nio.file.Path;

/** Options that control a translation run. */
public record TranslateOptions(
        Target target,
        boolean useSymbolSolver,
        boolean strictJavaEquality,
        boolean convertProperties,
        boolean emitHeader,
        String classpath,
        Path sourceRoot,
        boolean compatPlugin
) {
    public static TranslateOptions defaults() {
        // The primary supported use case is Java -> Kotlin against the Pumpkin stub API.
        // Use withTarget(Target.JVM) for generic Java -> Kotlin/JVM translation.
        return new TranslateOptions(Target.PUMPKIN, true, true, true, true, "", null, true);
    }

    public TranslateOptions withTarget(Target value) {
        return new TranslateOptions(value, useSymbolSolver, strictJavaEquality, convertProperties, emitHeader, classpath, sourceRoot, compatPlugin);
    }

    public TranslateOptions withSymbolSolver(boolean value) {
        return new TranslateOptions(target, value, strictJavaEquality, convertProperties, emitHeader, classpath, sourceRoot, compatPlugin);
    }

    public TranslateOptions withStrictJavaEquality(boolean value) {
        return new TranslateOptions(target, useSymbolSolver, value, convertProperties, emitHeader, classpath, sourceRoot, compatPlugin);
    }

    public TranslateOptions withConvertProperties(boolean value) {
        return new TranslateOptions(target, useSymbolSolver, strictJavaEquality, value, emitHeader, classpath, sourceRoot, compatPlugin);
    }

    public TranslateOptions withClasspath(String value) {
        return new TranslateOptions(target, useSymbolSolver, strictJavaEquality, convertProperties, emitHeader, value, sourceRoot, compatPlugin);
    }

    public TranslateOptions withSourceRoot(Path value) {
        return new TranslateOptions(target, useSymbolSolver, strictJavaEquality, convertProperties, emitHeader, classpath, value, compatPlugin);
    }

    public TranslateOptions withCompatPlugin(boolean value) {
        return new TranslateOptions(target, useSymbolSolver, strictJavaEquality, convertProperties, emitHeader, classpath, sourceRoot, value);
    }
}
