package com.pedidos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Las alarmas de AlarmManager se borran cada vez que el móvil se reinicia,
 * así que al arrancar volvemos a programar todos los mensajes activos.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.reprogramarTodos(context)
        }
    }
}
