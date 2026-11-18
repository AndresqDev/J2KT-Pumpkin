package pumpkin

/**
 * JVM-only shape fixture for the generated Pumpkin Kotlin API.
 *
 * It mirrors the nested-interface layout of `pumpkin-api-kt` closely enough to
 * type-check generated plugin code with `kotlinc`. It is intentionally not a
 * runtime implementation.
 */

interface Logging {
    enum class Level { TRACE, DEBUG, INFO, WARN, ERROR }
    fun log(level: Level, message: String)
    companion object : Logging {
        override fun log(level: Level, message: String) {}
    }
}

interface Text {
    class TextComponent {
        fun getText(): String = ""
        fun addChild(child: TextComponent) {}
        fun bold(value: Boolean) {}
        fun italic(value: Boolean) {}
        fun underlined(value: Boolean) {}
        fun colorNamed(color: pumpkin.Common.NamedColor) {}
        companion object {
            fun text(plain: String): TextComponent = TextComponent()
        }
    }
}

interface Player {
    class Player {
        fun getName(): String = ""
        fun getPosition(): Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
        fun getWorld(): World.World = World.World()
        fun sendSystemMessage(text: Text.TextComponent, overlay: Boolean) {}
        fun teleport(
            position: Triple<Double, Double, Double>,
            yaw: Float?,
            pitch: Float?,
            world: World.World,
        ) {
        }
        fun asJava(): JavaPlayer? = null
        fun asBedrock(): BedrockPlayer? = null
        fun getInventory(): pumpkin.Inventory.Inventory = pumpkin.Inventory.Inventory()
        fun openGui(gui: pumpkin.Gui.Gui) {}
    }

    class JavaPlayer {
        fun sendPacket(packet: JavaPackets.ClientboundPacket) {}
        fun showDialog(dialog: JavaDialogs.Dialog) {}
    }

    class BedrockPlayer {
        fun sendPacket(packet: BedrockPackets.ClientboundPacket) {}
        fun openForm(form: Forms.Form): UInt = 0u
    }
}

interface JavaPackets {
    class CCustomPayload(var channel: String, var data: List<UByte>)
    sealed interface ClientboundPacket {
        class CCustomPayload(val value: pumpkin.JavaPackets.CCustomPayload) : ClientboundPacket
    }
}

interface BedrockPackets {
    sealed interface ClientboundPacket {
        object Unknown : ClientboundPacket
    }
}

interface World {
    class Block(
        var id: UShort = 0u,
        var name: String = "",
        var hardness: Float = 0f,
        var blastResistance: Float = 0f,
        var mapColor: UByte = 0u,
        var slipperiness: Float = 0.6f,
        var velocityMultiplier: Float = 1f,
        var jumpVelocityMultiplier: Float = 1f,
        var itemId: UShort = 0u,
        var defaultStateId: UShort = 0u,
        var stateIds: List<UShort> = emptyList(),
        var isSolid: Boolean = true,
        var isAir: Boolean = false,
        var isFlammable: Boolean = false,
    )
    class BlockState(
        var id: UShort = 0u,
        var blockId: UShort = 0u,
        var blockName: String = "",
        var luminance: UByte = 0u,
        var opacity: UByte = 0u,
        var hardness: Float = 0f,
        var isAir: Boolean = false,
        var isLiquid: Boolean = false,
    )
    class World {
        fun getScoreboard(): Scoreboard.Scoreboard = Scoreboard.Scoreboard()
        fun getTimeOfDay(): ULong = 0uL
        fun setTimeOfDay(time: ULong) {}
        fun setRaining(raining: Boolean) {}
        fun setThundering(thundering: Boolean) {}
        fun spawnEntity(type: EntityTypes.EntityType, pos: Triple<Double, Double, Double>): Entity =
            Entity()
    }
    open class Entity {
        fun asMob(): Mob? = null
    }
    class LivingEntity {
        fun getHealth(): Float = 20f
        fun setHealth(value: Float) {}
        fun getMaxHealth(): Float = 20f
        fun setMaxHealth(value: Float) {}
    }
    class Mob {
        fun asLiving(): LivingEntity = LivingEntity()
        fun navigateToPos(pos: Triple<Double, Double, Double>, speed: Double): Boolean = false
        fun navigateToEntity(target: Entity, speed: Double): Boolean = false
        fun addCustomAiGoal(priority: UByte, goalId: UInt) {}
        fun getTarget(): Entity? = null
        fun setTarget(target: Entity?) {}
    }

