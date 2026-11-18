package plugin

import pumpkin.ItemStack

/** Item construction helper using a normal Kotlin Int for the count. */
fun item(registryKey: String, count: Int = 1): ItemStack.ItemStack =
    ItemStack.ItemStack(registryKey, count.toUByte())

fun ItemStack.ItemStack.countInt(): Int = getCount().toInt()

fun ItemStack.ItemStack.withCount(count: Int): ItemStack.ItemStack =
    apply { setCount(count.toUByte()) }
