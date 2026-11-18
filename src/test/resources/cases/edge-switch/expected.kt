package edge

open class EdgeSwitch {
    open fun name(value: Int): String {
        var result: String = ""
        run switch@ {
            val __switchValue = value
            var __fallthrough = false
            var __matched = false
            if (__fallthrough || __switchValue == 1) {
                __fallthrough = true
                __matched = true
                result = "one"
                return@switch
            }
            if (__fallthrough || __switchValue == 2) {
                __fallthrough = true
                __matched = true
                result = "two"
            }
            if (__fallthrough || __switchValue == 3) {
                __fallthrough = true
                __matched = true
                result = "three"
                return@switch
            }
            if (__fallthrough || !__matched) {
                result = "many"
            }
        }
        return result
    }

    open fun expression(value: Int): Int {
        return when (value) { 1, 2 -> 10; 3 -> 30; else -> 0 }
    }

}
