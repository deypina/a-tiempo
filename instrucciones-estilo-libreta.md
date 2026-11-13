# Instrucciones para Claude Code — Estilo "libreta" de A Tiempo

Copia y pega todo lo que está debajo de la línea en Claude Code, dentro del proyecto de la app.

Referencia visual (pantallas de muestra): https://claude.ai/artifact/BaWXFFCd9FsytdBafZd2eh

---

Necesito rediseñar la apariencia de A Tiempo. La idea es que se vea como una **libreta de papel rayada**, no como una app genérica: sin tarjetas flotantes, sin cajitas de aviso con ícono, sin la "píldora" morada de la barra de abajo. La usa una señora de 71 años que no se lleva mucho con la tecnología y es muy detallista: todo tiene que ser obvio, ordenado y fácil de tocar.

Antes de escribir código:
1. Lee `CLAUDE.md` y los documentos de `docs/`.
2. Algunas de estas decisiones **cambian** lo que dicen `docs/PRD.md` y `docs/ESPECIFICACION_TECNICA.md` (las marco con ⚠️). Ya las decidí yo, así que primero actualiza esos documentos y `docs/TEXTOS_UI.md`, y después el código.
3. Dime qué archivos vas a tocar y en qué orden, y espera mi visto bueno.

## 1. Decisiones que cambian los documentos ⚠️

- **Hora siempre en formato de 12 horas** ("3:30 p. m.", "7:00 a. m."), sin importar cómo esté configurado el teléfono. Aplica en pantallas, notificaciones e historial. Cambia la línea de idioma del PRD (§ requisitos no funcionales) que hoy dice "según configuración del teléfono".
- **Apariencia elegida por la persona, no automática.** Nueva opción en Ajustes con tres valores: *Claro* (por defecto), *Oscuro* y *Como el teléfono*. La app arranca en claro aunque el teléfono esté en oscuro. Cambia la línea "Tema claro por defecto, respetando el modo oscuro del sistema" de la especificación.
- **Cada medicamento tiene un color** que la persona elige al crearlo o editarlo, para reconocerlo de un vistazo. Es un dato nuevo en la base de datos (ver sección 5).
- **Tipografía incluida en la app** (no descargada): títulos en *Literata* y texto en *Atkinson Hyperlegible* (diseñada para personas con baja visión). Ambas tienen licencia libre SIL OFL 1.1: guarda los archivos `.ttf` en `res/font` y su licencia en el repositorio. No se agrega permiso de INTERNET. Cambia la línea "Títulos con la letra serif del teléfono (no se descarga ninguna fuente)". Si te preocupa el peso del APK, dime cuánto aumenta antes de seguir.
- **Ya no se usan tarjetas** (`Card`) para tomas ni medicamentos: se reemplazan por renglones con líneas finas (sección 3). Actualiza la descripción del diseño "cuaderno cálido" en la especificación.

## 2. Colores (en `ui/theme/Theme.kt`)

Mantén los colores base que ya existen (papel `#F5EFE3`, tinta `#1E1A15`, verde botica `#2E5A44`, ladrillo `#A13A22`, y sus versiones de noche). Agrega:

| Uso | Claro | Oscuro |
|---|---|---|
| Ocre: "quedan pocas" | `#8A5A12` | `#E3B25E` |
| Línea de renglón | `#C9BBA3` | `#3F3933` |
| Línea doble (arriba de cada lista y de la barra inferior) | tinta | tinta de noche |

Colores para medicamentos (la persona elige uno; en modo oscuro se usa la versión clara de cada uno):

| Nombre | Claro | Oscuro |
|---|---|---|
| Azul | `#3D6A8E` | `#7FA6C8` |
| Rosa vieja | `#B0566B` | `#CF8396` |
| Mostaza | `#C99A2E` | `#D9B158` |
| Oliva | `#6E7F3A` | `#A3B26A` |
| Ciruela | `#7A5A8C` | `#A98BBA` |
| Blanco | `#FFFDF8` con borde | `#ECE4D6` |

Reglas:
- Todo texto debe tener contraste de al menos 4,5:1 contra el papel. Revisa el ocre y el ladrillo en ambos modos.
- El color nunca va solo: siempre acompañado de texto ("Se acabó", "Quedan 2", "Tomada").
- Nada de degradados, sombras ni fondos teñidos detrás de los avisos.

## 3. Forma: libreta rayada

- **Sin tarjetas.** Cada toma y cada medicamento es un renglón. Entre renglones, una línea de 1 dp (color "línea de renglón"). Arriba de cada lista, una línea doble de tinta (como el margen de una agenda).
- **Esquinas casi rectas:** 6 dp en botones y campos. Nada de formas de píldora (excepto los círculos de color de cada medicamento).
- **Los botones son lo único que parece botón:** relleno verde para la acción principal, con borde verde para la secundaria, subrayado para acciones pequeñas ("Deshacer", "Recordar en 5 minutos"). Mínimo 56 dp de alto (los subrayados, mínimo 44 dp de zona táctil).
- **Avisos importantes** (se acabó un medicamento, quedan pocas): no van en caja. Son un bloque de texto debajo del saludo: una palabra pequeña en mayúsculas "IMPORTANTE" en ladrillo y debajo el mensaje, con la parte clave en negrita y color. Si hay acción, va como enlace subrayado dentro del texto.
- **Barra inferior:** quita el indicador en forma de píldora de Material (`indicatorColor` transparente). La pestaña activa se marca con ícono y texto en verde y la palabra subrayada; las otras en gris cálido. Fondo del color del papel con línea doble arriba.

## 4. Pantallas

