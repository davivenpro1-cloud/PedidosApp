package com.pedidos

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Se encarga de programar y cancelar las alarmas del sistema (AlarmManager)
 * que disparan el envío automático de cada mensaje programado.
 */
object AlarmScheduler {

    private fun pendingIntent(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, EnvioProgramadoReceiver::class.java).apply {
            putExtra(EnvioProgramadoReceiver.EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Calcula en qué milisegundo (fecha absoluta) debe dispararse el próximo envío. */
    fun calcularProximoDisparo(mensaje: MensajeProgramado): Long? {
        val ahora = Calendar.getInstance()

        if (mensaje.tipo == TipoProgramacion.UNICO) {
            val fecha = mensaje.fechaMillis ?: return null
            return if (fecha > ahora.timeInMillis) fecha else null
        }

        // RECURRENTE: buscamos el próximo día de la semana (desde hoy) que coincida.
        if (mensaje.diasSemana.isEmpty()) return null

        for (i in 0..7) {
            val candidato = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, mensaje.hora)
                set(Calendar.MINUTE, mensaje.minuto)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (mensaje.diasSemana.contains(candidato.get(Calendar.DAY_OF_WEEK)) &&
                candidato.timeInMillis > ahora.timeInMillis
            ) {
                return candidato.timeInMillis
            }
        }
        return null
    }

    fun programar(context: Context, mensaje: MensajeProgramado) {
        if (!mensaje.activo) return
        val disparo = calcularProximoDisparo(mensaje) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, mensaje.id)

        if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) {
            // Sin el permiso de "alarmas y recordatorios" no podemos garantizar la hora exacta.
            // La pantalla de Programación se encarga de pedir este permiso al usuario.
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, disparo, pi)
    }

    fun cancelar(context: Context, id: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, id))
    }

    /** Reprograma todos los mensajes activos. Se usa al arrancar el móvil, ya que AlarmManager las borra. */
    fun reprogramarTodos(context: Context) {
        val lista = ProgramacionStore.obtenerTodos(context)
        for (m in lista) {
            if (m.activo) programar(context, m) else cancelar(context, m.id)
        }
    }
}
