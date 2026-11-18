package plugin

typealias PluginContext = pumpkin.Context.Context
typealias PluginMetadata = pumpkin.Metadata.PluginMetadata

abstract class PumpkinPlugin {
    val tasks = TaskScheduler()

    abstract fun metadata(): PluginMetadata

    open fun onLoad(context: PluginContext): Result<Unit> = Result.success(Unit)

    open fun onUnload(context: PluginContext): Result<Unit> = Result.success(Unit)

    open fun handleEvent(
        eventId: UInt,
        server: pumpkin.Server.Server,
        event: pumpkin.Event.Event,
    ): pumpkin.Event.Event = event

    open fun handleCommand(
        commandId: UInt,
        sender: pumpkin.Command.CommandSender,
        server: pumpkin.Server.Server,
        args: pumpkin.Command.ConsumedArgs,
    ): Result<Int> = Result.failure(IllegalStateException("no command handler"))

    open fun handleCommandSuggestion(
        handlerId: UInt,
        sender: pumpkin.Command.CommandSender,
        server: pumpkin.Server.Server,
        request: pumpkin.Command.SuggestionRequest,
    ): pumpkin.Command.CommandSuggestions = pumpkin.Command.CommandSuggestions(0u, 0u, emptyList())

    open fun handleIpcMessage(sender: String, message: List<UByte>): Result<List<UByte>> =
        Result.failure(IllegalStateException("no IPC handler"))

    open fun handleTask(handlerId: UInt, server: pumpkin.Server.Server) {}

    open fun handleAiGoalCanStart(goalId: UInt, server: pumpkin.Server.Server, entity: pumpkin.World.Entity): Boolean = false
    open fun handleAiGoalShouldContinue(goalId: UInt, server: pumpkin.Server.Server, entity: pumpkin.World.Entity): Boolean = false
    open fun handleAiGoalStart(goalId: UInt, server: pumpkin.Server.Server, entity: pumpkin.World.Entity) {}
    open fun handleAiGoalTick(goalId: UInt, server: pumpkin.Server.Server, entity: pumpkin.World.Entity) {}
    open fun handleAiGoalStop(goalId: UInt, server: pumpkin.Server.Server, entity: pumpkin.World.Entity) {}
}

class TaskScheduler {
    fun afterTicks(delayTicks: ULong, action: (pumpkin.Server.Server) -> Unit) {}
}

class ScheduledTask internal constructor(private val onCancel: () -> Unit) {
    fun cancel() = onCancel()
}