    companion object {
        fun getBlockByName(name: String): Block? = null
        fun getBlockById(id: UShort): Block? = null
        fun getAllBlocks(): List<Block> = emptyList()
        fun getStatesForBlock(block: Block): List<BlockState> = emptyList()
        fun getStatesForBlockId(blockId: UShort): List<BlockState> = emptyList()
        fun getStateIdsForBlockId(blockId: UShort): List<UShort> = emptyList()
    }
}

interface ItemStack {
    class ItemStack(var registryKey: String, count: UByte) {
        private var currentCount = count
        fun getCount(): UByte = currentCount
        fun setCount(value: UByte) { currentCount = value }
    }
}

interface Screens {
    enum class Screen { GENERIC_9X1, GENERIC_9X2, GENERIC_9X3, GENERIC_3X3 }
}

interface Gui {
    class Gui(val type: Screens.Screen, val title: Text.TextComponent) {
        fun getInventory(): Inventory.Inventory = Inventory.Inventory()
        fun setItem(slot: UInt, item: ItemStack.ItemStack) {}
        fun getItem(slot: UInt): ItemStack.ItemStack? = null
        fun getSize(): UInt = 0u
        fun clearItems() {}
        fun setAllowGrabItems(allow: Boolean) {}
        fun setAllowPutItems(allow: Boolean) {}
    }
}

interface Inventory {
    open class Inventory {
        fun getItem(slot: UInt): pumpkin.ItemStack.ItemStack? = null
        fun setItem(slot: UInt, item: pumpkin.ItemStack.ItemStack?) {}
        fun getSize(): UInt = 0u
        fun removeItem(slot: UInt): pumpkin.ItemStack.ItemStack? = null
        fun countItem(itemId: String): UInt = 0u
        fun containsItem(itemId: String): Boolean = false
        fun setAllItems(items: List<pumpkin.ItemStack.ItemStack?>) {}
    }

    class PlayerInventory : Inventory() {
        fun asInventory(): Inventory = this
        fun getSelectedSlot(): UByte = 0u
        fun setSelectedSlot(slot: UByte) {}
        fun getHelmet(): pumpkin.ItemStack.ItemStack? = null
        fun setHelmet(item: pumpkin.ItemStack.ItemStack?) {}
        fun getChestplate(): pumpkin.ItemStack.ItemStack? = null
        fun setChestplate(item: pumpkin.ItemStack.ItemStack?) {}
        fun getLeggings(): pumpkin.ItemStack.ItemStack? = null
        fun setLeggings(item: pumpkin.ItemStack.ItemStack?) {}
        fun getBoots(): pumpkin.ItemStack.ItemStack? = null
        fun setBoots(item: pumpkin.ItemStack.ItemStack?) {}
        fun getOffHand(): pumpkin.ItemStack.ItemStack? = null
        fun setOffHand(item: pumpkin.ItemStack.ItemStack?) {}
    }
}

interface Server {
    class Server {
        fun broadcast(message: String) {}
        fun getAllPlayers(): List<Player.Player> = emptyList()
        fun getPlayerByName(name: String): Player.Player? = null
        fun getWorldByName(name: String): World.World? = null
        fun getRecipeManager(): Recipe.RecipeManager = Recipe.RecipeManager()
        fun getDatapackManager(): Datapack.DatapackManager = Datapack.DatapackManager()
        fun getEnchantmentManager(): Enchantments.EnchantmentManager = Enchantments.EnchantmentManager()
    }
}

interface Event {
    enum class EventType {
        PLAYER_JOIN_EVENT, PLAYER_LEAVE_EVENT, PLAYER_CHAT_EVENT, PLAYER_DEATH_EVENT,
        PLAYER_MOVE_EVENT, BLOCK_BREAK_EVENT, BLOCK_PLACE_EVENT,
    }

