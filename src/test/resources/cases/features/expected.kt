package com.example

import java.io.BufferedReader
import java.io.IOException

open class Features {
    private val names: MutableList<String> = mutableListOf()

    private var numbers: IntArray = intArrayOf(1, 2, 3)

    open fun add(name: String) {
        names.add(name)
    }

    open fun classify(value: Int): Int {
        when (value) {
            1, 2 -> {
                return 10
            }
            3 -> {
                return 30
            }
            else -> {
                return 0
            }
        }
    }

    open fun describe(value: Any?): String {
        if (value is String && !value.isEmpty()) {
            return value.uppercase()
        }
        return value.toString()
    }

    open fun widen(a: Int, b: Long): Long {
        return (a).toLong() + b
    }

    open fun bitwise(a: Int, b: Int): Int {
        return (((a and b)) or (((a shl 2)) xor ((b ushr 1))))
    }

    open fun join(): String {
        return names.joinToString(",")
    }

    open fun counts(): MutableMap<String, Int?> {
        val result: MutableMap<String, Int?> = mutableMapOf()
        for (name in names) {
            result[name] = (if (result.containsKey(name)) result[name] else 0)!! + 1
        }
        return result
    }

    @Throws(IOException::class)
    open fun read(reader: BufferedReader) {
        val resource0 = reader
        resource0.use { resource0 ->
            try {
                val line: String = reader.readLine()
                println(line)
            } catch (e: IOException) {
                System.err.println(e.message)
            }
        }
    }

    open fun task(): Runnable {
        return { println("done") }
    }

    open fun copy(input: IntArray): IntArray {
        return input.copyOf(input.size)
    }

}
