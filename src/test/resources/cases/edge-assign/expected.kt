package edge

open class EdgeAssign {
    open fun bump(b: Byte): Byte {
        var b = b
        b = (b + 1).toByte()
        return b
    }

    open fun addLong(i: Int): Int {
        var i = i
        i = (i + 2L).toInt()
        return i
    }

    open fun next(c: Char): Char {
        var c = c
        c = (c + 1)
        return c
    }

    open fun clearLowBit(x: Int): Int {
        var x = x
        x = (x and (1).inv())
        return x
    }

    open fun shift(x: Int): Int {
        var x = x
        x = (x shl 2)
        return x
    }

}
