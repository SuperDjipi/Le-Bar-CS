package club.djipi.lebarcs.shared

actual fun platform(): String = "Android"

/**
 * NOUVEL AJOUT
 * Implémentation pour Android, utilisant l'API standard Java/Android.
 */
actual fun getCurrentTimestamp(): Long = System.currentTimeMillis()
