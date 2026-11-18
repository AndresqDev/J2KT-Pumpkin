package edge

open class EdgeFactory {
    open fun names(): MutableList<String> {
        return mutableListOf<String>("a", "b")
    }

    open fun unique(): MutableSet<String> {
        return mutableSetOf<String>("a", "b")
    }

    open fun counts(): MutableMap<String, Int?> {
        return mutableMapOf<String, Int?>("a" to 1, "b" to 2)
    }

}
