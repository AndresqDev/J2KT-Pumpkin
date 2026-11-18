package edge

open class EdgeMapEntry {
    open fun first(values: MutableMap<String, Int?>): Map.Entry<String, Int?> {
        return values.entries.iterator().next()
    }

    open fun sum(values: MutableMap<String, Int?>): Int {
        var total: Int = 0
        for (entry in values.entries) {
            total += entry.value!!
        }
        return total
    }

}
