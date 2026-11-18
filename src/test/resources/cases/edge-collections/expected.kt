package edge

open class EdgeCollections {
    private val names: MutableList<String> = mutableListOf()

    private val counts: MutableMap<String, Int?> = mutableMapOf()

    open fun add(name: String) {
        names.add(name)
        counts[name] = (if (counts.containsKey(name)) counts[name] else 0)!! + 1
    }

    open fun first(): String {
        return names[0]
    }

    open fun removeFirst() {
        names.removeAt(0)
    }

    open fun size(): Int {
        return names.size
    }

    open fun copy(values: IntArray): IntArray {
        return values.copyOf(values.size)
    }

}
