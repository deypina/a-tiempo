# Especificación técnica — A Tiempo

## 1. Arquitectura

```
ui/            Pantallas Compose + ViewModels
domain/        Lógica pura (Kotlin, sin Android): inventario, umbrales, cálculo de próximas tomas
data/          Room (entidades, DAOs), repositorios, preferencias (DataStore)
alarms/        AlarmScheduler, receptores (alarma, acciones de notificación, arranque)
notifications/ Canales, construcción de notificaciones
AppContainer   Inyección manual (un objeto creado en Application)
```

Flujo: la UI y los receptores llaman a **casos de uso** del dominio a través de los repositorios. Ni la UI ni los receptores modifican la base de datos directamente.

## 2. Modelo de datos (Room)

### `Medication`
| Campo | Tipo | Nota |
|---|---|---|
| id | Long (PK, autogen) | |
| name | String | |
| dosePerIntake | Double | 0.5, 1.0, 1.5, 2.0… |
| stock | Double | ≥ 0 |
| warnThreshold | Double | por defecto 5 |
| urgentThreshold | Double | por defecto 3; debe ser < warnThreshold |
| note | String? | |
| active | Boolean | false = pausado |
| restockPending | Boolean | true desde que cruza el umbral de aviso hasta registrar compra |
| colorKey | String | color para reconocerlo: `azul`, `rosa`, `mostaza`, `oliva`, `ciruela` o `blanco` (versión 2 de la base de datos) |
| createdAt | Long (epoch ms) | |

### `Schedule`
| Campo | Tipo |
|---|---|
| id | Long (PK) |
| medicationId | Long (FK, cascade) |
| hour | Int (0–23) |
| minute | Int |

### `IntakeEvent` (una fila por toma programada del día)
| Campo | Tipo | Nota |
|---|---|---|
| id | String (PK) | `"{medId}-{scheduleId}-{yyyyMMdd}"` → idempotencia |
| medicationId | Long | |
| scheduleId | Long | |
| scheduledAt | Long | |
| status | Enum | PENDING, TAKEN, SNOOZED, MISSED |
| snoozeCount | Int | |
| takenAt | Long? | |
| quantity | Double | cantidad descontada (para poder deshacer) |

### `StockMovement`
| Campo | Tipo | Nota |
|---|---|---|
| id | Long (PK) | |
| medicationId | Long | |
| type | Enum | INTAKE, PURCHASE, ADJUSTMENT, UNDO |
| delta | Double | negativo para tomas |
| resultingStock | Double | |
| createdAt | Long | |

### Preferencias (DataStore)
`userName`, `snoozeMinutes` (15), `maxRepeats` (3), `urgentReminderHour` (10:00), `fontScale` (NORMAL/LARGE/XLARGE), `appearance` (LIGHT por defecto, DARK, SYSTEM), `onboardingDone`.

`ATiempoTheme` recibe `appearance` igual que recibe `fontScale`. LIGHT arranca en claro aunque el teléfono esté en oscuro; SYSTEM sigue el modo del teléfono.

### Versiones de la base de datos y migraciones
- **Versión 1:** modelo inicial.
- **Versión 2:** agrega `colorKey` a `Medication`.
- **Migración v1→v2** (`Migration(1, 2)`): agrega la columna `colorKey` y asigna colores distintos a los medicamentos que ya existen, en orden de `id`, siguiendo el orden de la paleta (azul, rosa, mostaza, oliva, ciruela, blanco; si hay más de seis, vuelve a empezar). Conserva todos los medicamentos, horarios, tomas, movimientos e inventario.
- La migración **nunca es destructiva**: está prohibido `fallbackToDestructiveMigration`. La persona no puede perder su inventario al actualizar.
- Room exporta el esquema de cada versión (`exportSchema`) para poder probar la migración.

## 3. Lógica de dominio (con pruebas unitarias)

