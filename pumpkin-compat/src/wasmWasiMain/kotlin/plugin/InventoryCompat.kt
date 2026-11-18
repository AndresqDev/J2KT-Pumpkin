package plugin

import pumpkin.Inventory
import pumpkin.ItemStack
import pumpkin.Player

/** Inventory helpers that accept Kotlin Int slots. */
fun Inventory.Inventory.getItem(slot: Int): ItemStack.ItemStack? = getItem(slot.toUInt())

fun Inventory.Inventory.setItem(slot: Int, item: ItemStack.ItemStack?) {
    setItem(slot.toUInt(), item)
}

fun Inventory.Inventory.removeItem(slot: Int): ItemStack.ItemStack? = removeItem(slot.toUInt())

fun Inventory.Inventory.size(): Int = getSize().toInt()

fun Inventory.Inventory.count(itemId: String): Int = countItem(itemId).toInt()

fun Inventory.Inventory.contains(itemId: String): Boolean = containsItem(itemId)

fun Inventory.Inventory.setAll(items: List<ItemStack.ItemStack?>) {
    setAllItems(items)
}

/** Player-inventory helpers that keep the public API in normal Int/Boolean terms. */
fun Inventory.PlayerInventory.selectedSlot(): Int = getSelectedSlot().toInt()

fun Inventory.PlayerInventory.selectSlot(slot: Int) {
    setSelectedSlot(slot.toUByte())
}

fun Inventory.PlayerInventory.helmet(): ItemStack.ItemStack? = getHelmet()
fun Inventory.PlayerInventory.chestplate(): ItemStack.ItemStack? = getChestplate()
fun Inventory.PlayerInventory.leggings(): ItemStack.ItemStack? = getLeggings()
fun Inventory.PlayerInventory.boots(): ItemStack.ItemStack? = getBoots()
fun Inventory.PlayerInventory.offHand(): ItemStack.ItemStack? = getOffHand()

fun Inventory.PlayerInventory.equipHelmet(item: ItemStack.ItemStack?) = setHelmet(item)
fun Inventory.PlayerInventory.equipChestplate(item: ItemStack.ItemStack?) = setChestplate(item)
fun Inventory.PlayerInventory.equipLeggings(item: ItemStack.ItemStack?) = setLeggings(item)
fun Inventory.PlayerInventory.equipBoots(item: ItemStack.ItemStack?) = setBoots(item)
fun Inventory.PlayerInventory.equipOffHand(item: ItemStack.ItemStack?) = setOffHand(item)
