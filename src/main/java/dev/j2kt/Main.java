package dev.j2kt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Command-line entry point. */
public final class Main {
    public static void main(String[] args) {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException failure) {
            System.err.println("error: " + failure.getMessage());
            System.err.println();
            usage();
            System.exit(2);
            return;
        }
        if (options.help) {
            usage();
            return;
        }
        if (options.inputs.isEmpty()) {
            System.err.println("error: no input files");
            usage();
            System.exit(2);
            return;
        }

        List<Path> sources = collectSources(options.inputs);
        if (sources.isEmpty()) {
            System.err.println("error: no .java files found");
            System.exit(2);
            return;
        }

        Translator translator = new Translator();
        Map<Path, List<Diagnostic>> diagnosticsByFile = new LinkedHashMap<>();
        int errors = 0;
        int warnings = 0;
        int written = 0;
        Map<Path, Path> outputs = new LinkedHashMap<>();

        for (Path source : sources) {
            Translator.Translation translation = translator.translate(source, options.translateOptions);
            diagnosticsByFile.put(source, translation.diagnostics().all());
            errors += translation.diagnostics().errorCount();
            warnings += translation.diagnostics().warningCount();

            if (translation.diagnostics().hasErrors() && translation.kotlin().isBlank()) {
                printDiagnostics(source, translation.diagnostics());
                continue;
            }

            if (options.stdout) {
                System.out.print(translation.kotlin());
                printDiagnostics(source, translation.diagnostics());
            } else {
                Path output = outputPath(source, options, translation.packageName());
                Path previous = outputs.putIfAbsent(output.toAbsolutePath().normalize(),
                        source.toAbsolutePath().normalize());
                if (previous != null) {
                    System.err.println("error: output collision for " + output
                            + ": " + previous + " and " + source.toAbsolutePath().normalize());
                    errors++;
                    printDiagnostics(source, translation.diagnostics());
                    continue;
                }
                try {
                    Path parent = output.toAbsolutePath().getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.writeString(output, translation.kotlin(), StandardCharsets.UTF_8);
                    written++;
                    System.out.println("wrote " + output);
                } catch (IOException failure) {
                    System.err.println("error: could not write " + output + ": " + failure.getMessage());
                    errors++;
                }
                printDiagnostics(source, translation.diagnostics());
            }
        }

