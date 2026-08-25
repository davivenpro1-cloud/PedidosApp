package com.pedidos

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.pedidos.databinding.ActivityProgramacionBinding
import com.pedidos.databinding.DialogNuevoProgramadoBinding
import java.util.Calendar

class ProgramacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgramacionBinding
    private lateinit var adapter: ProgramadosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgramacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ProgramadosAdapter(emptyList()) { mensaje ->
            AlarmScheduler.cancelar(this, mensaje.id)
            ProgramacionStore.eliminar(this, mensaje.id)
            refrescarLista()
        }
        binding.recyclerProgramados.layoutManager = LinearLayoutManager(this)
        binding.recyclerProgramados.adapter = adapter

        binding.fabAgregar.setOnClickListener { mostrarDialogoNuevo() }

        binding.avisoAccesibilidad.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        refrescarLista()
        comprobarPermisos()
    }

    private fun refrescarLista() {
        val lista = ProgramacionStore.obtenerTodos(this).filter { it.activo }
        adapter.actualizar(lista)
        binding.textoVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerProgramados.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun comprobarPermisos() {
        if (Build.VERSION.SDK_INT >= 31) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Activa el permiso de \"Alarmas y recordatorios\" para que los pedidos se envíen a su hora exacta",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }
        }

        binding.avisoAccesibilidad.visibility =
            if (servicioAccesibilidadActivo()) View.GONE else View.VISIBLE
    }

    private fun servicioAccesibilidadActivo(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val activos = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return activos.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun mostrarDialogoNuevo() {
        val dialogBinding = DialogNuevoProgramadoBinding.inflate(layoutInflater)

        var fechaSeleccionada: Triple<Int, Int, Int>? = null // año, mes, día
        var horaElegida = 8
        var minutoElegido = 0

        val checkboxesDias = mapOf(
            Calendar.MONDAY to dialogBinding.diaLun,
            Calendar.TUESDAY to dialogBinding.diaMar,
            Calendar.WEDNESDAY to dialogBinding.diaMie,
            Calendar.THURSDAY to dialogBinding.diaJue,
            Calendar.FRIDAY to dialogBinding.diaVie,
            Calendar.SATURDAY to dialogBinding.diaSab,
            Calendar.SUNDAY to dialogBinding.diaDom
        )

        fun actualizarVisibilidad() {
            val esUnico = dialogBinding.radioUnico.isChecked
            dialogBinding.botonElegirFecha.visibility = if (esUnico) View.VISIBLE else View.GONE
            dialogBinding.filaDias.visibility = if (esUnico) View.GONE else View.VISIBLE
        }
        actualizarVisibilidad()
        dialogBinding.grupoTipo.setOnCheckedChangeListener { _, _ -> actualizarVisibilidad() }

        dialogBinding.botonElegirFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, anio, mes, dia ->
                fechaSeleccionada = Triple(anio, mes, dia)
                dialogBinding.botonElegirFecha.text = "Fecha: $dia/${mes + 1}/$anio"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialogBinding.botonElegirHora.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                horaElegida = h
                minutoElegido = m
                dialogBinding.botonElegirHora.text = String.format("Elegir hora (%02d:%02d)", h, m)
            }, horaElegida, minutoElegido, true).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Nuevo pedido programado")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val texto = dialogBinding.campoMensaje.text.toString().trim()
                if (TextUtils.isEmpty(texto)) {
                    Toast.makeText(this, "Escribe el pedido antes de guardar", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val esUnico = dialogBinding.radioUnico.isChecked
                val nuevo = if (esUnico) {
                    val fecha = fechaSeleccionada
                    if (fecha == null) {
                        Toast.makeText(this, "Elige una fecha", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val cal = Calendar.getInstance()
                    cal.set(fecha.first, fecha.second, fecha.third, horaElegida, minutoElegido, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    MensajeProgramado(
                        id = ProgramacionStore.nuevoId(this),
                        texto = texto,
                        tipo = TipoProgramacion.UNICO,
                        fechaMillis = cal.timeInMillis,
                        hora = horaElegida,
                        minuto = minutoElegido
                    )
                } else {
                    val dias = checkboxesDias.filterValues { it.isChecked }.keys
                    if (dias.isEmpty()) {
                        Toast.makeText(this, "Elige al menos un día de la semana", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    MensajeProgramado(
                        id = ProgramacionStore.nuevoId(this),
                        texto = texto,
                        tipo = TipoProgramacion.RECURRENTE,
                        diasSemana = dias,
                        hora = horaElegida,
                        minuto = minutoElegido
                    )
                }

                ProgramacionStore.agregar(this, nuevo)
                AlarmScheduler.programar(this, nuevo)
                refrescarLista()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
