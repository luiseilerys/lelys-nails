# Lely's Nails 💅

Agenda de turnos para el salón de uñas **Lely's Nails**.

Aplicación móvil Android (APK) generada con [Capacitor](https://capacitorjs.com/) a partir de una web app.

## Características

- Calendario mensual con estados visuales (libre / 1 turno / completo / no laborable)
- Máximo **2 clientas por día**
- Miércoles y domingos no laborables (fijos)
- Días no laborables personalizados
- Datos guardados en el dispositivo (localStorage)
- Diseño móvil optimizado

## Generar el APK

### Opción 1: GitHub Actions (recomendado)

1. Ve a la pestaña **Actions** de este repositorio
2. Selecciona el workflow **Build APK**
3. Haz clic en **Run workflow** → **Run workflow**
4. Cuando termine, descarga el artefacto **lelys-nails-apk**
5. Dentro encontrarás `app-debug.apk` — instálalo en tu Android

> La primera vez puede tardar 3–5 minutos (descarga de SDK y dependencias).

### Opción 2: Localmente

Requisitos: Node.js 20+, Java 17+, Android SDK.

```bash
npm install
npx cap add android
npx cap sync
cd android && ./gradlew assembleDebug
```

El APK quedará en:
`android/app/build/outputs/apk/debug/app-debug.apk`

## Estructura

```
www/                 ← Código de la app (HTML/CSS/JS)
capacitor.config.json
package.json
.github/workflows/build-apk.yml
```

## Licencia

Uso privado para Lely's Nails.