        System.err.println();
        System.err.println("translated " + written + " file(s); " + errors + " error(s), " + warnings + " warning(s)");
        if (options.report != null) {
            writeReport(options.report, diagnosticsByFile);
        }
        if (errors > 0 || (options.strict && warnings > 0)) {
            System.exit(1);
        }
    }

    private static void printDiagnostics(Path source, Diagnostics diagnostics) {
        for (Diagnostic diagnostic : diagnostics.all()) {
            System.err.println(diagnostic.format(source));
        }
    }

    private static List<Path> collectSources(List<Path> inputs) {
        List<Path> result = new ArrayList<>();
        for (Path input : inputs) {
            if (Files.isDirectory(input)) {
                try (Stream<Path> walk = Files.walk(input)) {
                    walk.filter(path -> path.toString().endsWith(".java"))
                            .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                            .filter(path -> !path.getFileName().toString().equals("module-info.java"))
                            .sorted()
                            .forEach(result::add);
                } catch (IOException failure) {
                    System.err.println("warning: could not walk " + input + ": " + failure.getMessage());
                }
            } else if (Files.isRegularFile(input)) {
                result.add(input);
            } else {
                System.err.println("warning: input does not exist: " + input);
            }
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    private static Path outputPath(Path source, Options options, String packageName) {
        String fileName = source.getFileName().toString().replaceFirst("\\.java$", ".kt");
        if (options.outputDirectory == null) {
            return source.resolveSibling(fileName);
        }
        Path output = options.outputDirectory;
        if (packageName != null && !packageName.isBlank()) {
            output = output.resolve(packageName.replace('.', '/'));
        }
        return output.resolve(fileName);
    }

    private static void writeReport(Path report, Map<Path, List<Diagnostic>> diagnosticsByFile) {
        StringBuilder builder = new StringBuilder();
        builder.append("# j2kt-pumpkin porting report\n\n");
        builder.append("Generated by `j2kt-pumpkin`. This report lists places where the translation\n");
        builder.append("is not a mechanical syntax change and needs review.\n\n");
        boolean any = diagnosticsByFile.values().stream().anyMatch(list -> !list.isEmpty());
        if (!any) {
            builder.append("No diagnostics.\n");
        } else {
            for (Map.Entry<Path, List<Diagnostic>> entry : diagnosticsByFile.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                builder.append("## ").append(entry.getKey()).append("\n\n");
                for (Diagnostic diagnostic : entry.getValue()) {
                    builder.append(diagnostic.markdown(entry.getKey())).append('\n');
                }
                builder.append('\n');
            }
        }
        try {
            Path parent = report.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(report, builder.toString(), StandardCharsets.UTF_8);
            System.err.println("report written to " + report);
        } catch (IOException failure) {
            System.err.println("warning: could not write report: " + failure.getMessage());
        }
    }

    private static void usage() {
        System.out.println("""
                j2kt-pumpkin - Java to Kotlin translator with a Pumpkin/WASM porting pass

                Usage:
                  j2kt [options] <file.java | directory>...

                Options:
                  --out <dir>          Write .kt files under this directory
                  --stdout             Print Kotlin to stdout (single file recommended)
                  --target <pumpkin|jvm>
                                       pumpkin (default) emits against the Pumpkin
                                       Kotlin/stub API; jvm keeps ordinary Java APIs
                  --classpath <path>   Extra classpath for symbol resolution (: separated)
                  --no-symbols         Disable the JavaParser symbol solver
                  --value-equality     Translate Java == on objects to Kotlin == instead
                                       of the faithful === identity comparison
                  --no-properties      Keep getX()/setX() calls instead of Kotlin properties
                  --no-compat          Extend PumpkinPlugin instead of PumpkinCompatPlugin
                  --no-header          Do not emit the generated-file header
                  --strict             Exit with status 1 when there are warnings
                  --report <file.md>   Write a Markdown porting report
                  --help               Show this help

                Examples:
                  j2kt src/main/java/com/example/MyPlugin.java
                  j2kt --target pumpkin --out build/kotlin src/main/java
                  j2kt --stdout --no-symbols Example.java
                """);
    }

    private static final class Options {
        final List<Path> inputs = new ArrayList<>();
        Path outputDirectory;
        Path report;
        Path commonInputRoot;
        boolean stdout;
        boolean strict;
        boolean help;
        TranslateOptions translateOptions = TranslateOptions.defaults();

        static Options parse(String[] args) {
            Options options = new Options();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--help", "-h" -> options.help = true;
                    case "--stdout" -> options.stdout = true;
                    case "--strict" -> options.strict = true;
                    case "--no-symbols" -> options.translateOptions = options.translateOptions.withSymbolSolver(false);
                    case "--value-equality" -> options.translateOptions = options.translateOptions.withStrictJavaEquality(false);
                    case "--no-properties" -> options.translateOptions = options.translateOptions.withConvertProperties(false);
                    case "--no-compat" -> options.translateOptions = options.translateOptions.withCompatPlugin(false);
                    case "--no-header" -> options.translateOptions = new TranslateOptions(
                            options.translateOptions.target(),
                            options.translateOptions.useSymbolSolver(),
                            options.translateOptions.strictJavaEquality(),
                            options.translateOptions.convertProperties(),
                            false,
                            options.translateOptions.classpath(),
                            options.translateOptions.sourceRoot(),
                            options.translateOptions.compatPlugin());
                    case "--out" -> options.outputDirectory = Path.of(requireValue(args, ++i, "--out"));
                    case "--report" -> options.report = Path.of(requireValue(args, ++i, "--report"));
                    case "--classpath" -> options.translateOptions = options.translateOptions.withClasspath(
                            requireValue(args, ++i, "--classpath"));
                    case "--target" -> {
                        String value = requireValue(args, ++i, "--target");
                        Target target = switch (value.toLowerCase()) {
                            case "jvm" -> Target.JVM;
                            case "pumpkin", "wasm", "kotlin-wasm" -> Target.PUMPKIN;
                            default -> throw new IllegalArgumentException("unknown target: " + value);
                        };
                        options.translateOptions = options.translateOptions.withTarget(target);
                    }
                    default -> {
                        if (arg.startsWith("-")) {
                            throw new IllegalArgumentException("unknown option: " + arg);
                        }
                        options.inputs.add(Path.of(arg));
                    }
                }
            }
            if (!options.inputs.isEmpty()) {
                Path first = options.inputs.get(0).toAbsolutePath().normalize();
                if (Files.isDirectory(first)) {
                    options.commonInputRoot = first;
                } else if (first.getParent() != null) {
                    options.commonInputRoot = first.getParent();
                }
                options.translateOptions = options.translateOptions.withSourceRoot(options.commonInputRoot);
            }
            return options;
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("missing value for " + option);
            }
            return args[index];
        }
    }
}
