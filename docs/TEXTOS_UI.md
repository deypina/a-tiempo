# Textos de la interfaz — A Tiempo

Reglas de estilo:
- Trato de **usted**, cálido y breve. Frases cortas.
- Usar `{nombre}` (nombre o apodo elegido). Si está vacío, omitir el saludo sin dejar coma suelta.
- Evitar adjetivos con género para que sirva a cualquier persona ("¿Cómo le gustaría que le llamemos?").
- Cantidades: "1 pastilla", "2 pastillas", "½ pastilla", "1½ pastillas". Con 1 o ½ se usa singular también en el verbo: "Le queda 1 pastilla", "Le queda ½ pastilla" (en Android, con recursos `plurals`).
- No poner artículos delante de `{medicamento}` ("su {medicamento}", "de {medicamento}"), porque cada nombre tiene su propio género.
- Nada de lenguaje médico ni consejos de salud.

## General
| Clave | Texto |
|---|---|
| app_name | A Tiempo |
| tab_today | Hoy |
| tab_meds | Mis medicamentos |
| tab_settings | Ajustes |
| common_cancel | Cancelar |
| common_delete | Eliminar |

## Bienvenida
| Clave | Texto |
|---|---|
| welcome_title | Le damos la bienvenida a A Tiempo |
| welcome_body | Le recordaremos sus pastillas a la hora que usted elija y le avisaremos antes de que se le acaben. |
| welcome_name_question | ¿Cómo le gustaría que le llamemos? |
| welcome_name_hint | Su nombre o apodo |
| welcome_continue | Continuar |
| perm_notif_title | Permitir avisos |
| perm_notif_body | Para recordarle sus pastillas necesitamos permiso para mostrar avisos. |
| perm_alarm_title | Avisos a la hora exacta |
| perm_alarm_body | Así el recordatorio llega justo a la hora que usted eligió. |
| perm_battery_title | Que el teléfono no apague los avisos |
| perm_battery_body | Algunos teléfonos Samsung apagan las apps para ahorrar batería y los avisos no llegan. Toque el botón y elija "Sin restricciones". |
| perm_allow | Permitir |
| perm_later | Ahora no |
| disclaimer | A Tiempo solo le ayuda a organizarse. No reemplaza las indicaciones de su médico. |

## Hoy
| Clave | Texto |
|---|---|
| today_greeting | Hola, {nombre} |
| today_greeting_no_name | Hola |
| today_subtitle | Sus tomas de hoy |
| today_empty | Todavía no tiene medicamentos. Toque "Agregar medicamento" para empezar. |
| status_pending | Pendiente |
| status_taken | Tomada a las {hora} |
| status_snoozed | Le recordaremos a las {hora} |
| status_missed | No confirmada |
| btn_taken | Ya la tomé |
| btn_undo | Deshacer |
| stock_line | Le quedan {n} pastillas · le alcanza para {d} días |
| stock_line_one_day | Le quedan {n} pastillas · le alcanza para 1 día |
| stock_line_less_than_day | Le quedan {n} pastillas · no le alcanza para un día completo |
| stock_empty_line | Ya no le quedan pastillas |

## Notificaciones de toma
| Clave | Texto |
|---|---|
| notif_intake_title | {nombre}, es hora de su {medicamento} |
| notif_intake_body | Tome {cantidad}. {nota} |
| notif_intake_body_nostock | Tome {cantidad}. Según la cuenta, ya no le quedan pastillas. |
| notif_action_taken | ✅ Ya la tomé |
| notif_action_snooze | ⏰ Recordar en {x} min |
| notif_repeat_title | {nombre}, recuerde su {medicamento} |
| notif_missed | No confirmó su toma de {medicamento} de las {hora}. |
| notif_taken_toast | Listo. Le quedan {n} pastillas de {medicamento}. |

