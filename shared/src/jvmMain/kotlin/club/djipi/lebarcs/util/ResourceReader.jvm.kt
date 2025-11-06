package club.djipi.lebarcs.util

import java.io.File

class JvmResourceReader : ResourceReader {
    override fun readText(path: String): String {
        val resourceUrl = Thread.currentThread().contextClassLoader.getResource(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        return File(resourceUrl.toURI()).readText()
    }
}

