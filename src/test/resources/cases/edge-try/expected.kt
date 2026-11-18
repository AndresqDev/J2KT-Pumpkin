package edge

import java.io.BufferedReader
import java.io.IOException

open class EdgeTry {
    open fun read(reader: BufferedReader): String {
        try {
            return reader.readLine()
        } catch (e: IOException) {
            return e.message!!
        } catch (e: IllegalStateException) {
            return e.message!!
        } finally {
            println("done")
        }
    }

    @Throws(IOException::class)
    open fun close(reader: BufferedReader) {
        val resource0 = reader
        resource0.use { resource0 ->
            println(reader.readLine())
        }
    }

}
