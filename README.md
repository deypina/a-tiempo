# A Tiempo

App Android gratuita y de código abierto que le recuerda a una persona tomar sus pastillas a la hora que elija y le avisa antes de que se le acaben.

- Recordatorio con botones **Ya la tomé** y **Recordar en X min**.
- Lleva la cuenta de cuántas pastillas quedan y cuántos días le alcanzan.
- Avisa cuando quedan pocas y con urgencia cuando casi se acaban, hasta que registre la compra.
- Funciona sin internet. Los datos se quedan solo en el teléfono.
- Letra grande, botones grandes y trato de usted.

> A Tiempo solo ayuda a organizarse. No reemplaza las indicaciones médicas.

## Para familias: cómo instalarla

1. En el teléfono Android, abra la sección **Releases** de este repositorio y descargue el archivo `A-Tiempo.apk`.
2. Ábralo. Si Android pregunta, permita "instalar apps de origen desconocido" para el navegador.
3. Abra A Tiempo, escriba el nombre o apodo de la persona y acepte los permisos.
4. **Importante en Samsung y otras marcas:** Ajustes → Aplicaciones → A Tiempo → Batería → **Sin restricciones**. Si no, los avisos pueden llegar tarde.
5. Agregue cada medicamento con su hora y cuántas pastillas tiene.

## Para desarrolladores

Este repositorio se construye con Claude Code. Empiece por:
- `CLAUDE.md` — instrucciones del proyecto.
- `docs/PRD.md` — qué hace la app.
- `docs/ESPECIFICACION_TECNICA.md` — cómo está construida.
- `docs/PLAN_DE_CONSTRUCCION.md` — fases y prompts.

Compilar: `./gradlew assembleDebug`

## Licencia

MIT
