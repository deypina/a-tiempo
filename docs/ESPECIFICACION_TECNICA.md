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
`userName`, `snoozeMinutes` (15), `maxRepeats` (3), `urgentReminderHour` (10:00), `fontScale` (NORMAL/LARGE/XLARGE), `onboardingDone`.

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
  si stock > urgent → cancelar aviso diario urgente
  si stock > warn   → restockPending = false
```

### Corregir inventario
`adjustStock(medId, newValue ≥ 0)`: movimiento ADJUSTMENT con delta = new − old; reevaluar umbrales (sin notificar si sube).

### Días que alcanza
`daysLeft = floor(stock / (dosePerIntake × schedules.size))`; si no hay horarios o está pausado → no mostrar.

## 4. Alarmas y notificaciones

### Programación
- `AlarmScheduler.scheduleNext(schedule)`: calcula la próxima ocurrencia (hoy si no ha pasado, si no mañana) y usa `setExactAndAllowWhileIdle(RTC_WAKEUP, …)`.
- `requestCode` estable por schedule (`scheduleId.toInt()`); posponer usa `requestCode` distinto (`100000 + scheduleId`).
- Al dispararse la alarma: crear/obtener `IntakeEvent` del día (estado PENDING), mostrar notificación, programar la del día siguiente.
- Repeticiones: si no hay respuesta, `ReminderRepeat` cada `snoozeMinutes` hasta `maxRepeats`; al agotarse → status MISSED y notificación silenciosa "No confirmó su toma de…".
- Aviso diario de compra urgente: una alarma por medicamento a `urgentReminderHour` mientras `stock <= urgentThreshold` y no haya compra.

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
- "Ya compré" abre una Activity pequeña (diálogo) para escribir la cantidad.
- Tocar la notificación abre la pantalla Hoy.

## 5. Interfaz

Pantallas: Bienvenida (nombre + permisos + batería) · Hoy · Medicamentos (lista) · Editar medicamento · Registrar compra · Corregir cantidad · Historial · Ajustes · Acerca de.

Navegación inferior con 3 pestañas grandes: **Hoy**, **Mis medicamentos**, **Ajustes**.

Diseño: colores sobrios con un verde de acento para "tomada" y ámbar/rojo para avisos, siempre acompañados de ícono y texto (no solo color). Tema claro por defecto, respetando modo oscuro del sistema.

## 6. Respaldo

- `android:allowBackup="true"` con reglas que incluyan la base de datos y DataStore (respaldo automático de Google).
- v1.1: exportar/importar JSON mediante Storage Access Framework (sin permisos de almacenamiento).

## 7. Firma y compilación

- Firma release leída de variables de entorno (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Si no existen, compilar release sin firmar no debe romper el build de debug.
- La llave se crea **una sola vez** y se guarda en un lugar seguro + secretos de GitHub. Perderla obliga a desinstalar (y perder datos) para actualizar.
- `.github/workflows/build-apk.yml` compila en cada push a `main` (artefacto) y publica en Releases al subir una etiqueta `v*`.

## 8. Pruebas

- Unitarias del dominio: confirmar, idempotencia, deshacer, umbrales (incluidos cruces dobles y medias pastillas), compra, ajuste, días restantes, próxima ocurrencia de alarma (cambio de día, medianoche).
- Instrumentadas mínimas: DAO de Room.
- Manuales: `docs/CASOS_DE_PRUEBA.md`.
