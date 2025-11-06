package club.djipi.lebarcs.shared

import java.util.UUID

actual fun platform(): String = "Android"

/**
 * NOUVEL AJOUT
 * Implémentation pour Android, utilisant l'API standard Java/Android.
 */
actual fun getCurrentTimestamp(): Long = System.currentTimeMillis()

actual fun generateUUID(): String = UUID.randomUUID().toString()