## Avisos de compra
| Clave | Texto |
|---|---|
| notif_low_title | Le quedan {n} pastillas de {medicamento} |
| notif_low_body | {nombre}, recuerde comprar pronto. Cuando compre, avísenos. |
| notif_urgent_title | Solo le quedan {n} de {medicamento} |
| notif_urgent_body | {nombre}, compre hoy para no quedarse sin ellas. |
| notif_out_title | Se le acabaron las pastillas de {medicamento} |
| notif_out_body | {nombre}, ya no le quedan pastillas. Compre hoy. |
| notif_action_purchased | Ya compré |

## Medicamentos
| Clave | Texto |
|---|---|
| meds_title | Mis medicamentos |
| meds_add | Agregar medicamento |
| med_name | Nombre del medicamento |
| med_name_hint | Ej.: Gabapentina |
| med_dose | ¿Cuánto toma cada vez? |
| med_dose_other | Otra cantidad |
| med_times | ¿A qué hora lo toma? |
| med_add_time | Agregar otra hora |
| med_stock | ¿Cuántas pastillas tiene ahora? |
| med_warn | Avisarme cuando me queden |
| med_urgent | Avisarme con urgencia cuando me queden |
| med_note | Nota (opcional) |
| med_note_hint | Ej.: con comida |
| med_save | Guardar |
| med_pause | Pausar recordatorios |
| med_resume | Reanudar recordatorios |
| med_paused | Recordatorios en pausa |
| med_delete | Eliminar medicamento |
| med_delete_confirm | ¿Quiere eliminar {medicamento}? También se borrará su historial. |
| med_error_name | Escriba el nombre del medicamento. |
| med_error_time | Agregue al menos una hora. |
| med_error_thresholds | El aviso urgente debe ser menor que el aviso normal. |

## Compra y corrección
| Clave | Texto |
|---|---|
| purchase_title | ¿Cuántas pastillas de {medicamento} compró? |
| purchase_other | Otra cantidad |
| purchase_save | Guardar compra |
| purchase_error | Escriba una cantidad mayor que 0. |
| purchase_done | Listo. Ahora tiene {n} pastillas de {medicamento}. |
| adjust_title | ¿Cuántas pastillas de {medicamento} tiene realmente? |
| adjust_save | Corregir |
| adjust_btn | Corregir cantidad |

## Historial
| Clave | Texto |
|---|---|
| history_title | Historial de {medicamento} |
| history_taken | Tomó {cantidad} |
| history_missed | No confirmada |
| history_purchase | Compró {n} |
| history_adjust | Corrigió la cantidad a {n} |
| history_undo | Deshizo una toma |
| history_empty | Aún no hay movimientos. |

## Ajustes
| Clave | Texto |
|---|---|
| settings_title | Ajustes |
| settings_name | Su nombre o apodo |
| settings_snooze | Cuando toque "Recordar", volver a avisar en |
| settings_minutes | {x} minutos |
| settings_urgent_hour | Hora del aviso diario de compra urgente |
| settings_font | Tamaño de letra |
| font_normal | Normal |
| font_large | Grande |
| font_xlarge | Muy grande |
| settings_permissions | Revisar permisos y batería |
| settings_about | Acerca de A Tiempo |
| about_version | Versión {v} |
| about_repo | Ver el código en GitHub |
| about_body | A Tiempo es una app gratuita y de código abierto. Sus datos se guardan solo en este teléfono. |
| warn_exact_denied | Los avisos podrían llegar con unos minutos de retraso. Toque aquí para corregirlo. |
| warn_battery | Su teléfono podría retrasar los avisos. Toque aquí para corregirlo. |

## Canales de notificación
Estos nombres aparecen en Ajustes del teléfono → Notificaciones de A Tiempo.

| Clave | Texto |
|---|---|
| channel_intake_name | Recordatorios de toma |
| channel_intake_desc | Le avisa a la hora de cada toma. |
| channel_purchase_name | Avisos de compra |
| channel_purchase_desc | Le avisa cuando le quedan pocas pastillas. |
| channel_info_name | Tomas no confirmadas |
| channel_info_desc | Le cuenta qué tomas no se confirmaron. |