### Confirmar toma
```
confirmIntake(eventId):
  si event.status == TAKEN → no hacer nada (idempotente)
  qty = min(med.dosePerIntake, med.stock)
  med.stock = max(0, med.stock - med.dosePerIntake)
  event.status = TAKEN, takenAt = ahora, quantity = qty
  registrar StockMovement(INTAKE, -qty)
  evaluarUmbrales(med, stockAnterior)
```

### Deshacer toma
```
undoIntake(eventId): si TAKEN → stock += quantity, status = PENDING, movimiento UNDO.
  Si el stock vuelve a superar warnThreshold → restockPending = false y cancelar avisos de compra.
```

### Evaluar umbrales (solo al cruzarlos hacia abajo)
```
si stockAnterior > warn  y stock <= warn  y stock > urgent → aviso NORMAL, restockPending = true
si stockAnterior > urgent y stock <= urgent y stock > 0    → aviso URGENTE + programar diario
si stock == 0                                              → aviso SIN STOCK + programar diario
```
Cruces dobles (p. ej. de 6 a 2 en una sola toma de 4) disparan solo el aviso más grave.

### Registrar compra
```
registerPurchase(medId, qty > 0): stock += qty, movimiento PURCHASE,
  restockPending = false y cancelar todos los avisos de compra (incluido el diario urgente)
```
Registrar una compra siempre apaga los avisos, aunque el stock siga bajo (PRD F5: "hasta que registre compra"). Si luego llega a 0, vuelve a avisar.

### Corregir inventario
`adjustStock(medId, newValue ≥ 0)`: movimiento ADJUSTMENT con delta = new − old; reevaluar umbrales (sin notificar si sube).

### Días que alcanza
`daysLeft = floor(stock / (dosePerIntake × schedules.size))`; si no hay horarios o está pausado → no mostrar.

### Primer color libre
`firstFreeColor(usados)`: devuelve el primer color de la paleta (azul, rosa, mostaza, oliva, ciruela, blanco) que no use ningún otro medicamento; si todos están usados, el primero de la paleta. Se usa al crear un medicamento para preseleccionar su color.

## 4. Alarmas y notificaciones

### Programación
- `AlarmScheduler.scheduleNext(schedule)`: calcula la próxima ocurrencia (hoy si no ha pasado, si no mañana) y usa `setExactAndAllowWhileIdle(RTC_WAKEUP, …)`.
- `requestCode` estable por schedule (`scheduleId.toInt()`); posponer usa `requestCode` distinto (`100000 + scheduleId`).
- Al dispararse la alarma: crear/obtener `IntakeEvent` del día (estado PENDING), mostrar notificación, programar la del día siguiente.
- Repeticiones: si no hay respuesta, la alarma de repetición suena cada `snoozeMinutes` hasta `maxRepeats` veces (`snoozeCount` cuenta las repeticiones sin respuesta). Un intervalo después de la última → status MISSED y notificación silenciosa "No confirmó su toma de…". Con 15 min y 3 repeticiones: 9:00, 9:15, 9:30, 9:45 y a las 10:00 queda "No confirmada".
- "Recordar en X min" deja la toma en SNOOZED y reinicia el conteo: la persona respondió. Al sonar de nuevo vuelve a PENDING con sus propias repeticiones.
- Las reglas viven en `domain/ReminderRules.kt` (puras, con pruebas). Cada cambio de una toma se hace en una transacción, así "Ya la tomé" y una repetición que llegan juntas no se pisan.
- Si al sonar la alarma el medicamento ya no existe, está pausado o el horario cambió, la alarma se descarta y no se vuelve a programar. Al guardar, pausar o eliminar se cancelan las alarmas de los horarios quitados.
- Abrir la app también reprograma todo (cubre "Forzar detención", que borra las alarmas sin avisar).
- Se guarda la hora de cada alarma diaria armada. Si al reprogramar esa hora ya pasó y la alarma no sonó (teléfono apagado, forzar detención, alarma inexacta tardía), se avisa en ese momento si pasaron 2 horas o menos; si pasó más, se salta para no pedir una toma muy tarde.
- Tras reiniciar, también se vuelven a armar las repeticiones de las tomas sin responder de las últimas 6 horas.
- Todas las entradas de `ReminderManager` corren bajo un mismo candado (`Mutex`), así una alarma y un toque que llegan juntos se atienden uno tras otro.
- Sin el permiso de alarmas exactas, Android puede retrasar mucho la alarma (no solo minutos). Por eso la bienvenida lo pide y Hoy muestra la advertencia hasta concederlo.
- Aviso diario de compra urgente: una alarma por medicamento a `urgentReminderHour` mientras `stock <= urgentThreshold` y no haya compra (`restockPending`), y el medicamento no esté pausado. `requestCode` = `300000 + medicationId`. Las reglas viven en `domain/PurchaseRules.kt`.
  - Al cruzar el umbral urgente o llegar a 0 se avisa en ese momento y la primera repetición es al día siguiente a esa hora. Si al sonar el stock es 0, muestra el aviso "Se le acabaron…".
  - Hay una sola notificación de compra por medicamento (id `300000 + medicationId`): el aviso más grave reemplaza al anterior.
  - Registrar una compra cancela la alarma y la notificación. Corregir la cantidad por encima del umbral de aviso también quita la notificación.
  - Igual que las tomas: se guarda la hora armada; al reprogramar, si estaba vencida hace 2 horas o menos se avisa en ese momento, y si sigue pendiente a la hora configurada se vuelve a armar en el mismo momento (un reinicio borra las alarmas).
