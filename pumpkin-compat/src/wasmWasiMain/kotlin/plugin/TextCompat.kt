package plugin

import pumpkin.Common
import pumpkin.Text

/** Small text-builders matching the ergonomics of Pumpkin's Rust text API. */
fun text(plain: String): Text.TextComponent = Text.TextComponent.text(plain)

fun String.toComponent(): Text.TextComponent = Text.TextComponent.text(this)

fun Text.TextComponent.bold(): Text.TextComponent = apply { bold(true) }

fun Text.TextComponent.italic(): Text.TextComponent = apply { italic(true) }

fun Text.TextComponent.underlined(): Text.TextComponent = apply { underlined(true) }

fun Text.TextComponent.color(color: Common.NamedColor): Text.TextComponent =
    apply { colorNamed(color) }

fun joinText(vararg components: Text.TextComponent): Text.TextComponent {
    val root = Text.TextComponent.text("")
    components.forEach { root.addChild(it) }
    return root
}
