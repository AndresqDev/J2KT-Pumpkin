package com.example

open class Hello(private val name: String) {
    private var count: Int = 0

    open fun greet(formal: Boolean): String {
        val prefix: String = if (formal) "Good day" else "Hi"
        return prefix + ", " + name + "!"
    }

    open fun sum(values: IntArray): Int {
        var total: Int = 0
        for (value in values) {
            total += value
        }
        return total
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val hello: Hello = Hello("Andres")
            println(hello.greet(true))
        }

    }
}
