package dev.j2kt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fast translation check for the Java sources used by the wasm integration
 * tests. The actual wasm build is exercised by scripts/test-wasm.sh.
 */
class WasmExamplesTranslateTest {
    private static final Path EXAMPLES = Path.of("examples/wasm");
    private static final Path TEMPLATE = Path.of("templates/intellij-plugin/java-plugin/src/main/java");

    @Test
    void allWasmExamplesTranslateWithoutErrors() throws IOException {
        List<Path> sources;
        try (Stream<Path> examples = Files.walk(EXAMPLES);
             Stream<Path> template = Files.walk(TEMPLATE)) {
            sources = Stream.concat(examples, template)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
        assertFalse(sources.isEmpty(), "no wasm example sources found");

        for (Path source : sources) {
            TranslateOptions options = TranslateOptions.defaults()
                    .withClasspath(Path.of("pumpkin-stub/build/classes/java/main").toAbsolutePath().toString())
                    .withSourceRoot(source.getParent())
                    .withCompatPlugin(true)
                    .withSymbolSolver(true);
            Translator.Translation translation = new Translator().translate(source, options);
            assertFalse(translation.diagnostics().hasErrors(),
                    () -> "translation errors for " + source + "\n"
                            + translation.diagnostics().all().stream()
                            .map(diagnostic -> diagnostic.format(translation.source()))
                            .reduce("", (a, b) -> a + "\n" + b));
            assertTrue(translation.kotlin().contains("package "),
                    () -> "missing Kotlin package for " + source);
        }
    }
}
