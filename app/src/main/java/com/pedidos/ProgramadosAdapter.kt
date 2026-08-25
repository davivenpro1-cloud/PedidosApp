package com.pedidos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pedidos.databinding.ItemProgramadoBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProgramadosAdapter(
    private var items: List<MensajeProgramado>,
    private val onEliminar: (MensajeProgramado) -> Unit
) : RecyclerView.Adapter<ProgramadosAdapter.VistaMensaje>() {

    inner class VistaMensaje(val binding: ItemProgramadoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VistaMensaje {
        val binding = ItemProgramadoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VistaMensaje(binding)
    }

    override fun onBindViewHolder(holder: VistaMensaje, position: Int) {
        val m = items[position]
        holder.binding.textoMensaje.text = m.texto
        holder.binding.textoProgramacion.text = describir(m)
        holder.binding.botonEliminar.setOnClickListener { onEliminar(m) }
    }

    override fun getItemCount() = items.size

    fun actualizar(nuevos: List<MensajeProgramado>) {
        items = nuevos
        notifyDataSetChanged()
    }

    private fun describir(m: MensajeProgramado): String {
        val hora = String.format(Locale.getDefault(), "%02d:%02d", m.hora, m.minuto)
        return if (m.tipo == TipoProgramacion.UNICO) {
            val formato = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
            val fechaTexto = m.fechaMillis?.let { formato.format(it) } ?: ""
            "Se enviará el $fechaTexto a las $hora"
        } else {
            val nombres = mapOf(
                Calendar.MONDAY to "Lun", Calendar.TUESDAY to "Mar", Calendar.WEDNESDAY to "Mié",
                Calendar.THURSDAY to "Jue", Calendar.FRIDAY to "Vie", Calendar.SATURDAY to "Sáb",
                Calendar.SUNDAY to "Dom"
            )
            val orden = listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
                Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
            )
            val dias = orden.filter { m.diasSemana.contains(it) }.mapNotNull { nombres[it] }.joinToString(", ")
            "Cada $dias a las $hora"
        }
    }
}
