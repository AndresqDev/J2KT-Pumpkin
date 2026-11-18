package plugin

import pumpkin.World

/** Safe casts/navigation helpers over World.Entity and World.Mob. */
fun World.Entity.mob(): World.Mob? = asMob()

fun World.Mob.health(value: Float) {
    asLiving().setHealth(value)
}

fun World.Mob.maxHealth(value: Float) {
    asLiving().setMaxHealth(value)
}

fun World.Mob.navigateTo(position: Triple<Double, Double, Double>, speed: Double = 1.0): Boolean =
    navigateToPos(position, speed)

fun World.Mob.navigateTo(target: World.Entity, speed: Double = 1.0): Boolean =
    navigateToEntity(target, speed)