- Si se edita un medicamento sin tocar el campo de cantidad, se conserva la cantidad de la base de datos, así una compra registrada desde la notificación mientras la pantalla estaba abierta no se pierde.
- La hora de la alarma de repetición o "Recordar" también se guarda (en las mismas preferencias privadas de alarmas, no en Room), y Hoy la muestra como "Le recordaremos a las…". Se guarda antes de cambiar la toma a SNOOZED.
- "Ya la tomé" desde Hoy hace lo mismo que desde la notificación. Si la alarma aún no sonó, primero crea la toma del día como PENDING; cuando la alarma suene, la encuentra TAKEN y no avisa. También quita el aviso "No confirmó su toma…".
- "Deshacer" (solo tomas TAKEN): devuelve la cantidad, deja la toma PENDING y registra el movimiento UNDO. Si la hora de la toma ya pasó, se vuelve a recordar después de `snoozeMinutes`, como cualquier toma sin responder. Después se ajustan los avisos de compra con la cantidad nueva.
- Al reprogramar tras un reinicio solo se vuelven a armar repeticiones de tomas cuya hora ya pasó: una toma futura (marcada antes y luego deshecha) espera a su alarma diaria.
- Cambiar `urgentReminderHour` en Ajustes reprograma los avisos diarios de compra a la hora nueva.

### Permisos
| Permiso | Motivo | Cómo |
|---|---|---|
| `POST_NOTIFICATIONS` | Android 13+ | Pedir en la bienvenida |
| `SCHEDULE_EXACT_ALARM` | Android 12+ | Verificar `canScheduleExactAlarms()`; si es false, abrir `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Si sigue denegado, usar `setAndAllowWhileIdle` y mostrar advertencia |
| `RECEIVE_BOOT_COMPLETED` | Reprogramar al reiniciar | Manifiesto |
| `USE_FULL_SCREEN_INTENT` | **No usar en v1** | — |

Escuchar también `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED`, `ACTION_MY_PACKAGE_REPLACED` y `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` para reprogramar todo.

> Nota para Play Store (fase 3): `USE_EXACT_ALARM` solo se permite a apps de alarma/calendario; por eso se usa `SCHEDULE_EXACT_ALARM`.

### Ahorro de batería (Samsung)
- En la bienvenida y en Ajustes: comprobar `PowerManager.isIgnoringBatteryOptimizations()`. Si es false, explicar y abrir `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, o la pantalla de detalles de la app como alternativa.
- Texto de ayuda específico para Samsung: Ajustes → Aplicaciones → A Tiempo → Batería → **Sin restricciones**. Además revisar que no esté en "Aplicaciones en suspensión profunda" (Ajustes → Batería → Límites de uso en segundo plano).

