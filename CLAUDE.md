# CLAUDE.md — Instrucciones para Claude Code

Estás construyendo **A Tiempo**, una app Android de recordatorios de medicamentos con inventario. Lee esto completo antes de escribir código.

## Contexto de la persona que dirige el proyecto

- Dey (dueña del proyecto) **no escribe código**. Explícale en español, en lenguaje claro, qué hiciste y qué tiene que probar. No le pidas que edite archivos a mano.
- Cuando necesites que ella haga algo (instalar algo, crear un secreto en GitHub, probar en el teléfono), dale pasos numerados y concretos.
- Trabaja por fases según `docs/PLAN_DE_CONSTRUCCION.md`. Al terminar cada fase: compila, corre pruebas, haz commit y resume en 5 líneas máximo.

## Documentos fuente (en orden de autoridad)

1. `docs/PRD.md` — qué hace la app y por qué.
2. `docs/ESPECIFICACION_TECNICA.md` — cómo se construye.
3. `docs/TEXTOS_UI.md` — todos los textos visibles. **No inventes textos**: usa los de ese archivo; si falta uno, agrégalo ahí primero con el mismo estilo.
4. `docs/CASOS_DE_PRUEBA.md` — lo que debe funcionar antes de dar una fase por terminada.

Si encuentras una contradicción entre documentos, pregunta antes de decidir.

## Stack (no cambiar sin preguntar)

- Kotlin, Jetpack Compose, Material 3.
- Room (con KSP) para datos.
- AlarmManager (`setExactAndAllowWhileIdle`) para recordatorios. **No** WorkManager para las tomas (no es exacto).
- Hilt **no**: inyección manual simple (un `AppContainer`). El proyecto es pequeño.
- minSdk 26, targetSdk y compileSdk = la última versión estable disponible.
- Gradle con Kotlin DSL y version catalog (`gradle/libs.versions.toml`).
- Paquete: `ec.atiempo.app`.

## Reglas firmes

- **Sin permiso de INTERNET.** Sin librerías de analítica, anuncios ni Firebase.
- Todo texto visible va en `res/values/strings.xml` (español, trato de usted). Nada de textos sueltos en el código.
- Cantidades como `Double` con pasos de 0.5; mostrar "½" y "1½" en la interfaz, nunca "0.5".
- El inventario nunca baja de 0.
- Cada toma programada tiene un id único (`medicationId + scheduleId + fecha`). Confirmarla dos veces **no** descuenta dos veces.
- Toda lógica de inventario y avisos vive en una capa de dominio pura (sin Android) con pruebas unitarias.
- Las alarmas se reprograman en: arranque del teléfono, cambio de hora/zona, actualización de la app y al editar un medicamento.
- Botones de mínimo 56 dp de alto, texto base 18 sp, contraste alto. Respeta el tamaño de letra del sistema.
- No uses IA ni llamadas a modelos en la app.
- Cumple la sección 9 de `docs/ESPECIFICACION_TECNICA.md` (seguridad y privacidad) en cada fase.

## Comandos

- Compilar: `./gradlew assembleDebug`
- Pruebas unitarias: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- APK resultante: `app/build/outputs/apk/debug/app-debug.apk`

## Git

- Commits pequeños en español: `feat: registrar compra desde notificación`.
- Nunca subas `*.keystore`, `*.jks`, `keystore.properties` ni `local.properties`.
- Versiones con etiquetas `v1.0.0`; al subir una etiqueta, GitHub Actions publica el APK firmado en Releases.
