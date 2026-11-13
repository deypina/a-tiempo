# Instrucciones para Claude Code — A Tiempo

Copia y pega todo lo que está debajo de la línea en Claude Code, dentro del proyecto de la app.

---

Necesito tres cambios en la app A Tiempo. Antes de empezar, revisa cómo está organizada la app y dime qué archivos vas a tocar.

## 1. Nueva pantalla "Acerca de"

- Se entra desde un ícono discreto (por ejemplo, "i" o tres puntos) en la pantalla principal. No debe estorbar el uso diario.
- Mantén el mismo estilo de la app: letra grande, alto contraste, lenguaje sencillo y trato de "usted".
- Contenido, en este orden:
  1. Nombre de la app y número de versión (tomado automáticamente de la versión de la app).
  2. Texto: "Hecha con cariño por Dey Piña · @deykpina".
  3. Botón principal grande: **"Enviar mi opinión"**. Debe abrir la app de correo del teléfono con:
     - Para: `TU-CORREO@gmail.com`
     - Asunto: `[A Tiempo] Mi opinión`
     - Cuerpo ya escrito: dos líneas en blanco para que la persona escriba y, debajo, "Versión de la app: X · Android: Y · Teléfono: marca y modelo".
     - Si el teléfono no tiene app de correo, muestra un mensaje amable que sugiera escribir por Instagram a @deykpina.
  4. Fila de botones más pequeños que abren el navegador o la app correspondiente:
     - Instagram: https://instagram.com/deykpina
     - LinkedIn: https://www.linkedin.com/in/deykpina
     - X: https://x.com/deykpina
     - Facebook: https://facebook.com/deykpina
     - Código en GitHub: https://github.com/deykpina/a-tiempo
  5. Aviso al final, en letra normal: "A Tiempo es una herramienta de apoyo y no reemplaza las indicaciones de su médico. Sus datos se guardan solo en este teléfono."
- No agregues analítica, rastreo ni conexiones a internet. La app debe seguir siendo 100% local; solo se abren enlaces cuando la persona toca un botón.

## 2. Nombre fijo del APK para publicar

- Configura la compilación de lanzamiento (release) para que el archivo final se llame siempre `A-Tiempo.apk`.
- Explícame paso a paso, en lenguaje no técnico, cómo crear un Release en GitHub y subir ese APK. El nombre debe ser exactamente ese, porque la página de descarga apunta a `releases/latest/download/A-Tiempo.apk`.

## 3. Página de descarga con GitHub Pages

- Crea una carpeta `docs/` en el repositorio y pon dentro el archivo `index.html` que te voy a pasar.
- Explícame cómo activar GitHub Pages desde la carpeta `docs/` de la rama principal y qué dirección web va a tener la página.
- Si subo un video `video.mp4` a esa misma carpeta, aparecerá en la página. Avísame si el archivo pesa demasiado para GitHub.

Al terminar, dame un resumen de lo que cambiaste y cómo probarlo en mi teléfono.