### Canales
| Canal | Importancia | Uso |
|---|---|---|
| `tomas` | HIGH, sonido + vibración | Recordatorios de toma |
| `compras` | HIGH | Avisos de compra |
| `info` | LOW | Tomas no confirmadas |

### Acciones de notificación
- `NotificationActionReceiver` maneja `ACTION_TAKEN`, `ACTION_SNOOZE`, `ACTION_PURCHASED`.
- Usar `goAsync()` y una corrutina para escribir en Room; terminar con `pendingResult.finish()`.
- "Ya compré" abre `PurchaseActivity` (no exportada, fuera de recientes) con la pantalla "¿Cuántas pastillas compró?". La misma pantalla se abre desde "Registrar compra" y "Corregir cantidad" en *Mis medicamentos*.
- Tocar la notificación abre la pantalla Hoy.

## 5. Interfaz

Pantallas: Bienvenida (nombre + permisos + batería) · Hoy · Medicamentos (lista) · Detalle del medicamento ("Ver más") · Editar medicamento · Registrar compra · Corregir cantidad · Historial · Ajustes · Acerca de.

Navegación inferior con 3 pestañas grandes: **Hoy**, **Mis medicamentos**, **Ajustes**.

- **Hora:** siempre en 12 h ("3:30 p. m.", "7:00 a. m."), sin importar la configuración del teléfono, en pantallas, notificaciones e historial. Una sola función en `ui/common/Formatting.kt` da formato a todas las horas: medianoche = "12:00 a. m.", mediodía = "12:00 p. m.", "1:25 p. m." sin cero adelante.
- **Hoy:**
  - Saludo: fecha pequeña arriba ("Jueves 12 de noviembre") y "Hola, {nombre}" en Literata.
  - Avisos importantes (bloque IMPORTANTE, ver diseño), si algún medicamento se acabó o le quedan pocas.
  - Tomas del día de los medicamentos activos como renglones: la hora en una columna a la izquierda (unos 78 dp, separada por una línea vertical fina; si con letra grande no cabe, la columna crece, nunca se recorta la hora) y a la derecha círculo de color + nombre, cantidad y estado.
  - La próxima toma pendiente (o atrasada) va primero y más grande: hora en verde con "AHORA" debajo (o "ATRASADA" en ladrillo si ya pasó su hora), nombre más grande, línea de inventario ("Quedan 2 · le alcanza para 1 día", en ocre si quedan pocas), botón grande "Ya la tomé" y debajo el enlace subrayado "Recordar en {x} minutos" (los minutos de Ajustes).
  - "Recordar en {x} minutos" solo aparece en la toma AHORA o ATRASADA y usa las mismas reglas que "Recordar" desde la notificación (`ReminderManager.onSnooze`).
  - El resto de tomas, en orden de hora, más compactas. Las tomadas dicen "✓ Tomada a las 7:10 a. m." en verde, con "Deshacer" como enlace subrayado a la derecha.
- **Mis medicamentos:**
  - Botón con borde "Agregar medicamento" arriba.
  - Cada medicamento es un renglón: círculo de color + nombre (Literata) + "Ver más ›" a la derecha; toda la fila de arriba es tocable y abre el detalle.
  - Debajo: "1 pastilla · 3:30 p. m." y la línea de inventario con su color de estado (verde normal, ocre quedan pocas, ladrillo "Se acabó · no le quedan pastillas").
  - Solo un botón visible por medicamento y solo cuando hace falta: "Registrar compra" (relleno si se acabó, con borde si quedan pocas). Si hay suficientes, no se muestra ningún botón.