    enum class EventPriority { HIGHEST, HIGH, NORMAL, LOW, LOWEST }

    class PlayerJoinEventData(
        var player: Player.Player,
        var joinMessage: Text.TextComponent,
        var cancelled: Boolean,
    )

    class PlayerDeathEventData(
        var player: Player.Player,
        var deathMessage: Text.TextComponent,
        var cancelled: Boolean,
    )

    sealed interface Event {
        class PlayerJoinEvent(val value: PlayerJoinEventData) : Event
        class PlayerDeathEvent(val value: PlayerDeathEventData) : Event
    }
}

interface Command {
    class Command(names: List<String>, description: String)
    class CommandSender {
        fun sendMessage(text: Text.TextComponent) {}
    }
    class ConsumedArgs
    class CommandNode {
        fun suggestWithHandlerId(handlerId: UInt) {}

        companion object {
            fun literal(name: String): CommandNode = CommandNode()
            fun argument(name: String, type: Any?): CommandNode = CommandNode()
        }
    }
    class SuggestionRequest(val input: String, val cursor: UInt, val start: UInt, val remaining: String)
    class CommandSuggestion(var value: String, var tooltip: Text.TextComponent?)
    class CommandSuggestions(
        var start: UInt,
        var length: UInt,
        var values: List<CommandSuggestion>,
    )
}

interface Context {
    class Context {
        fun registerEvent(
            handlerId: UInt,
            eventType: Event.EventType,
            eventPriority: Event.EventPriority,
            blocking: Boolean,
        ) {
        }

        fun registerCommand(command: Command.Command, permission: String) {}

        fun registerPermission(permission: Permission.Permission): Result<Unit> = Result.success(Unit)

        fun getServer(): Server.Server = Server.Server()
        fun getDataFolder(): String = "/data"
    }
}

interface Metadata {
    class PluginMetadata(
        var name: String,
        var version: String,
        var authors: List<String>,
        var description: String,
        var dependencies: List<String>,
        var permissions: List<String>,
    )
}

interface Scheduler {
    companion object {
        fun scheduleDelayedTask(handlerId: UInt, delayTicks: ULong): UInt = 0u
        fun scheduleRepeatingTask(handlerId: UInt, delayTicks: ULong, periodTicks: ULong): UInt = 0u
        fun cancelTask(taskId: UInt) {}
    }
}

interface Ipc {
    companion object {
        fun sendIpcMessage(recipient: String, message: List<UByte>): Result<Result<List<UByte>>> =
            Result.success(Result.success(emptyList()))
    }
}

interface Permission {
    enum class PermissionLevel { ZERO, ONE, TWO, THREE, FOUR }

    sealed interface PermissionDefault {
        object Allow : PermissionDefault
        object Deny : PermissionDefault
        class Op(val value: PermissionLevel) : PermissionDefault
    }

    class PermissionChild(var node: String, var value: Boolean)

    class Permission(
        var node: String,
        var description: String,
        var default: PermissionDefault,
        var children: List<PermissionChild>,
    )
}

interface Common {
    enum class NamedColor { BLACK, WHITE, RED, BLUE, GREEN }
}

interface BossBar {
    enum class BossBarColor { PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE }
    enum class BossBarDivision { NO_DIVISION, NOTCHES_6, NOTCHES_10, NOTCHES_12, NOTCHES_20 }
    class BossBar(
        title: Text.TextComponent,
        color: BossBarColor,
        division: BossBarDivision,
    ) {
        fun setTitle(title: Text.TextComponent) {}
        fun getTitle(): Text.TextComponent = Text.TextComponent.text("")
        fun setHealth(health: Float) {}
        fun getHealth(): Float = 0.0f
        fun setColor(color: BossBarColor) {}
        fun getColor(): BossBarColor = BossBarColor.WHITE
        fun setDivision(division: BossBarDivision) {}
        fun addPlayer(player: Player.Player) {}
        fun removePlayer(player: Player.Player) {}
        fun removeAll() {}
    }
}
