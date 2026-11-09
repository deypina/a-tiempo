# Plan de construcción con Claude Code

Cada fase trae un **prompt listo para pegar** en Claude Code. Pega uno, espera a que termine, prueba lo que te pida y recién entonces pasa al siguiente. Si algo no te gusta, díselo con tus palabras antes de avanzar.

---

## Antes de empezar (una sola vez)

En tu computadora necesitas:
1. **Claude Code** instalado y con sesión iniciada.
2. **Git** y una cuenta de **GitHub**.
3. **Android Studio** (gratuito). Solo para que se instale el SDK de Android y Java; no tienes que usarlo. Ábrelo una vez, deja que termine la configuración inicial y ciérralo.
4. Abre la terminal en la carpeta del kit (`a-tiempo`) y escribe `claude`.

> Alternativa sin Android Studio: Claude Code puede escribir todo el código y GitHub Actions lo compila. En ese caso, en cada fase pídele "no compiles localmente; sube los cambios y revisa que la compilación en GitHub pase".

---

## Fase 0 — Preparar el repositorio

```
Lee CLAUDE.md y todos los archivos de docs/. Luego:
1. Inicializa git y crea el repositorio "a-tiempo" en mi GitHub (público), usando gh. Si gh no está instalado o no tengo sesión, dime paso a paso cómo hacerlo.
2. Haz el primer commit con el kit tal como está.
3. Resúmeme en 5 líneas qué entendiste de la app y qué harás en la Fase 1.
```

## Fase 1 — Proyecto base y compilación automática

```
Fase 1 del plan: crea el proyecto Android base según la especificación técnica (Kotlin, Compose, Material 3, Room con KSP, paquete ec.atiempo.app, minSdk 26). Solo una pantalla "Hola" con la navegación inferior de 3 pestañas vacías. Configura la firma release por variables de entorno como dice la especificación. Verifica que .github/workflows/build-apk.yml compile el proyecto. Compila, sube a GitHub y confirma que la acción en GitHub terminó en verde.
```
**Tú pruebas:** en GitHub → pestaña Actions → la última ejecución en verde → descarga el artefacto "a-tiempo-apk", instálalo en tu A37 y verifica que abre.

## Fase 2 — Medicamentos e inventario

```
Fase 2: implementa el modelo de datos completo (Room), la capa de dominio con sus pruebas unitarias (confirmar toma, idempotencia, deshacer, umbrales con cruces dobles y medias pastillas, compra, ajuste, días restantes), y las pantallas "Mis medicamentos" y "Editar medicamento" con los textos de docs/TEXTOS_UI.md. Todavía sin notificaciones. Corre las pruebas, compila y súbelo.
```
**Tú pruebas:** casos C-01 a C-06 de `docs/CASOS_DE_PRUEBA.md`.

## Fase 3 — Recordatorios

```
Fase 3: implementa alarmas exactas, canales de notificación, la notificación de toma con los botones "Ya la tomé" y "Recordar en X min", repeticiones hasta maxRepeats y estado "No confirmada". Reprogramación al reiniciar, cambiar hora/zona y actualizar la app. Agrega la pantalla de bienvenida con nombre, permisos y la guía de batería para Samsung. Compila y súbelo.
```
**Tú pruebas:** casos C-07 a C-14. Pon una hora 2–3 minutos en el futuro para no esperar.

## Fase 4 — Avisos de compra

```
Fase 4: implementa los avisos de compra normal, urgente (diario a la hora configurada) y sin stock, el botón "Ya compré" desde la notificación con la pantalla para escribir la cantidad (atajos 5, 10, 14, 20, 28, 30), y "Corregir cantidad". Compila y súbelo.
```
**Tú pruebas:** casos C-15 a C-21.

## Fase 5 — Hoy, historial, ajustes y accesibilidad

```
Fase 5: completa la pantalla Hoy (estados, "Ya la tomé", deshacer, línea de stock y días), el historial por medicamento, Ajustes (nombre, minutos de posponer, hora del aviso urgente, tamaño de letra, permisos, Acerca de) y revisa accesibilidad: botones de 56 dp, texto 18 sp, TalkBack, letra del sistema grande. Compila y súbelo.
```
**Tú pruebas:** casos C-22 a C-28.

## Fase 6 — Llave de firma y versión 1.0

```
Fase 6: ayúdame a crear la llave de firma definitiva con keytool, explícame dónde guardarla para no perderla, y guíame para cargar los 4 secretos en GitHub (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD). Luego actualiza el README para familias y crea la etiqueta v1.0.0 para que se publique el APK firmado en Releases.
```
**Tú pruebas:** descarga el APK desde Releases, desinstala la versión de prueba, instala la 1.0 en tu A37 y corre toda la lista de casos. Después instálala en el A16 de tu suegra y configúrale sus medicamentos.

## Fase 7 (v1.1) — Respaldo

```
Fase 7: agrega exportar e importar respaldo en JSON usando el selector de archivos de Android (sin permisos de almacenamiento). Sube versión a 1.1.0.
```

---

## Cómo pedir cambios

Describe lo que viste y lo que esperabas, por ejemplo:
> "En el A16 la notificación de las 9 p. m. llegó a las 9:07. Esperaba que llegara a las 9:00."

Claude Code debe proponer la causa, corregir, agregar una prueba si aplica y subir los cambios.
