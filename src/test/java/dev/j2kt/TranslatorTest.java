package dev.j2kt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslatorTest {
    private static final Path CASES = Path.of("src/test/resources/cases");

    record Case(Path directory, String name) {
    }

    static Stream<Case> cases() throws IOException {
        try (Stream<Path> directories = Files.list(CASES)) {
            return directories.filter(Files::isDirectory)
                    .sorted()
                    .map(path -> new Case(path, path.getFileName().toString()))
                    .toList()
                    .stream();
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("cases")
    void translatesGoldenCases(Case testCase) throws IOException {
        Path input = testCase.directory().resolve("input.java");
        Path expectedFile = testCase.directory().resolve("expected.kt");
        assertTrue(Files.exists(input), "missing " + input);

        TranslateOptions options = readOptions(testCase.directory());
        Translator.Translation translation = new Translator().translate(input, options);

        assertFalse(translation.diagnostics().hasErrors(),
                () -> "unexpected errors: " + formatDiagnostics(translation));

        String actual = normalize(translation.kotlin());
        if (System.getenv("UPDATE_GOLDEN") != null) {
            Files.writeString(expectedFile, actual, StandardCharsets.UTF_8);
            return;
        }
        assertTrue(Files.exists(expectedFile), "missing " + expectedFile + " (run with UPDATE_GOLDEN=1)");
        String expected = normalize(Files.readString(expectedFile, StandardCharsets.UTF_8));
        assertEquals(expected, actual, () -> "golden mismatch for " + testCase.name()
                + "\n--- expected ---\n" + expected + "\n--- actual ---\n" + actual);
    }

    @Test
    void defaultTargetIsThePumpkinStubApi() {
        assertEquals(Target.PUMPKIN, TranslateOptions.defaults().target());
    }

    @Test
    void embeddableFacadeReturnsKotlinAndDiagnostics() {
        String java = """
                package demo;
                public class Hello {
                    public static int answer() {
                        return 40 + 2;
                    }
                }
                """;
        J2kt.Result result = J2kt.translate(java,
                TranslateOptions.defaults().withTarget(Target.JVM).withSymbolSolver(false));
        assertFalse(result.diagnostics().hasErrors(), () -> formatDiagnostics(result.diagnostics()));
        assertTrue(result.kotlin().contains("fun answer(): Int"));
        assertTrue(result.kotlin().contains("return 40 + 2"));
    }

    private static TranslateOptions readOptions(Path directory) throws IOException {
        Properties properties = new Properties();
        Path file = directory.resolve("options.properties");
        if (Files.exists(file)) {
            try (var reader = Files.newBufferedReader(file)) {
                properties.load(reader);
            }
        }
        Target target = "pumpkin".equalsIgnoreCase(properties.getProperty("target", "jvm"))
                ? Target.PUMPKIN
                : Target.JVM;
        boolean symbols = Boolean.parseBoolean(properties.getProperty("symbols", "false"));
        boolean header = Boolean.parseBoolean(properties.getProperty("header", "false"));
        boolean strictEquality = Boolean.parseBoolean(properties.getProperty("strictEquality", "true"));
        boolean propertiesConversion = Boolean.parseBoolean(properties.getProperty("properties", "true"));
        boolean compatPlugin = Boolean.parseBoolean(properties.getProperty("compat", "true"));
        return new TranslateOptions(target, symbols, strictEquality, propertiesConversion,
                header, "", directory, compatPlugin);
    }

    private static String normalize(String text) {
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            result.append(line.stripTrailing()).append('\n');
        }
        String normalized = result.toString();
        while (normalized.endsWith("\n\n")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String formatDiagnostics(Translator.Translation translation) {
        return translation.diagnostics().all().stream()
                .map(diagnostic -> diagnostic.format(translation.source()))
                .reduce("", (a, b) -> a + "\n" + b);
    }

    private static String formatDiagnostics(Diagnostics diagnostics) {
        List<Diagnostic> all = diagnostics.all();
        return all.stream().map(Diagnostic::toString).reduce("", (a, b) -> a + "\n" + b);
    }
}