### Hoy
- Saludo: fecha pequeña arriba ("Jueves 12 de noviembre") y "Hola, [nombre]" en Literata.
- Avisos importantes (sección 3), si los hay.
- Lista de tomas como renglones con **la hora en una columna a la izquierda** (unos 78 dp, separada por una línea vertical fina), y a la derecha: círculo de color + nombre del medicamento, cantidad, estado.
- **La próxima toma pendiente (o atrasada) va primero** y más grande: hora en verde con la palabra "AHORA" debajo (o "ATRASADA" en ladrillo si ya pasó su hora), nombre más grande, línea de inventario ("Quedan 2 · le alcanza para 1 día" en ocre si quedan pocas), botón grande "Ya la tomé" y debajo el enlace "Recordar en 5 minutos" (los minutos salen de Ajustes).
- El resto de tomas del día, en orden de hora, más compactas. Las tomadas dicen "✓ Tomada a las 7:10 a. m." en verde, con "Deshacer" como enlace subrayado a la derecha.

### Mis medicamentos
- Botón con borde "Agregar medicamento" arriba.
- Cada medicamento es un renglón: círculo de color + nombre (Literata) + "Ver más ›" a la derecha (toda la fila de arriba es tocable).
- Debajo: "1 pastilla · 3:30 p. m." y la línea de inventario con su color de estado (verde normal, ocre quedan pocas, ladrillo "Se acabó · no le quedan pastillas").
- **Solo un botón visible por medicamento**, y solo cuando hace falta: "Registrar compra" (relleno si se acabó, con borde si quedan pocas). Si hay suficientes, no se muestra ningún botón.
- "Corregir cantidad", "Ver historial" y "Editar" pasan a la pantalla que se abre con "Ver más". Proponme si conviene reutilizar `EditMedicationScreen` o crear una pantalla de detalle sencilla.

### Agregar / editar medicamento
- Nueva sección "Color para reconocerla": seis círculos grandes (mínimo 48 dp) en una fila. El elegido lleva borde de tinta y un ✓ encima; para TalkBack: "Azul, elegido".
- Texto de ayuda debajo: "Elija el color que más se parezca a la pastilla o a su caja."
- Al crear, viene preseleccionado el primer color que no use ningún otro medicamento.

### Ajustes
- Nueva sección **"Apariencia"** al inicio (después del nombre): tres botones grandes con ícono arriba y texto abajo: *Claro* (sol), *Oscuro* (luna), *Como el teléfono* (teléfono).
- En todos los grupos de opciones (apariencia, minutos para recordar, tamaño de letra), la opción elegida se marca con borde verde de 2 dp, fondo verde muy suave y texto en negrita. Nunca solo con un tono un poco más oscuro.
- "Aviso diario de compra", "Revisar permisos y batería" y "Acerca de A Tiempo" como renglones de lista (no botones con borde), con la hora del aviso a la derecha en verde.

### Notificaciones y aviso de toma
- Mismos textos de siempre, con la hora en formato de 12 horas.

## 5. Datos y lógica

- Campo nuevo `colorKey` (texto, por ejemplo `"azul"`) en `MedicationEntity`. Sube la versión de la base de datos y escribe una **migración** que conserve todos los datos actuales y asigne colores distintos a los medicamentos que ya existen, en orden. Nada de `fallbackToDestructiveMigration`: la persona no puede perder su inventario.
- Preferencia nueva `appearance` (`LIGHT` por defecto, `DARK`, `SYSTEM`) en el mismo lugar donde se guardan los demás ajustes. `ATiempoTheme` la recibe igual que hoy recibe `fontScale`.
- La lógica para elegir el primer color libre va en la capa de dominio, con prueba unitaria.
- Una sola función para formatear horas en 12 h (en `ui/common/Formatting.kt`), usada en todos lados, con prueba unitaria: medianoche = "12:00 a. m.", mediodía = "12:00 p. m.", "1:25 p. m." sin cero adelante.

## 6. Textos nuevos (agrégalos primero a `docs/TEXTOS_UI.md` y luego a `strings.xml`)

- Ajustes: "Apariencia", "Claro", "Oscuro", "Como el teléfono".
- Medicamento: "Color para reconocerla", "Elija el color que más se parezca a la pastilla o a su caja.", nombres de los colores (para TalkBack) y "elegido".
- Hoy: "AHORA", "ATRASADA", "IMPORTANTE", "Recordar en %d minutos".
- Mis medicamentos: "Ver más", "Se acabó · no le quedan pastillas".
- Revisa que los textos de inventario existentes sigan el formato "Quedan 2 · le alcanza para 1 día".

Si alguno ya existe con otra redacción, usa el existente y avísame.

## 7. Pruebas (agrégalas a `docs/CASOS_DE_PRUEBA.md`)

- La apariencia elegida se mantiene al cerrar y abrir la app; "Claro" se ve claro aunque el teléfono esté en oscuro.
- Al actualizar desde la versión anterior, los medicamentos, horarios, historial e inventario siguen iguales y cada medicamento tiene un color.
- Las horas salen en 12 h aunque el teléfono esté en 24 h.
- Con letra "Muy grande" + letra grande del sistema, la columna de la hora en Hoy no se corta (si no cabe, que crezca; nunca recortar la hora).
- TalkBack lee el color elegido y la opción seleccionada en Ajustes.
- Contraste de 4,5:1 en todos los textos, en claro y oscuro.

## 8. Al terminar

- Compila (`./gradlew assembleDebug`), corre pruebas unitarias y lint.
- Haz commits pequeños en español (por ejemplo: `feat: apariencia clara, oscura o del teléfono`, `feat: color por medicamento`, `feat: estilo libreta en Hoy`).
- Explícame en lenguaje sencillo qué cambió y dame una lista numerada de lo que tengo que probar en el teléfono, incluyendo cómo probar que la actualización no borra los datos.