- **Detalle del medicamento ("Ver más"):** pantalla nueva y sencilla (no reutiliza Editar medicamento). Muestra el nombre con su círculo de color, la cantidad por toma, los horarios, la línea de inventario y los botones "Registrar compra", "Corregir cantidad", "Ver historial" y "Editar".
- **Agregar / editar medicamento:** sección "Color para reconocerla" con seis círculos grandes (mínimo 48 dp) en una fila. El elegido lleva borde de tinta y un ✓ encima; TalkBack lee "{color}, elegido". Debajo, el texto de ayuda "Elija el color que más se parezca a la pastilla o a su caja.". Al crear, viene preseleccionado el primer color libre (dominio).
- **Historial:** se abre con "Ver historial" en el detalle del medicamento. Tomas, compras, correcciones y tomas deshechas salen de `StockMovement` (una toma deshecha sigue en el historial); las tomas "No confirmada" salen de `IntakeEvent`. Más recientes primero.
- **Ajustes:** cada cambio se guarda al momento. `fontScale` multiplica el tamaño de letra del sistema (Grande ×1,15; Muy grande ×1,3), así ambos se suman.
  - Sección "Apariencia" al inicio (después del nombre): tres botones grandes con ícono arriba y texto abajo: *Claro* (sol), *Oscuro* (luna), *Como el teléfono* (teléfono).
  - En todos los grupos de opciones (apariencia, minutos para recordar, tamaño de letra), la opción elegida se marca con borde verde de 2 dp, fondo verde muy suave y texto en negrita; nunca solo con un tono un poco más oscuro. TalkBack anuncia cuál está elegida.
  - "Aviso diario de compra", "Revisar permisos y batería" y "Acerca de A Tiempo" son renglones de lista (no botones con borde), con la hora del aviso a la derecha en verde.
- **Acerca de:** en este orden: nombre y versión (de la app instalada), `about_credit`, botón "Enviar mi opinión", fila de enlaces (Instagram, LinkedIn, X, GitHub) y `about_disclaimer`.
  - "Enviar mi opinión" abre la app de correo con `ACTION_SENDTO` (`mailto:deykpina.proyectos@gmail.com`), asunto `about_feedback_subject` y el cuerpo `about_feedback_body` con versión de la app, versión de Android y marca y modelo del teléfono. Nada se envía solo: la persona ve el borrador y decide. Sin app de correo, se muestra `about_no_email`.
  - Redes en `deykpina`; repositorio en `github.com/deypina/a-tiempo`.

Diseño "libreta" (`ui/theme/Theme.kt` y `ui/common/Paper.kt`): la app se ve como una libreta de papel rayada, no como una app genérica.

**Apariencia:** *Claro* por defecto; la persona elige en Ajustes *Claro*, *Oscuro* o *Como el teléfono* (preferencia `appearance`). La app arranca en claro aunque el teléfono esté en oscuro.

**Colores base** (se mantienen): papel `#F5EFE3` (noche `#17140F`), tinta `#1E1A15` (noche `#ECE4D6`), verde botica `#2E5A44` para la acción principal y "tomada", ladrillo `#A13A22` para avisos, con sus versiones de noche. Se agregan:

| Uso | Claro | Oscuro |
|---|---|---|
| Ocre: "quedan pocas" | `#8A5A12` | `#E3B25E` |
| Línea de renglón | `#C9BBA3` | `#3F3933` |
| Línea doble (arriba de cada lista y de la barra inferior) | tinta | tinta de noche |

**Colores de medicamento** (`colorKey`; en modo oscuro se usa la versión clara de cada uno):

| Clave | Nombre | Claro | Oscuro |
|---|---|---|---|
| `azul` | Azul | `#3D6A8E` | `#7FA6C8` |
| `rosa` | Rosa vieja | `#B0566B` | `#CF8396` |
| `mostaza` | Mostaza | `#C99A2E` | `#D9B158` |
| `oliva` | Oliva | `#6E7F3A` | `#A3B26A` |
| `ciruela` | Ciruela | `#7A5A8C` | `#A98BBA` |
| `blanco` | Blanco | `#FFFDF8` con borde | `#ECE4D6` |

Reglas de color:
- Todo texto tiene contraste de al menos 4,5:1 contra el papel, en claro y en oscuro (revisar en especial el ocre y el ladrillo).
- El color nunca va solo: siempre acompañado de texto ("Se acabó", "Quedan 2", "Tomada").
- Nada de degradados, sombras ni fondos teñidos detrás de los avisos.

