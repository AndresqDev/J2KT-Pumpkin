package plugin

import pumpkin.World

/**
 * Block registry lookups. These are host-level functions on the generated
 * `World` interface, not instance methods of a loaded world. They never touch
 * world generation.
 */
fun blockByName(name: String): World.Block? = World.getBlockByName(name)

fun blockById(id: UShort): World.Block? = World.getBlockById(id)

fun allBlocks(): List<World.Block> = World.getAllBlocks()

fun statesOf(block: World.Block): List<World.BlockState> = World.getStatesForBlock(block)

fun statesOf(blockId: UShort): List<World.BlockState> = World.getStatesForBlockId(blockId)

fun stateIdsOf(blockId: UShort): List<UShort> = World.getStateIdsForBlockId(blockId)
