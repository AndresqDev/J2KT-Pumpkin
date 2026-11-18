package plugin

import pumpkin.Gui
import pumpkin.ItemStack
import pumpkin.Player
import pumpkin.Screens
import pumpkin.Text

/** Creates a GUI using a text title. */
fun gui(type: Screens.Screen, title: String): Gui.Gui =
    Gui.Gui(type, Text.TextComponent.text(title))

/** Creates a GUI and applies a builder block. */
fun gui(
    type: Screens.Screen,
    title: String,
    block: GuiBuilder.() -> Unit,
): Gui.Gui = GuiBuilder(type, title).apply(block).build()

class GuiBuilder internal constructor(
    private val type: Screens.Screen,
    title: String,
) {
    private val gui = Gui.Gui(type, Text.TextComponent.text(title))

    fun item(slot: Int, item: ItemStack.ItemStack?): GuiBuilder {
        if (item == null) {
            gui.getInventory().setItem(slot.toUInt(), null)
        } else {
            gui.setItem(slot.toUInt(), item)
        }
        return this
    }

    fun clear(): GuiBuilder = apply { gui.clearItems() }

    fun allowGrab(allow: Boolean): GuiBuilder = apply { gui.setAllowGrabItems(allow) }

    fun allowPut(allow: Boolean): GuiBuilder = apply { gui.setAllowPutItems(allow) }

    fun build(): Gui.Gui = gui
}

/** Opens this GUI for the player. */
fun Player.Player.open(gui: Gui.Gui) {
    openGui(gui)
}
