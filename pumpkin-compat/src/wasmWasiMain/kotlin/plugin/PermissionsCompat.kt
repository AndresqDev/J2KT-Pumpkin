package plugin

import pumpkin.Permission

/** Permission builder using Kotlin defaults and varargs. */
fun permission(
    node: String,
    description: String = "",
    default: Permission.PermissionDefault = Permission.PermissionDefault.Deny,
    vararg children: Permission.PermissionChild,
): Permission.Permission = Permission.Permission(node, description, default, children.toList())
