# Cómo integrar la Programación en tu proyecto

No tengo el `AndroidManifest.xml` ni el `activity_main.xml` originales (no se subieron),
así que aquí tienes exactamente qué copiar y dónde.

## 1. Copiar los archivos nuevos

Copia todo lo de `app/src/main/java/com/pedidos/` (8 archivos `.kt`) y todo lo de
`app/src/main/res/` (layouts + xml + strings) a las mismas rutas dentro de tu proyecto.
Sustituye tu `app/build.gradle` por el que va aquí (solo le añade la línea de RecyclerView).

## 2. Añadir esto a tu `AndroidManifest.xml`

Dentro de `<manifest>`, junto a los demás `<uses-permission>`:

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

Dentro de `<application>`, junto a tus otras `<activity>`:

```xml
<activity
    android:name=".ProgramacionActivity"
    android:exported="false" />

<receiver
    android:name=".EnvioProgramadoReceiver"
    android:exported="false" />

<receiver
    android:name=".BootReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>

<service
    android:name=".WhatsAppAutoEnviarService"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

## 3. Añadir el botón "Programación" en `activity_main.xml`

Añade un botón como el que ya tengas para "Ajustes", por ejemplo:

```xml
<Button
    android:id="@+id/botonProgramacion"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Programación" />
```

## 4. Enganchar el botón en `MainActivity.kt`

Dentro de `onCreate`, junto a donde abres `SettingsActivity`, añade:

```kotlin
binding.botonProgramacion.setOnClickListener {
    startActivity(Intent(this, ProgramacionActivity::class.java))
}
```

## 5. Lo que verá tu padre la primera vez

1. Al abrir "Programación" por primera vez, si el móvil es Android 12 o superior,
   se le pedirá activar el permiso de **"Alarmas y recordatorios"** (se abre solo
   la pantalla de Ajustes correspondiente).
2. Verá un aviso amarillo: **"Activa el servicio de accesibilidad"**. Al tocarlo,
   se abre Ajustes > Accesibilidad, donde debe buscar el nombre de la app y activarlo.
   Es un único paso, una sola vez.
3. A partir de ahí, cualquier pedido programado se enviará solo, sin tocar nada,
   el día y la hora indicados.

## Nota sobre fiabilidad

- Si Android "mata" la app en segundo plano de forma muy agresiva (pasa en algunas
  marcas como Xiaomi, Huawei o Samsung con el ahorro de batería), puede que la alarma
  no despierte el envío a tiempo. Si ves que falla, desactiva la optimización de
  batería para esta app en Ajustes del móvil.
- Si en algún momento subís la app a Google Play, ten en cuenta que el uso de un
  servicio de accesibilidad para este fin puede requerir justificación adicional
  en el proceso de revisión de Google.
