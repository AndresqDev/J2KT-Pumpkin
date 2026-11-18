package plugin

import pumpkin.Display
import pumpkin.EntityTypes
import pumpkin.ItemStack
import pumpkin.Text
import pumpkin.World

fun World.World.spawnBlockDisplay(position: Triple<Double, Double, Double>): Display.BlockDisplayEntity =
    Display.BlockDisplayEntity.fromEntity(
        spawnEntity(EntityTypes.EntityType.BLOCK_DISPLAY, position)
    ) ?: error("spawnEntity did not return a block display")

fun World.World.spawnItemDisplay(position: Triple<Double, Double, Double>): Display.ItemDisplayEntity =
    Display.ItemDisplayEntity.fromEntity(
        spawnEntity(EntityTypes.EntityType.ITEM_DISPLAY, position)
    ) ?: error("spawnEntity did not return an item display")

fun World.World.spawnTextDisplay(position: Triple<Double, Double, Double>): Display.TextDisplayEntity =
    Display.TextDisplayEntity.fromEntity(
        spawnEntity(EntityTypes.EntityType.TEXT_DISPLAY, position)
    ) ?: error("spawnEntity did not return a text display")

fun World.Entity.asBlockDisplay(): Display.BlockDisplayEntity? =
    Display.BlockDisplayEntity.fromEntity(this)

fun World.Entity.asItemDisplay(): Display.ItemDisplayEntity? =
    Display.ItemDisplayEntity.fromEntity(this)

fun World.Entity.asTextDisplay(): Display.TextDisplayEntity? =
    Display.TextDisplayEntity.fromEntity(this)

fun Display.ItemDisplayEntity.setDisplayedItem(item: ItemStack.ItemStack?) {
    setItem(item)
}

fun Display.TextDisplayEntity.setText(text: String) {
    setText(Text.TextComponent.text(text))
}
