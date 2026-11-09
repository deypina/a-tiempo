# Seguridad

A Tiempo guarda información de salud (nombres de medicamentos y horarios), así que la tratamos con cuidado:

- La app **no pide permiso de internet**. Cada compilación lo verifica y falla si alguna vez aparece.
- Los datos se guardan solo en el teléfono. No hay cuentas, servidores, analítica ni anuncios.
- La copia de seguridad de Google solo se activa cuando puede ir cifrada de extremo a extremo (el teléfono debe tener bloqueo de pantalla).
- El APK publicado siempre se firma con la misma llave, guardada fuera del repositorio.

## Cómo reportar un problema de seguridad

No abra un issue público. Use **Security → Report a vulnerability** en este repositorio de GitHub para enviarlo en privado. Respondemos lo antes posible.
