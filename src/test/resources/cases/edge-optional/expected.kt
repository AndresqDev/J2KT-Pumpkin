package edge

open class EdgeOptional {
    open fun name(value: String?): String {
        return (value ?: "unknown")
    }

    open fun present(value: String?): Boolean {
        return value != null
    }

    open fun upper(value: String?): String {
        return (value?.let(String::uppercase) ?: "")
    }

}
