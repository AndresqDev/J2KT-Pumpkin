package plugin

import pumpkin.ItemStack
import pumpkin.JavaDialogs
import pumpkin.Player
import pumpkin.Text

/** Builds a Java Edition dialog. */
fun dialog(
    title: String,
    type: JavaDialogs.DialogType = JavaDialogs.DialogType.NOTICE,
    block: DialogBuilder.() -> Unit,
): JavaDialogs.Dialog {
    val builder = DialogBuilder(title, type)
    builder.block()
    return builder.build()
}

class DialogBuilder internal constructor(
    private val title: String,
    private val type: JavaDialogs.DialogType,
) {
    private val body = mutableListOf<JavaDialogs.DialogBody>()
    private val inputs = mutableListOf<JavaDialogs.DialogInput>()
    private val buttons = mutableListOf<JavaDialogs.ActionButton>()
    private val links = mutableListOf<JavaDialogs.Link>()

    var afterAction: JavaDialogs.AfterAction? = null
    var canCloseWithEscape: Boolean = true
    var externalTitle: Text.TextComponent? = null

    fun message(text: String): DialogBuilder {
        body += JavaDialogs.DialogBody.PlainMessage(Text.TextComponent.text(text))
        return this
    }

    fun item(item: ItemStack.ItemStack): DialogBuilder {
        body += JavaDialogs.DialogBody.Item(item)
        return this
    }

    fun inputBool(label: String, initial: Boolean = false): DialogBuilder {
        inputs += JavaDialogs.DialogInput.Bool(
            JavaDialogs.DialogInputBool(Text.TextComponent.text(label), initial)
        )
        return this
    }

    fun inputText(label: String, placeholder: String = "", initial: String = ""): DialogBuilder {
        inputs += JavaDialogs.DialogInput.Text(
            JavaDialogs.DialogInputText(
                Text.TextComponent.text(label),
                Text.TextComponent.text(placeholder),
                initial,
            )
        )
        return this
    }

    fun inputNumber(
        label: String,
        min: Float,
        max: Float,
        initial: Float,
        step: Float,
        labelFormat: String? = null,
    ): DialogBuilder {
        inputs += JavaDialogs.DialogInput.NumberRange(
            JavaDialogs.DialogInputNumberRange(
                Text.TextComponent.text(label), min, max, initial, step, labelFormat
            )
        )
        return this
    }

    fun inputOption(label: String, options: List<String>, initialIndex: UInt = 0u): DialogBuilder {
        inputs += JavaDialogs.DialogInput.SingleOption(
            JavaDialogs.DialogInputSingleOption(
                Text.TextComponent.text(label),
                options.map { Text.TextComponent.text(it) },
                initialIndex,
            )
        )
        return this
    }

    fun button(
        text: String,
        action: JavaDialogs.Action,
        tooltip: String? = null,
        width: UInt? = null,
    ): DialogBuilder {
        buttons += JavaDialogs.ActionButton(
            Text.TextComponent.text(text),
            tooltip?.let { Text.TextComponent.text(it) },
            width,
            action,
        )
        return this
    }

    fun openUrlButton(text: String, url: String): DialogBuilder =
        button(text, JavaDialogs.Action.OpenUrl(url))

    fun customButton(text: String, id: String, payload: List<UByte>? = null): DialogBuilder =
        button(text, JavaDialogs.Action.CustomClick(JavaDialogs.CustomClickAction(id, payload)))

    fun link(label: String, url: String): DialogBuilder {
        links += JavaDialogs.Link(
            JavaDialogs.LinkLabel.Custom(Text.TextComponent.text(label)),
            url,
        )
        return this
    }

    fun builtInLink(type: JavaDialogs.LinkType, url: String): DialogBuilder {
        links += JavaDialogs.Link(JavaDialogs.LinkLabel.BuiltIn(type), url)
        return this
    }

    internal fun build(): JavaDialogs.Dialog = JavaDialogs.Dialog(
        title = Text.TextComponent.text(title),
        type = type,
        body = body,
        inputs = inputs,
        buttons = buttons,
        links = links,
        afterAction = afterAction,
        canCloseWithEscape = canCloseWithEscape,
        externalTitle = externalTitle,
    )
}

/** Shows this dialog to a Java Edition player. */
fun JavaDialogs.Dialog.show(player: Player.JavaPlayer) {
    player.showDialog(this)
}
