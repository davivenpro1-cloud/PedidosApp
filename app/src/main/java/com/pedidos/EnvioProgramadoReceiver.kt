package com.pedidos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Se ejecuta cuando llega la hora programada para un mensaje.
 * Abre WhatsApp con el mensaje ya escrito y activa el "envío automático":
 * el WhatsAppAutoEnviarService (servicio de accesibilidad) se encargará de
 * pulsar el botón de enviar por sí solo, sin que nadie toque el móvil.
 */
class EnvioProgramadoReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ID = "extra_id_mensaje_programado"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id == -1L) return

        val mensaje = ProgramacionStore.obtenerPorId(context, id) ?: return
        if (!mensaje.activo) return

        val telefono = Prefs.getTelefono(context)
        if (telefono.isBlank()) return // Sin número configurado en Ajustes no se puede enviar nada.

        // Avisamos al servicio de accesibilidad: el próximo botón "Enviar" que
        // aparezca en WhatsApp debe pulsarse solo.
        AutoEnvioFlag.activar(context)

        val texto = Uri.encode(mensaje.texto)
        val uri = Uri.parse("https://wa.me/$telefono?text=$texto")
        val abrirIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(abrirIntent)
        } catch (e: Exception) {
            AutoEnvioFlag.desactivar(context)
        }

        // Si es recurrente, ya dejamos programado el siguiente envío (la semana que viene).
        // Si era de un solo día, lo quitamos de la lista porque ya ha cumplido su función.
        if (mensaje.tipo == TipoProgramacion.RECURRENTE) {
            AlarmScheduler.programar(context, mensaje)
        } else {
            ProgramacionStore.eliminar(context, mensaje.id)
        }
    }
}
