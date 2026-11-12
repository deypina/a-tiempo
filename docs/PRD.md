# PRD — A Tiempo

**Versión:** 1.0 · **Fecha:** 9 de noviembre de 2026 · **Responsable:** Dey Piña
**Plataforma:** Android (app nativa) · **Licencia prevista:** código abierto (MIT)

---

## 1. Resumen

A Tiempo es una app Android sencilla que le recuerda a una persona tomar sus medicamentos a la hora que ella eligió y lleva la cuenta de cuántas pastillas le quedan. Cuando el inventario baja, le avisa que compre, primero en tono normal y después con urgencia, hasta que registre la compra.

Es un **inventario que habla**: no reemplaza al médico ni da indicaciones de salud; solo ordena lo que la persona ya decidió tomar.

## 2. Problema

La usuaria inicial (la suegra de Dey, con artrosis) toma 3 medicamentos con horarios definidos por ella (por ejemplo, gabapentina alrededor de las 9:00 p. m.). Su problema no es médico sino de organización:

- No lleva control de cuántas pastillas le quedan.
- Se da cuenta de que se acabaron justo cuando va a tomarlas.
- Con dolor fuerte no puede salir a comprar en ese momento.

Apps genéricas existen, pero son complejas, en inglés, con cuentas, anuncios o suscripciones. Telegram o WhatsApp no sirven: su teléfono tiene poca memoria y la API de WhatsApp exige número dedicado, aprobación de Meta y costos por mensaje.

## 3. Objetivos

| Objetivo | Cómo se mide |
|---|---|
| Que nunca se quede sin pastillas | 0 días sin stock en las primeras 8 semanas de uso |
| Que tome sus pastillas a la hora que eligió | ≥ 90 % de tomas confirmadas en el historial |
| Que la use sola, sin ayuda diaria | Después de la configuración inicial, ella registra sus compras sin asistencia |
| Que no pese en su teléfono | APK < 15 MB, sin internet, sin cuenta |

## 4. No objetivos (v1)

- No usa inteligencia artificial.
- No tiene servidor, cuentas, nube ni sincronización.
- No avisa a familiares o cuidadores.
- No da consejos médicos, dosis ni interacciones.
- No escanea recetas ni códigos de barras.
- No es multiusuario (una persona por teléfono).

## 5. Usuaria principal

- Persona mayor, usuaria básica de Android (Samsung Galaxy A16).
- Dolor en manos por artrosis: necesita **botones grandes** y pocos toques.
- Prefiere que le hablen de **usted** y por su **nombre o apodo**.
- Configuración inicial hecha con ayuda de un familiar; uso diario autónomo.

## 6. Funcionalidades

> Los textos de esta sección son de referencia. La redacción exacta que ve la usuaria está en `docs/TEXTOS_UI.md`, que manda en caso de diferencia.

### F1 — Bienvenida y nombre
- Al abrir por primera vez pregunta: "¿Cómo le gustaría que le llamemos?" (campo libre: nombre o apodo).
- El nombre aparece en notificaciones y en la pantalla principal. Editable en Ajustes.
- Guía para conceder permisos (notificaciones, alarmas exactas, excluir de ahorro de batería) con explicación en lenguaje simple.

### F2 — Medicamentos
Por cada medicamento:
- Nombre (texto libre, ej. "Gabapentina").
- Cantidad por toma: ½, 1, 1½, 2, o número libre (admite medias).
- Uno o varios horarios diarios (ej. 9:00 p. m.; o 8:00 a. m. y 8:00 p. m.).
- Inventario actual (número, admite medias).
- Umbral de aviso (por defecto 5) y umbral urgente (por defecto 3).
- Nota opcional (ej. "con comida").
- Se puede pausar (deja de recordar sin borrar historial) y eliminar.

### F3 — Recordatorio de toma
- A la hora programada llega una notificación: "{Nombre}, es hora de su Gabapentina. Tome 1 pastilla."
- Botones dentro de la notificación: **✅ Ya la tomé** y **⏰ Recordar en {X} min**.
- **Ya la tomé:** registra la toma con fecha y hora y descuenta la cantidad del inventario.
- **Recordar en X min:** no descuenta; vuelve a sonar en X minutos. X lo elige la usuaria en Ajustes (5, 10, 15, 30; por defecto 15).
- **Sin respuesta:** se repite hasta 3 veces (cada X minutos). Si sigue sin respuesta, la toma queda como "No confirmada" y no descuenta.
- La notificación es de alta prioridad, con sonido y vibración, y visible en pantalla bloqueada.

### F4 — Pantalla "Hoy"
- Lista de tomas del día en orden horario con estado: Pendiente · Tomada · Pospuesta · No confirmada.
- Botón grande para marcar "Ya la tomé" desde la app (si no vio la notificación).
- **Deshacer** una toma marcada por error (devuelve la cantidad al inventario).
- Debajo de cada medicamento: "Le quedan 7 pastillas · le alcanza para 7 días".

