package dev.j2kt;

import java.nio.file.Path;
import java.util.List;

/**
 * Embeddable facade for the translator.
 *
 * <pre>{@code
 * J2kt.Result result = J2kt.translate(javaSource);
 * if (!result.diagnostics().hasErrors()) {
 *     Files.writeString(kotlinFile, result.kotlin());
 * }
 * }</pre>
 */
public final class J2kt {
    public record Result(String kotlin, Diagnostics diagnostics) {
        public List<Diagnostic> problems() {
            return diagnostics.all();
        }
    }

    private J2kt() {
    }

    public static Result translate(String javaSource) {
        return translate(javaSource, TranslateOptions.defaults());
    }

    public static Result translate(String javaSource, TranslateOptions options) {
        Translator.Translation translation = new Translator().translate(javaSource, "Source.java", options);
        return new Result(translation.kotlin(), translation.diagnostics());
    }

    public static Result translate(Path source) {
        return translate(source, TranslateOptions.defaults());
    }

    public static Result translate(Path source, TranslateOptions options) {
        Translator.Translation translation = new Translator().translate(source, options);
        return new Result(translation.kotlin(), translation.diagnostics());
    }
}
