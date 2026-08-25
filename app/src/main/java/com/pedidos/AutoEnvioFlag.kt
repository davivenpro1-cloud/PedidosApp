package com.pedidos

import android.content.Context

/**
 * Bandera compartida que le dice al servicio de accesibilidad que hay un envío
 * automático pendiente: "la próxima vez que veas el botón Enviar de WhatsApp,
 * púlsalo tú solo".
 *
 * Tiene caducidad (30 segundos) para que el servicio nunca pulse "Enviar" por
 * error si el usuario abre WhatsApp de forma normal poco después.
 */
object AutoEnvioFlag {

    private const val PREFS_NAME = "auto_envio_flag"
    private const val KEY_ACTIVO = "activo"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val CADUCIDAD_MS = 30_000L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun activar(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ACTIVO, true)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun desactivar(context: Context) {
        prefs(context).edit().putBoolean(KEY_ACTIVO, false).apply()
    }

    fun estaPendiente(context: Context): Boolean {
        val p = prefs(context)
        if (!p.getBoolean(KEY_ACTIVO, false)) return false
        val transcurrido = System.currentTimeMillis() - p.getLong(KEY_TIMESTAMP, 0L)
        if (transcurrido > CADUCIDAD_MS) {
            desactivar(context)
            return false
        }
        return true
    }
}
