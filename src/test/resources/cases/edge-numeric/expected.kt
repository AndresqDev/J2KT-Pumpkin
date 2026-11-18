package edge

open class EdgeNumeric {
    open fun widen(a: Int, b: Long): Long {
        return (a).toLong() + b
    }

    open fun chars(a: Char, b: Char): Int {
        return a.code + b.code
    }

    open fun bump(b: Byte): Byte {
        return (((b).toInt() + 1)).toByte()
    }

    open fun bits(a: Int, b: Int): Int {
        return (((a and b)) or (((a shl 2)) xor ((b ushr 1))))
    }

    open fun same(a: String, b: String): Boolean {
        return a === b
    }

    open fun sameValue(a: String, b: String): Boolean {
        return a == b
    }

    open fun mix(a: Int, b: Double): Double {
        return (a).toDouble() * b + (1).toDouble()
    }

    open fun next(c: Char): Char {
        return ((c.code + 1)).toChar()
    }

}
