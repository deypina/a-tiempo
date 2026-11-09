# Empieza aquí, Dey

Este kit tiene todo lo que Claude Code necesita para construir A Tiempo. No necesitas escribir código.

## Qué hay en la carpeta

| Archivo | Para qué sirve |
|---|---|
| `CLAUDE.md` | Las reglas que Claude Code lee solo al abrir la carpeta: stack, restricciones, cómo hablarte |
| `docs/PRD.md` | El documento de producto: qué hace la app y por qué |
| `docs/ESPECIFICACION_TECNICA.md` | Cómo se construye: datos, alarmas, permisos, Samsung |
| `docs/TEXTOS_UI.md` | Todos los textos de la app, tratando de usted. Edítalos aquí si quieres cambiar alguno |
| `docs/PLAN_DE_CONSTRUCCION.md` | Las 7 fases con el prompt exacto que pegas en cada una |
| `docs/CASOS_DE_PRUEBA.md` | Tu checklist de QA para cada fase |
| `.github/workflows/build-apk.yml` | La automatización que compila el APK en GitHub |
| `README.md` | La portada del repositorio público, con instrucciones para familias |

## Pasos

1. Descomprime la carpeta `a-tiempo` donde guardes tus proyectos.
2. Abre la terminal en esa carpeta y escribe `claude`.
3. Abre `docs/PLAN_DE_CONSTRUCCION.md`, revisa "Antes de empezar" y pega el prompt de la **Fase 0**.
4. Avanza una fase a la vez y prueba en tu A37 con los casos de prueba.

## Lo más importante para no perder datos

En la Fase 6 se crea la **llave de firma**. Guárdala en un lugar seguro (por ejemplo, en tu gestor de contraseñas o en un Drive privado) junto con sus contraseñas. Si se pierde, las actualizaciones no se podrán instalar encima y habría que desinstalar la app, y con eso se borraría el historial de tu suegra.
