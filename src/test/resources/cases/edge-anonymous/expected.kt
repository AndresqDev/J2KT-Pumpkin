package edge

import java.util.Comparator

open class EdgeAnonymous {
    open fun task(): Runnable {
        return object : Runnable {
            override fun run() {
                println("x")
            }
        }

    }

    open fun comparator(): Comparator<String> {
        return object : Comparator<String> {
            override fun compare(a: String, b: String): Int {
                return a.compareTo(b)
            }
        }

    }

}
