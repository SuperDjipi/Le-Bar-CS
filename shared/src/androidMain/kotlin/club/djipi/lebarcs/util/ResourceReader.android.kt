package club.djipi.lebarcs.util

import android.content.Context

class AndroidResourceReader(private val context: Context) : ResourceReader {
    override fun readText(path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }
}