### F5 — Inventario y avisos de compra
- Cada toma confirmada descuenta la cantidad por toma.
- El inventario nunca baja de 0.
- Cálculo "días que le alcanza" = inventario ÷ consumo diario (cantidad × número de horarios).
- **Aviso normal** al cruzar el umbral de aviso: "Le quedan 5 pastillas de Gabapentina. {Nombre}, recuerde comprar pronto." con botón **Ya compré**.
- **Aviso urgente** al cruzar el umbral urgente sin compra registrada: "Solo le quedan 3 de Gabapentina. {Nombre}, compre hoy para no quedarse sin ellas." Se repite **una vez al día** (hora configurable, por defecto 10:00 a. m.) hasta que registre compra o el inventario vuelva a superar el umbral.
- **Sin stock** (0): "Se le acabaron las pastillas de Gabapentina. {Nombre}, ya no le quedan pastillas. Compre hoy." El recordatorio de toma sigue sonando, con aviso de que no hay stock.

### F6 — Registrar compra
- Desde la notificación ("Ya compré") o desde el medicamento.
- Pregunta: "¿Cuántas pastillas compró?" con atajos grandes (5, 10, 14, 20, 28, 30) y campo libre.
- Suma al inventario y apaga los avisos de compra pendientes.
- Queda en el historial.

### F7 — Corrección de inventario
- "Corregir cantidad": la usuaria escribe cuántas tiene realmente (por si se cayó una pastilla o la cuenta no cuadra). Queda registrado como ajuste.

### F8 — Historial
- Por medicamento: tomas (con estado), compras y ajustes, más recientes primero.
- Útil para mostrar al médico o para que un familiar revise si ella lo pide.

### F9 — Ajustes
- Nombre o apodo.
- Minutos para "Recordar en…".
- Hora del aviso diario de compra urgente.
- Tamaño de letra: Normal / Grande / Muy grande.
- Reconfigurar permisos y ahorro de batería.
- Copia de seguridad: exportar / importar archivo (v1.1).
- Acerca de: versión, autora, botón "Enviar mi opinión" (abre el correo del teléfono), enlaces a las redes de la autora y al repositorio, y aviso de que la app no reemplaza indicaciones médicas.

## 7. Requisitos no funcionales

- **Sin internet:** la app no declara permiso de INTERNET.
- **Privacidad:** todos los datos se guardan solo en el teléfono. Sin analítica, sin anuncios.
- **Fiabilidad de alarmas:** usar alarmas exactas; reprogramar al reiniciar el teléfono, al cambiar hora o zona horaria y al actualizar la app.
- **Accesibilidad:** botones de mínimo 56 dp de alto, texto base 18 sp, alto contraste, compatible con TalkBack y con el tamaño de letra del sistema.
- **Idioma:** español (Ecuador), trato de usted, formato de hora de 12 h con a. m./p. m. según configuración del teléfono.
- **Compatibilidad:** Android 8.0 (API 26) en adelante. Probado en Samsung Galaxy A16 y A37.
- **Peso:** APK < 15 MB.
- **Actualizable sin perder datos:** el APK debe firmarse siempre con la misma llave.

## 8. Riesgos

| Riesgo | Mitigación |
|---|---|
| Samsung suspende la app y la alarma no suena | Pantalla de configuración que lleva a "Batería → Sin restricciones"; prueba en A16 real |
| La usuaria toca "Ya la tomé" sin tomarla o dos veces | Idempotencia por toma (no descuenta dos veces) y botón Deshacer |
| El inventario se desfasa de la realidad | Corrección manual sencilla |
| Se pierde el teléfono o se reinstala | Respaldo automático de Android + exportar archivo (v1.1) |
| Actualizar la app borra datos | Firma con llave fija guardada en secretos de GitHub |
| Uso como consejo médico | Aviso visible en Acerca de y en la bienvenida |

## 9. Distribución

1. **Fase 1:** APK instalado directamente en el A37 de Dey (pruebas) y luego en el A16 de la usuaria.
2. **Fase 2:** repositorio público en GitHub con el APK en "Releases" e instrucciones para familias.
3. **Fase 3 (opcional):** Play Store — cuenta de desarrollador (pago único), prueba cerrada previa con testers, política de privacidad y declaración de app de salud.

## 10. Hitos

| Hito | Contenido |
|---|---|
| M1 | Proyecto base + compilación automática en GitHub |
| M2 | Medicamentos e inventario (sin notificaciones) |
| M3 | Recordatorios con botones, posponer, repeticiones |
| M4 | Avisos de compra, registro de compra, corrección |
| M5 | Pantalla Hoy, historial, ajustes, accesibilidad |
| M6 | Pruebas en A37 y A16, ajuste fino, versión 1.0 |
| M7 | Repo público + guía para familias |
