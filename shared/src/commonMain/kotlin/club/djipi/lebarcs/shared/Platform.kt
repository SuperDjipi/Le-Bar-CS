// Dans : shared/src/commonMain/kotlin/club/djipi/lebarcs/shared/Platform.kt

package club.djipi.lebarcs.shared

/**
 * Retourne le nom de la plateforme actuelle (ex: "Android", "JVM").
 */
expect fun platform(): String

/**
 * NOUVEL AJOUT
 * Retourne le timestamp actuel en millisecondes depuis l'époque Unix.
 * L'implémentation sera fournie par chaque plateforme.
 */
expect fun getCurrentTimestamp(): Long

expect fun generateUUID(): String