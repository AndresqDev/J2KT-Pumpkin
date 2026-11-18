package com.example

import kotlin.math.*

open class Advanced {
    fun interface Greeter {
        abstract fun greet(name: String): String

        fun greetTwice(name: String): String {
            return greet(name) + " " + greet(name)
        }

        companion object {
            @JvmStatic
            fun upper(): Greeter {
                return { name -> name.uppercase() }
            }

        }
    }

    abstract class Base<T>(protected val value: T) {
        abstract fun get(): T

    }

    open class Derived(value: String) : Base<String>(value), Greeter {
        private val history: MutableList<String> = mutableListOf()

        override fun get(): String {
            return value
        }

        override fun greet(name: String): String {
            history.add(name)
            return "Hello " + name
        }

        companion object {
            private var created: Int = 0

            init {
                created = 0
            }

            @JvmStatic
            fun created(): Int {
                return created
            }

        }
    }

    enum class Color(val code: String) {
        RED("r"),
        GREEN("g")
        ;
    }

    data class Point(val x: Int, val y: Int) {
        init {
            if (x < 0) {
                throw IllegalArgumentException("x")
            }
        }

        fun sum(): Int {
            return x + y
        }

    }

    sealed interface Shape {
    }

    data class Circle(val radius: Double) : Shape {
    }

    data class Square(val side: Double) : Shape {
    }

    companion object {
        @JvmStatic
        fun area(shape: Shape): Double {
            return when (shape) { is Circle -> run { val c = shape; PI * c.radius * c.radius }; is Square -> run { val s = shape; s.side * s.side } }
        }

        @JvmStatic
        fun pick(value: Int): String {
            return when (value) { 1, 2 -> "low"; 3 -> "three"; else -> "high" }
        }

        @JvmStatic
        fun matrix(rows: Int, cols: Int): Array<IntArray> {
            val result: Array<IntArray> = Array(rows) { IntArray(cols) }
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    result[i][j] = i * j
                }
            }
            return result
        }

        @JvmStatic
        fun <T : Comparable<T>> max(values: MutableList<T>): T {
            var best: T = values[0]
            for (value in values) {
                if (value.compareTo(best) > 0) {
                    best = value
                }
            }
            return best
        }

        @JvmStatic
        fun apply(fn: (String) -> String, value: String): String {
            return fn(value)
        }

        @JvmStatic
        fun runnable(): Runnable {
            return object : Runnable {
                override fun run() {
                    println("run")
                }
            }

        }

    }
}
