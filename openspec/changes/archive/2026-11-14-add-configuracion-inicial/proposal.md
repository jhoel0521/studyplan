# Proposal

## Why

La aplicación todavía es la plantilla *Empty Views Activity* de Android Studio: solo existe `MainActivity` con «Hello World!». Antes de construir materias, sesiones y calendario hace falta lo que toda pantalla necesita: saber quién es el estudiante, con qué tema e idioma se muestra, si la app está bloqueada y cómo se borran sus datos. Esta épica de configuración inicial fija además la arquitectura hexagonal en tres capas, con el primer flujo completo de la aplicación como prueba, para que las épicas siguientes se construyan sobre una estructura ya revisada. Los requisitos están especificados en `add-requisitos-base`, cuya validación por el docente sigue pendiente (tarea 2.5 de ese change): por eso `tasks.md` la pide como compuerta antes de escribir código.

## What Changes

- Primer uso: pantalla **Perfil inicial** que guarda nombre y meta semanal predeterminada en el teléfono (HU-01, RN-01, RN-14, RN-15).
- Pantalla de **Inicio** con el saludo y el progreso contra la meta (HU-02). **Parcial**: aún no existen sesiones ni metas por semana, así que se implementan el saludo, el progreso `0 / meta` y los estados vacíos.
- Pantalla de **Ajustes**: tema claro, oscuro o del sistema, e idioma español o inglés, que se conservan al reiniciar (HU-03), y activación del bloqueo con huella o PIN (HU-04).
- Pantalla de **Autenticación**, que se muestra al abrir o volver a primer plano cuando el bloqueo está activo (HU-04).
- Pantalla **Borrar mis datos** (Ajustes > Privacidad) con doble confirmación; al terminar vuelve a Perfil inicial (HU-38, RN-21). **Parcial**: solo se borran los datos del teléfono; las opciones «Drive» y «ambos» llegan con el change de copia de seguridad.
- Primera aplicación de la **arquitectura hexagonal** con los paquetes `domain`, `application.port.in`, `application.port.out`, `application.service`, `adapter.in.ui`, `adapter.out.persistence` y `adapter.out.security` (ver `design.md`). MVVM se conserva dentro de la capa de presentación.
- Primera base de datos Room (versión 1) con el perfil y los ajustes.
- Los layouts XML abren en el editor de diseño de Android Studio y quedan enlazados con su Activity; los eventos (`onCreate`, `onClick` y demás listeners) van en métodos con nombre.

**Fuera de alcance:** copia de seguridad, cifrado y restauración en Drive (HU-35 a HU-37, change propio después), widget (HU-05), materias, sesiones, metas por semana, calendario, estadísticas y reportes. No se agrega inyección de dependencias con librería, Navigation ni Compose.

## Capabilities

### New Capabilities
- Ninguna.

### Modified Capabilities
- Ninguna.

Este change **no define requisitos nuevos**: implementa los ya especificados en `add-requisitos-base` (capacidades `perfil-ajustes`, HU-01 a HU-04, y `respaldo-privacidad`, solo HU-38 parcial). Por eso declara `skip_specs: true` en `.openspec.yaml`: duplicar esos requirements en un segundo change los haría chocar al archivar. La trazabilidad HU → código → prueba se mantiene citando los IDs en los Javadoc y en `tasks.md`. Para leer de un vistazo qué escenarios cubre este change, cuáles quedan pendientes y en qué tareas, ver [`trazabilidad.md`](trazabilidad.md), que enlaza cada historia a su spec.

## Impact

- **Código** (`app/`): todos los paquetes nuevos bajo `io.github.jhoel0521.studyplan`; `MainActivity` pasa a ser la pantalla de Inicio; se agregan `PerfilInicialActivity`, `AutenticacionActivity`, `AjustesActivity` y `BorrarDatosActivity`, sus layouts, los recursos `strings.xml` en español e inglés y una clase `Application` que arma las dependencias a mano.
- **Dependencias nuevas, por aprobar** (no se agregan sin tu sí): Room, Lifecycle (ViewModel y LiveData), `androidx.biometric` y, solo para pruebas, `core-testing`. Sus versiones se verifican al aprobarlas.
- **Permisos:** `USE_BIOMETRIC`. Ninguno más.
- **Datos:** esquema Room v1 con exportación del esquema al repositorio.
- **Reglas y documentos:** `AGENTS.md`, `openspec/config.yaml` y el subagente `revisor-java` ya describen la arquitectura hexagonal. El capítulo 4 del documento (4.1 y 4.3) ya incluye los diagramas de arquitectura y de flujo de pantallas.
- **Nota de coherencia:** la propuesta de `add-requisitos-base` anticipaba los paquetes `ui`, `viewmodel`, `domain` y `data`; esa previsión queda reemplazada por la estructura hexagonal.
