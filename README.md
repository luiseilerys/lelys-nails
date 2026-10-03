# Lely's Nails 💅

Agenda de turnos para el salón de uñas **Lely's Nails**.

**Aplicación Android 100% nativa** escrita en **Kotlin** + Material Design 3.  
Sin HTML, sin WebView, sin Capacitor.

## Características

- Calendario mensual nativo (RecyclerView)
- Estados visuales: libre / 1 turno / completo / no laborable
- Máximo **2 clientas por día**
- Miércoles y domingos no laborables (fijos)
- Días no laborables personalizados
- Bottom sheets nativos para turnos y formulario
- Datos en SharedPreferences (dispositivo)

## Stack

| Tecnología | Uso |
|------------|-----|
| Kotlin | Lógica |
| Material Design 3 | UI |
| ViewBinding | Layouts |
| SharedPreferences + JSON | Persistencia |
| minSdk 26 | Android 8.0+ |

## Generar el APK

### GitHub Actions (recomendado)

1. Ve a **Actions** → **Build APK**
2. **Run workflow** → **Run workflow**
3. Espera ~3–5 min
4. Descarga el artefacto **lelys-nails-apk** → `app-debug.apk`
5. Instálalo en el móvil Android

### Localmente

```bash
# Requiere JDK 17 + Android SDK
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Estructura del proyecto

```
app/src/main/
  java/com/lelysnails/agenda/
    MainActivity.kt
    data/AppointmentStore.kt
    model/
    ui/CalendarAdapter.kt
  res/
    layout/          # XML nativos
    drawable/        # Fondos e iconos vectoriales
    values/          # colores, strings, temas
```

## Licencia

Uso privado para Lely's Nails.