**Forma:**
- **Sin tarjetas** (`Card`) para tomas ni medicamentos. Cada toma y cada medicamento es un renglón; entre renglones, una línea de 1 dp del color "línea de renglón". Arriba de cada lista, una línea doble de tinta (como el margen de una agenda).
- **Esquinas casi rectas:** 6 dp en botones y campos. Nada de formas de píldora, excepto los círculos de color de cada medicamento.
- **Botones:** son lo único que parece botón. Relleno verde para la acción principal, con borde verde para la secundaria, subrayado para acciones pequeñas ("Deshacer", "Recordar en 5 minutos"). Mínimo 56 dp de alto; los subrayados, mínimo 44 dp de zona táctil. Las pantallas usan los botones de `ui/common/Paper.kt`, no los de Material.
- **Bloque IMPORTANTE** (se acabó un medicamento, quedan pocas): no va en caja. Es un bloque de texto debajo del saludo: la palabra pequeña en mayúsculas "IMPORTANTE" en ladrillo y debajo el mensaje, con la parte clave en negrita y color. Si hay acción, va como enlace subrayado dentro del texto.
- **Barra inferior:** sin el indicador en forma de píldora de Material (`indicatorColor` transparente). La pestaña activa se marca con ícono y texto en verde y la palabra subrayada; las otras en gris cálido. Fondo del color del papel con línea doble arriba.

**Tipografía** (incluida en la app, no se descarga y no se agrega permiso de INTERNET):
- Títulos en *Literata*; texto en *Atkinson Hyperlegible* (diseñada para personas con baja visión).
- Los archivos `.ttf` van en `res/font`. Ambas tienen licencia SIL OFL 1.1; el texto de la licencia se guarda en el repositorio.
- El texto que la persona lee mide al menos 18 sp; las etiquetas cortas ("a. m.", "AHORA", "IMPORTANTE") miden de 15 a 16 sp. Se respeta el tamaño de letra del sistema.

## 6. Respaldo

- `android:allowBackup="true"` con reglas que incluyan la base de datos y DataStore (respaldo automático de Google).
- v1.1: exportar/importar JSON mediante Storage Access Framework (sin permisos de almacenamiento).

## 7. Firma y compilación

