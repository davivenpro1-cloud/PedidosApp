package com.pedidos

/**
 * Representa un mensaje que se debe enviar automáticamente por WhatsApp,
 * bien una única vez en una fecha y hora concretas, bien de forma recurrente
 * en ciertos días de la semana (por ejemplo, todos los lunes y viernes a las 8:00).
 */
data class MensajeProgramado(
    val id: Long,
    var texto: String,
    var tipo: TipoProgramacion,
    var fechaMillis: Long? = null,          // Solo para tipo UNICO: fecha y hora exacta del envío
    var diasSemana: Set<Int> = emptySet(),  // Solo para tipo RECURRENTE: Calendar.MONDAY, Calendar.FRIDAY, etc.
    var hora: Int,
    var minuto: Int,
    var activo: Boolean = true
)

enum class TipoProgramacion {
    UNICO,
    RECURRENTE
}
