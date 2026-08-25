package com.pedidos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Guarda y recupera la lista de mensajes programados usando SharedPreferences,
 * igual que Prefs guarda el teléfono de WhatsApp. Todo se serializa como JSON,
 * así no hace falta añadir Room ni ninguna librería nueva de base de datos.
 */
object ProgramacionStore {

    private const val PREFS_NAME = "programacion_prefs"
    private const val KEY_LISTA = "lista_mensajes"
    private const val KEY_SIGUIENTE_ID = "siguiente_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun obtenerTodos(context: Context): MutableList<MensajeProgramado> {
        val json = prefs(context).getString(KEY_LISTA, null) ?: return mutableListOf()
        val array = JSONArray(json)
        val lista = mutableListOf<MensajeProgramado>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val diasArray = o.optJSONArray("diasSemana")
            val dias = mutableSetOf<Int>()
            if (diasArray != null) {
                for (j in 0 until diasArray.length()) dias.add(diasArray.getInt(j))
            }
            lista.add(
                MensajeProgramado(
                    id = o.getLong("id"),
                    texto = o.getString("texto"),
                    tipo = TipoProgramacion.valueOf(o.getString("tipo")),
                    fechaMillis = if (o.has("fechaMillis") && !o.isNull("fechaMillis")) o.getLong("fechaMillis") else null,
                    diasSemana = dias,
                    hora = o.getInt("hora"),
                    minuto = o.getInt("minuto"),
                    activo = o.optBoolean("activo", true)
                )
            )
        }
        return lista
    }

    private fun guardarTodos(context: Context, lista: List<MensajeProgramado>) {
        val array = JSONArray()
        for (m in lista) {
            val o = JSONObject()
            o.put("id", m.id)
            o.put("texto", m.texto)
            o.put("tipo", m.tipo.name)
            o.put("fechaMillis", m.fechaMillis)
            o.put("diasSemana", JSONArray(m.diasSemana.toList()))
            o.put("hora", m.hora)
            o.put("minuto", m.minuto)
            o.put("activo", m.activo)
            array.put(o)
        }
        prefs(context).edit().putString(KEY_LISTA, array.toString()).apply()
    }

    fun agregar(context: Context, mensaje: MensajeProgramado) {
        val lista = obtenerTodos(context)
        lista.add(mensaje)
        guardarTodos(context, lista)
    }

    fun eliminar(context: Context, id: Long) {
        val lista = obtenerTodos(context)
        lista.removeAll { it.id == id }
        guardarTodos(context, lista)
    }

    fun actualizar(context: Context, mensaje: MensajeProgramado) {
        val lista = obtenerTodos(context)
        val idx = lista.indexOfFirst { it.id == mensaje.id }
        if (idx >= 0) {
            lista[idx] = mensaje
            guardarTodos(context, lista)
        }
    }

    fun obtenerPorId(context: Context, id: Long): MensajeProgramado? =
        obtenerTodos(context).firstOrNull { it.id == id }

    fun nuevoId(context: Context): Long {
        val p = prefs(context)
        val id = p.getLong(KEY_SIGUIENTE_ID, 1L)
        p.edit().putLong(KEY_SIGUIENTE_ID, id + 1).apply()
        return id
    }
}
