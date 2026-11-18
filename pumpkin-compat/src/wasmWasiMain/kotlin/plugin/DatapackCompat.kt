package plugin

import pumpkin.Datapack
import pumpkin.Server

/** Returns the server datapack manager. */
fun Server.Server.datapacks(): Datapack.DatapackManager = getDatapackManager()

fun Datapack.DatapackManager.allPacks(): List<Datapack.DatapackInfo> = listAllPacks()

fun Datapack.DatapackManager.enabledPacks(): List<Datapack.DatapackInfo> = listEnabledPacks()

fun Datapack.DatapackManager.availablePacks(): List<Datapack.DatapackInfo> = listAvailablePacks()

fun Datapack.DatapackManager.enable(
    name: String,
    position: Datapack.EnablePosition = Datapack.EnablePosition.Last,
): Result<Unit> = enablePack(name, position)

fun Datapack.DatapackManager.disable(name: String): Result<Unit> = disablePack(name)

fun Datapack.DatapackManager.reloadAll(): Result<Unit> = reload()

fun Datapack.DatapackManager.runFunction(name: String): Result<UInt> = executeFunction(name)
