package edge

open class EdgeLoops {
    open fun sumEven(n: Int): Int {
        var total: Int = 0
        for (i in 0 until n step 2) {
            total += i
        }
        return total
    }

    open fun countdown(n: Int): Int {
        var total: Int = 0
        for (i in n downTo 1) {
            total += i
        }
        return total
    }

    open fun inclusive(n: Int): Int {
        var total: Int = 0
        for (i in 0..n) {
            total += i
        }
        return total
    }

    open fun labeled() {
        outer@ for (i in 0 until 3) {
            for (j in 0 until 3) {
                if (j == 1) {
                    continue@outer
                }
                if (i == 2) {
                    break@outer
                }
            }
        }
    }

}
