package plugin

import pumpkin.Enchantments
import pumpkin.Server
import pumpkin.Text

/** Returns the server enchantment manager. */
fun Server.Server.enchantments(): Enchantments.EnchantmentManager = getEnchantmentManager()

fun customEnchantment(
    id: String,
    description: String,
    maxLevel: UInt = 1u,
    anvilCost: UInt = 1u,
    supportedItems: String = "#minecraft:enchantable/durability",
    weight: UInt = 5u,
    slots: List<Enchantments.AttributeModifierSlot> = listOf(
        Enchantments.AttributeModifierSlot.ANY
    ),
    exclusiveSet: List<String> = emptyList(),
): Enchantments.CustomEnchantment = Enchantments.CustomEnchantment(
    id = id,
    description = Text.TextComponent.text(description),
    maxLevel = maxLevel,
    anvilCost = anvilCost,
    supportedItems = supportedItems,
    weight = weight,
    slots = slots,
    exclusiveSet = exclusiveSet,
)

fun Enchantments.EnchantmentManager.register(
    enchantment: Enchantments.CustomEnchantment,
): Result<Unit> = registerEnchantment(enchantment)