- Firma release leída de variables de entorno (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Si no existen, compilar release sin firmar no debe romper el build de debug.
- La llave se crea **una sola vez** y se guarda en un lugar seguro + secretos de GitHub. Perderla obliga a desinstalar (y perder datos) para actualizar.
- `.github/workflows/build-apk.yml` compila en cada push a `main` (artefacto) y publica en Releases al subir una etiqueta `v*`.
- El release usa R8 (`isMinifyEnabled` y `isShrinkResources`) para reducir tamaño y no es depurable. En cada push se compila sin firmar para detectar a tiempo problemas de R8.
- La llave es PKCS12 (alias `a-tiempo`, contraseña de la llave igual a la del almacén), creada con `openssl` por un script que corre la dueña en su Mac. Vive fuera del repositorio, con una copia de respaldo, y en los 4 secretos de GitHub.
- Etiquetas `v1.0.0-rcN` publican una versión de prueba marcada como *pre-release*: el botón de descarga (`releases/latest`) sigue apuntando a la última versión final.

## 8. Pruebas

- Unitarias del dominio: confirmar, idempotencia, deshacer, umbrales (incluidos cruces dobles y medias pastillas), compra, ajuste, días restantes, próxima ocurrencia de alarma (cambio de día, medianoche).
- Instrumentadas mínimas: DAO de Room.
- Migración de Room (en el emulador): crea, desde el esquema exportado `1.json`, una base de datos versión 1 con medicamentos, horarios, tomas y movimientos, la migra a la versión 2 y comprueba que todo sigue igual y que cada medicamento tiene un color distinto, en orden de `id`.
- Unitarias: primer color libre y formato de hora en 12 h (medianoche, mediodía, sin cero adelante).
- De punta a punta (`app/src/androidTest/.../e2e`): la app real en un emulador Android 15 dentro de GitHub Actions (trabajo `e2e`, `./gradlew connectedDebugAndroidTest`). Cada prueba empieza con la app recién instalada (Test Orchestrator con `clearPackageData`). Cubren bienvenida, agregar medicamento, "Ya la tomé" y "Deshacer" desde Hoy, compra e historial. Una versión (etiqueta `v*`) solo se publica si pasan. Siguen siendo manuales: que la alarma suene a la hora, la pantalla bloqueada y el tamaño de letra del teléfono.
- Las librerías de prueba (`androidTestImplementation`) no entran en el APK de la app.
- El APK de prueba de GitHub Actions se firma siempre con la misma llave de depuración (secreto `DEBUG_KEYSTORE_BASE64`), para instalarlo encima del anterior sin perder datos. No es la llave de las versiones publicadas.
- Manuales: `docs/CASOS_DE_PRUEBA.md`.

## 9. Seguridad y privacidad

La app guarda datos de salud. Reglas que toda fase debe cumplir:

**Datos y red**
- Sin permisos de red. El manifiesto los elimina con `tools:node="remove"` por si una librería los agrega, y `scripts/check-no-internet.sh` hace fallar la compilación si el APK pide INTERNET, ACCESS_NETWORK_STATE o ACCESS_WIFI_STATE.
- Los enlaces externos de "Acerca de" (redes y repositorio) se abren con un Intent `ACTION_VIEW`, y el correo de opinión con `ACTION_SENDTO`, siempre solo cuando la persona toca el botón. La app no se conecta a nada y no envía datos por su cuenta: el correo queda como borrador en la app de correo.
- La base de datos y DataStore viven en el almacenamiento privado de la app. No escribir datos en almacenamiento compartido ni en logs (`Log.d` con nombres de medicamentos está prohibido en release).
- Respaldo: solo base de datos y `datastore/`. En Android 12+ la copia en la nube exige cifrado de extremo a extremo (`disableIfNoEncryptionCapabilities`); en Android 9–11 se usa `requireFlags="clientSideEncryption"`.
- Exportar e importar respaldo (v1.1): validar el JSON antes de escribir (tipos, rangos, stock ≥ 0) y hacerlo en una transacción; un archivo inválido no cambia nada.

**Componentes de Android**
- Solo `MainActivity` es `exported="true"`. `PurchaseActivity` y los receptores de acciones de notificación y de alarmas son `exported="false"`.
- El receptor de arranque y cambio de hora es `exported="true"` (lo exige el sistema), pero solo reprograma alarmas: no lee datos del Intent ni cambia el inventario.
- Todo `PendingIntent` usa `FLAG_IMMUTABLE` e Intents explícitos (con la clase de destino).
- Los receptores validan el `eventId` o `medicationId` contra la base de datos antes de actuar; un id desconocido se ignora.

**Notificaciones**
- Por pedido del PRD (F3), las notificaciones de toma son visibles en la pantalla bloqueada, así que muestran el nombre del medicamento. Los avisos de compra siguen la misma decisión (también muestran el nombre). Es una decisión de usabilidad sobre privacidad; si se cambia, usar `VISIBILITY_PRIVATE` con una versión pública sin el nombre del medicamento.

**Compilación y publicación**
- La llave de firma nunca entra al repositorio (`.gitignore`). En CI se decodifica en `$RUNNER_TEMP` y se borra al terminar.
- El workflow tiene permisos de solo lectura; solo el trabajo `release` (etiquetas `v*`) puede escribir.
- Las GitHub Actions se fijan por SHA del commit. Dependabot propone actualizaciones semanales de Actions y de dependencias Gradle.
- El wrapper de Gradle se verifica: `distributionSha256Sum` en `gradle-wrapper.properties` y validación del jar en CI.
- Reporte de vulnerabilidades en privado: ver `SECURITY.md`.
