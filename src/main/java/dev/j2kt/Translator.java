package dev.j2kt;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Parses Java and runs the Kotlin emitter. */
public final class Translator {
    public record Translation(String kotlin, Diagnostics diagnostics, Path source, String packageName) {
    }

    public Translation translate(Path source, TranslateOptions options) {
        Diagnostics diagnostics = new Diagnostics();
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setAttributeComments(true);

        if (options.useSymbolSolver()) {
            configureSymbolSolver(configuration, source, options, diagnostics);
        }

        JavaParser parser = new JavaParser(configuration);
        try {
            ParseResult<CompilationUnit> result = parser.parse(source);
            return finish(result, source, options, diagnostics);
        } catch (IOException failure) {
            diagnostics.error("J2KT-IO-001", "Could not read " + source + ": " + failure.getMessage(), null);
            return new Translation("", diagnostics, source, "");
        }
    }

    /** Translates an in-memory Java source. Useful when embedding j2kt as a library. */
    public Translation translate(String javaSource, String fileName, TranslateOptions options) {
        Diagnostics diagnostics = new Diagnostics();
        Path source = Path.of(fileName == null || fileName.isBlank() ? "Source.java" : fileName);
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setAttributeComments(true);
        if (options.useSymbolSolver()) {
            configureSymbolSolver(configuration, source, options, diagnostics);
        }
        JavaParser parser = new JavaParser(configuration);
        return finish(parser.parse(javaSource), source, options, diagnostics);
    }

    private Translation finish(ParseResult<CompilationUnit> result, Path source,
                               TranslateOptions options, Diagnostics diagnostics) {
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            for (var problem : result.getProblems()) {
                diagnostics.error("J2KT-PARSE-001", problem.getVerboseMessage(), null,
                        problem.getLocation().map(Object::toString).orElse(null));
            }
            return new Translation("", diagnostics, source, "");
        }
        CompilationUnit unit = result.getResult().orElseThrow();
        String packageName = unit.getPackageDeclaration()
                .map(declaration -> declaration.getNameAsString())
                .orElse("");
        KotlinEmitter emitter = new KotlinEmitter(options.target(), options, diagnostics, packageName);
        String kotlin = emitter.emit(unit);
        return new Translation(kotlin, diagnostics, source, packageName);
    }

    private void configureSymbolSolver(ParserConfiguration configuration, Path source,
                                       TranslateOptions options, Diagnostics diagnostics) {
        try {
            CombinedTypeSolver solver = new CombinedTypeSolver();
            solver.add(new ReflectionTypeSolver(true));

            Path sourceRoot = options.sourceRoot();
            if (sourceRoot == null) {
                sourceRoot = source.toAbsolutePath().getParent();
            }
            if (sourceRoot != null && Files.isDirectory(sourceRoot)) {
                solver.add(new JavaParserTypeSolver(sourceRoot));
            }

            if (options.classpath() != null && !options.classpath().isBlank()) {
                for (String entry : options.classpath().split(java.io.File.pathSeparator)) {
                    if (entry.isBlank()) {
                        continue;
                    }
                    Path path = Path.of(entry);
                    if (!Files.exists(path)) {
                        diagnostics.warn("J2KT-SYMBOL-001", "Classpath entry does not exist: " + entry, null);
                        continue;
                    }
                    if (Files.isDirectory(path)) {
                        solver.add(new JavaParserTypeSolver(path));
                    } else if (entry.endsWith(".jar")) {
                        solver.add(new JarTypeSolver(path));
                    }
                }
            }

            configuration.setSymbolResolver(new JavaSymbolSolver(solver));
        } catch (Throwable failure) {
            diagnostics.warn("J2KT-SYMBOL-002",
                    "Symbol solver could not be configured; falling back to local inference. " + failure.getMessage(),
                    null);
        }
    }
}
