# Casos de prueba — A Tiempo

Marca cada caso con ✅ o ❌ y anota qué pasó si falla. Prueba primero en el A37 y la versión final en el A16.

## Medicamentos e inventario
| ID | Paso | Resultado esperado | ✅/❌ |
|---|---|---|---|
| C-01 | Agregar "Gabapentina", 1 por toma, 9:00 p. m., 8 pastillas | Aparece en la lista con "Le quedan 8 · le alcanza para 8 días" | |
| C-02 | Agregar "Eutirox", ½ por toma, 7:00 a. m., 10 pastillas | Muestra "½ pastilla" (no "0.5") y "le alcanza para 20 días" | |
| C-03 | Medicamento con 2 horarios y 1 por toma, 10 pastillas | "le alcanza para 5 días" | |
| C-04 | Intentar guardar sin nombre o sin hora | Muestra el mensaje de error correspondiente | |
| C-05 | Poner aviso urgente mayor que el normal | No deja guardar, muestra error | |
| C-06 | Pausar un medicamento | No suena su recordatorio; sigue en la lista como pausado | |

## Recordatorios
| ID | Paso | Resultado esperado | ✅/❌ |
|---|---|---|---|
| C-07 | Programar toma 2 min en el futuro, teléfono bloqueado | Llega con sonido, nombre de la usuaria y dos botones | |
| C-08 | Tocar "Ya la tomé" en la notificación | Desaparece, inventario baja en la cantidad correcta, Hoy dice "Tomada a las…" | |
| C-09 | Tocar "Ya la tomé" dos veces rápido (o desde la app tras la notificación) | Solo descuenta una vez | |
| C-10 | Tocar "Recordar en 15 min" | Vuelve a sonar a los 15 min; inventario no cambia | |
| C-11 | No tocar nada | Se repite 3 veces; luego queda "No confirmada" sin descontar | |
| C-12 | Reiniciar el teléfono antes de la hora | El recordatorio suena igual | |
| C-13 | Dejar el teléfono sin usar 1 hora con pantalla apagada | El recordatorio llega a la hora exacta | |
| C-14 | Cambiar la hora del medicamento | Suena a la nueva hora, no a la anterior | |

## Avisos de compra
| ID | Paso | Resultado esperado | ✅/❌ |
|---|---|---|---|
| C-15 | Stock 6, umbral 5, confirmar toma | Aviso "Le quedan 5…" con botón "Ya compré" | |
| C-16 | Seguir confirmando hasta 3 | Aviso urgente | |
| C-17 | Día siguiente a la hora configurada, sin compra | Se repite el aviso urgente | |
| C-18 | Llegar a 0 | Aviso "Se le acabó…"; el recordatorio de toma advierte que no quedan | |
| C-19 | "Ya compré" → 10 | Inventario +10, mensaje de confirmación, avisos de compra se detienen | |
| C-20 | Corregir cantidad a 4 | Inventario queda en 4; historial muestra la corrección | |
| C-21 | Stock 6, dosis 4 (cruza 5 y 3 a la vez) | Solo llega el aviso urgente, no los dos | |

## Hoy, historial, ajustes, accesibilidad
| ID | Paso | Resultado esperado | ✅/❌ |
|---|---|---|---|
| C-22 | Marcar "Ya la tomé" desde Hoy | Igual que desde la notificación | |
| C-23 | Deshacer una toma | Inventario vuelve; estado Pendiente; queda en historial | |
| C-24 | Ver historial | Tomas, compras y correcciones en orden, más recientes primero | |
| C-25 | Cambiar el nombre en Ajustes | Las siguientes notificaciones usan el nombre nuevo | |
| C-26 | Cambiar minutos de posponer a 5 | "Recordar en 5 min" funciona | |
| C-27 | Letra "Muy grande" y letra grande del sistema | Todo se lee, nada se corta ni se superpone | |
| C-28 | Modo avión todo el día | La app funciona igual (no usa internet) | |

## Instalación y actualización
| ID | Paso | Resultado esperado | ✅/❌ |
|---|---|---|---|
| C-29 | Instalar v1.0 desde Releases sobre una v1.0 anterior firmada | Se actualiza sin perder datos | |
| C-30 | Revisar tamaño en Ajustes → Aplicaciones | Menos de 15 MB | |
