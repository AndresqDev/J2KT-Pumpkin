package plugin

import pumpkin.Forms
import pumpkin.Player
import pumpkin.Text

/** Builds a Bedrock simple form. */
fun simpleForm(
    title: String,
    content: String,
    block: SimpleFormBuilder.() -> Unit,
): Forms.Form {
    val builder = SimpleFormBuilder(title, content)
    builder.block()
    return Forms.Form.Simple(builder.build())
}

class SimpleFormBuilder internal constructor(
    private val title: String,
    private val content: String,
) {
    private val buttons = mutableListOf<Forms.SimpleFormButton>()

    fun button(text: String, image: Forms.FormImage? = null): SimpleFormBuilder {
        buttons += Forms.SimpleFormButton(Text.TextComponent.text(text), image)
        return this
    }

    internal fun build(): Forms.SimpleForm = Forms.SimpleForm(
        Text.TextComponent.text(title),
        Text.TextComponent.text(content),
        buttons,
    )
}

fun modalForm(
    title: String,
    content: String,
    button1: String,
    button2: String,
): Forms.Form = Forms.Form.Modal(
    Forms.ModalForm(
        Text.TextComponent.text(title),
        Text.TextComponent.text(content),
        Text.TextComponent.text(button1),
        Text.TextComponent.text(button2),
    )
)

fun customForm(
    title: String,
    block: CustomFormBuilder.() -> Unit,
): Forms.Form {
    val builder = CustomFormBuilder(title)
    builder.block()
    return Forms.Form.Custom(builder.build())
}

class CustomFormBuilder internal constructor(private val title: String) {
    private val elements = mutableListOf<Forms.CustomFormElement>()

    fun label(text: String): CustomFormBuilder {
        elements += Forms.CustomFormElement.Label(Text.TextComponent.text(text))
        return this
    }

    fun toggle(text: String, initial: Boolean = false): CustomFormBuilder {
        elements += Forms.CustomFormElement.Toggle(
            Text.TextComponent.text(text) to initial
        )
        return this
    }

    fun input(text: String, placeholder: String = "", initial: String = ""): CustomFormBuilder {
        elements += Forms.CustomFormElement.Input(
            Triple(Text.TextComponent.text(text), placeholder, initial)
        )
        return this
    }

    fun dropdown(text: String, options: List<String>, initial: UInt = 0u): CustomFormBuilder {
        elements += Forms.CustomFormElement.Dropdown(
            Triple(Text.TextComponent.text(text), options, initial)
        )
        return this
    }

    fun slider(
        text: String,
        min: Float,
        max: Float,
        initial: Float,
        step: Float,
    ): CustomFormBuilder {
        elements += Forms.CustomFormElement.Slider(
            pumpkin.runtime.Tuple5(Text.TextComponent.text(text), min, max, initial, step)
        )
        return this
    }

    fun stepSlider(
        text: String,
        options: List<String>,
        initial: UInt = 0u,
    ): CustomFormBuilder {
        elements += Forms.CustomFormElement.StepSlider(
            Triple(Text.TextComponent.text(text), options, initial)
        )
        return this
    }

    internal fun build(): Forms.CustomForm = Forms.CustomForm(
        Text.TextComponent.text(title),
        elements,
    )
}

/** Opens a Bedrock form and returns the form id. */
fun Player.BedrockPlayer.open(form: Forms.Form): UInt = openForm(form)
