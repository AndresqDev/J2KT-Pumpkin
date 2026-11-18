package plugin

import pumpkin.Common
import pumpkin.Scoreboard
import pumpkin.Text
import pumpkin.World

/** Returns the scoreboard associated with a world. */
fun World.World.scoreboard(): Scoreboard.Scoreboard = getScoreboard()

fun Scoreboard.Scoreboard.objective(
    name: String,
    title: String,
    renderType: Scoreboard.RenderType = Scoreboard.RenderType.INTEGER,
) {
    addObjective(name, Text.TextComponent.text(title), renderType, null)
}

fun Scoreboard.Scoreboard.updateObjectiveTitle(
    name: String,
    title: String,
    renderType: Scoreboard.RenderType = Scoreboard.RenderType.INTEGER,
) {
    updateObjective(name, Text.TextComponent.text(title), renderType, null)
}

fun Scoreboard.Scoreboard.score(entityName: String, objectiveName: String, value: Int) {
    updateScore(entityName, objectiveName, value, null)
}

fun Scoreboard.Scoreboard.display(slot: Scoreboard.DisplaySlot, objectiveName: String) {
    setDisplaySlot(slot, objectiveName)
}

fun Scoreboard.Scoreboard.team(name: String, block: TeamBuilder.() -> Unit) {
    val settings = TeamBuilder(name).apply(block).build()
    createTeam(name, settings)
}

class TeamBuilder internal constructor(private val name: String) {
    var displayName: Text.TextComponent = Text.TextComponent.text(name)
    var friendlyFire: Boolean = false
    var seeFriendlyInvisibles: Boolean = false
    var nametagVisibility: Scoreboard.NametagVisibility = Scoreboard.NametagVisibility.ALWAYS
    var collisionRule: Scoreboard.CollisionRule = Scoreboard.CollisionRule.ALWAYS
    var color: Common.NamedColor = Common.NamedColor.WHITE
    var prefix: Text.TextComponent = Text.TextComponent.text("")
    var suffix: Text.TextComponent = Text.TextComponent.text("")

    fun build(): Scoreboard.TeamSettings = Scoreboard.TeamSettings(
        displayName = displayName,
        friendlyFire = friendlyFire,
        seeFriendlyInvisibles = seeFriendlyInvisibles,
        nametagVisibility = nametagVisibility,
        collisionRule = collisionRule,
        color = color,
        prefix = prefix,
        suffix = suffix,
    )
}
