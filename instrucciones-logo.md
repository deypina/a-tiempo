# Instrucciones para Claude Code — Logo nuevo de A Tiempo

Copia y pega todo lo que está debajo de la línea en Claude Code, dentro del proyecto de la app.

---

Cambié el logo de A Tiempo: ahora es la letra "A" en Literata con una cápsula (mitad ocre) como travesaño, sobre fondo verde botica. Ya dejé los archivos en el proyecto; necesito que los revises, compiles y guardes.

Archivos que ya cambié:
- `app/src/main/res/drawable/ic_launcher_foreground.xml`: logo nuevo (reemplaza el reloj), centrado dentro de la zona segura de 66 dp.
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`: nuevo, versión de un solo color para los íconos temáticos de Android 13+.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`: ahora la capa `monochrome` usa `ic_launcher_monochrome`.
- `app/src/main/res/values/colors.xml`: `launcher_background` pasa a `#2E5A44` (el verde botica de la app).
- `index.html` (página de descarga) con el estilo libreta, trato de usted, datos corregidos y el logo nuevo.
- `logo.svg` en la raíz: el logo para la página y como ícono de la pestaña del navegador.

Qué necesito:
1. Revisa que los dos vectores sean válidos y que el ícono no se recorte con máscara circular, de gota o cuadrada. Si algo se sale de la zona segura, ajústalo.
2. Si el proyecto tiene íconos PNG antiguos en carpetas `mipmap-*dpi` (para Android 7 o menos, minSdk 26 no los necesita), dime si quedaron y si conviene borrarlos.
3. Compila (`./gradlew assembleDebug`) y corre lint.
4. Si la página de descarga se publica desde otra carpeta o rama, dime dónde debe ir `index.html` junto con `logo.svg`.
5. Haz commits pequeños en español, por ejemplo: `feat: logo nuevo con la A y la cápsula` y `feat: página de descarga con estilo libreta`. Incluye también `instrucciones-acerca-de.md`, `instrucciones-estilo-libreta.md` y este archivo, que todavía no están en git.
6. Dime en lenguaje sencillo cómo veo el ícono nuevo en mi teléfono (si hay que desinstalar e instalar de nuevo o basta con actualizar).
