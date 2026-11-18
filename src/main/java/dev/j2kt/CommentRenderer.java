package dev.j2kt;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts Java comments and Javadoc into Kotlin comments/KDoc. */
public final class CommentRenderer {
    private static final Pattern CODE_TAG = Pattern.compile("\\{@code ([^}]+)}");
    private static final Pattern LINK_TAG = Pattern.compile("\\{@link(?:plain)? ([^}]+)}");
    private static final Pattern LITERAL_TAG = Pattern.compile("\\{@literal ([^}]+)}");
    private static final Pattern INLINE_TAG = Pattern.compile("\\{@[a-zA-Z]+ ([^}]+)}");

    private final KotlinWriter out;

    public CommentRenderer(KotlinWriter out) {
        this.out = out;
    }

    /** Emits a leading non-Javadoc comment, if one is attached. */
    public void leading(Node node) {
        if (node == null) {
            return;
        }
        Optional<Comment> comment = node.getComment();
        if (comment.isEmpty() || comment.get() instanceof JavadocComment) {
            return;
        }
        Comment value = comment.get();
        if (value instanceof LineComment line) {
            String text = line.getContent().trim();
            if (!text.isEmpty()) {
                out.line("// " + text);
            }
        } else if (value instanceof BlockComment block) {
            String[] lines = block.getContent().split("\\R", -1);
            out.line("/*");
            for (String line : lines) {
                out.line(" * " + line.strip());
            }
            out.line(" */");
        }
    }

    /** Emits Javadoc as KDoc. */
    public void javadoc(Node node) {
        if (node == null) {
            return;
        }
        Optional<Comment> comment = node.getComment();
        if (comment.isEmpty() || !(comment.get() instanceof JavadocComment javadoc)) {
            return;
        }
        String content = normalize(javadoc.getContent());
        if (content.isBlank()) {
            return;
        }
        String[] lines = content.split("\\R", -1);
        out.line("/**");
        for (String line : lines) {
            String trimmed = line.stripTrailing();
            if (trimmed.isBlank()) {
                out.line(" *");
            } else {
                out.line(" * " + trimmed);
            }
        }
        out.line(" */");
    }

    private String normalize(String raw) {
        String[] lines = raw.split("\\R", -1);
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String value = line.strip();
            if (value.startsWith("*")) {
                value = value.substring(1);
                if (value.startsWith(" ")) {
                    value = value.substring(1);
                }
            }
            result.append(value).append('\n');
        }
        String text = result.toString();
        text = text.replace("\r", "");
        text = replace(CODE_TAG, text, "`", "`");
        text = replace(LINK_TAG, text, "[", "]");
        text = replace(LITERAL_TAG, text, "", "");
        text = replace(INLINE_TAG, text, "", "");
        text = text.replace("<p>", "\n\n").replace("<p/>", "\n\n").replace("</p>", "")
                .replace("<br>", "\n");
        text = text.replace("<b>", "**").replace("</b>", "**");
        text = text.replace("<i>", "*").replace("</i>", "*");
        text = text.replace("<code>", "`").replace("</code>", "`");
        text = text.replace("<pre>", "```\n").replace("</pre>", "\n```");
        text = text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.strip();
    }

    private String replace(Pattern pattern, String text, String prefix, String suffix) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(prefix + matcher.group(1) + suffix));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
