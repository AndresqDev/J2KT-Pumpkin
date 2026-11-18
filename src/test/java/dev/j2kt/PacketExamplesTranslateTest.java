package dev.j2kt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the concrete Java/Bedrock packet mappings used by the wasm examples. */
class PacketExamplesTranslateTest {
    private static final Path SOURCES = Path.of("examples/wasm/packets");

    @Test
    void packetExamplesUseTheTypedGeneratedWrappers() throws IOException {
        List<Path> sources;
        try (Stream<Path> files = Files.list(SOURCES)) {
            sources = files.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        assertFalse(sources.isEmpty(), "no packet example sources found");

        StringBuilder kotlin = new StringBuilder();
        for (Path source : sources) {
            TranslateOptions options = TranslateOptions.defaults()
                    .withClasspath(Path.of("pumpkin-stub/build/classes/java/main").toAbsolutePath().toString())
                    .withSourceRoot(SOURCES)
                    .withCompatPlugin(true)
                    .withSymbolSolver(true);
            Translator.Translation translation = new Translator().translate(source, options);
            assertFalse(translation.diagnostics().hasErrors(),
                    () -> "translation errors for " + source + "\n"
                            + translation.diagnostics().all().stream()
                            .map(diagnostic -> diagnostic.format(translation.source()))
                            .reduce("", (a, b) -> a + "\n" + b));
            kotlin.append(translation.kotlin()).append('\n');
        }

        String generated = kotlin.toString();
        assertTrue(generated.contains("JavaPackets.ClientboundPacket.CCustomPayload"));
        assertTrue(generated.contains("JavaPackets.ClientboundPacket.CPlayDisconnect"));
        assertTrue(generated.contains("JavaPackets.ClientboundPacket.CKeepAlive"));
        assertTrue(generated.contains("JavaPackets.ClientboundPacket.CGameEvent"));
        assertTrue(generated.contains("JavaPackets.ClientboundPacket.CTransfer"));
        assertTrue(generated.contains("BedrockPackets.ClientboundPacket.CDisconnect"));
        assertTrue(generated.contains("BedrockPackets.ClientboundPacket.CSetTime"));
        assertTrue(generated.contains("BedrockPackets.ClientboundPacket.CModalFormRequest"));
        assertTrue(generated.contains("BedrockPackets.ClientboundPacket.Unknown"));
        assertTrue(generated.contains("Event.Event.PlayerCustomPayloadEvent"));
    }
}
