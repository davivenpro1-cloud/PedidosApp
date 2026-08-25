package com.pedidos

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Servicio de accesibilidad: cuando hay un envío automático pendiente (ver
 * AutoEnvioFlag) y detecta que WhatsApp ha mostrado el botón de "Enviar",
 * lo pulsa él solo, sin que el padre tenga que tocar nada.
 *
 * Si NO hay ningún envío automático pendiente, el servicio no hace
 * absolutamente nada: no interfiere en el uso normal de WhatsApp.
 */
class WhatsAppAutoEnviarService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var intentosRestantes = 0
    private var buscando = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName != "com.whatsapp") return
        if (!AutoEnvioFlag.estaPendiente(this)) return

        if (!buscando) {
            buscando = true
            intentosRestantes = 10 // Reintentamos ~5 segundos mientras carga la pantalla de WhatsApp.
            intentarPulsarEnviar()
        }
    }

    private fun intentarPulsarEnviar() {
        if (!AutoEnvioFlag.estaPendiente(this)) {
            buscando = false
            return
        }

        val raiz = rootInActiveWindow
        val boton = raiz?.let { buscarBotonEnviar(it) }

        if (boton != null) {
            boton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            AutoEnvioFlag.desactivar(this)
            buscando = false
            return
        }

        intentosRestantes--
        if (intentosRestantes > 0) {
            handler.postDelayed({ intentarPulsarEnviar() }, 500)
        } else {
            // No lo hemos encontrado a tiempo: el mensaje se queda escrito en WhatsApp
            // listo para enviar a mano, y apagamos la bandera para no interferir después.
            AutoEnvioFlag.desactivar(this)
            buscando = false
        }
    }

    /** Busca el botón de enviar por su id conocido y, si no aparece, por su descripción. */
    private fun buscarBotonEnviar(nodo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val porId = nodo.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (porId.isNotEmpty()) return porId[0]
        return buscarPorDescripcion(nodo)
    }

    private fun buscarPorDescripcion(nodo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = nodo.contentDescription?.toString()?.lowercase()
        if (nodo.isClickable && (desc == "enviar" || desc == "send")) {
            return nodo
        }
        for (i in 0 until nodo.childCount) {
            val hijo = nodo.getChild(i) ?: continue
            val encontrado = buscarPorDescripcion(hijo)
            if (encontrado != null) return encontrado
        }
        return null
    }

    override fun onInterrupt() {}
}
