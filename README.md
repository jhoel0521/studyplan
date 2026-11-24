# StudyPlan

Aplicación Android nativa (Java) para gestionar y planificar el tiempo de estudio: materias,
sesiones con máquina de estados, calendario de estudio (día, semana y mes), Pomodoro, eventos de
estudio (tiempo extra, estudio con la app cerrada), metas semanales, estadísticas, reportes y
respaldo en Google Drive. Funciona offline-first.

| | |
|---|---|
| Tipo | Proyecto académico de la materia Aplicaciones Móviles (datos de autor y docente en `docs/documento/.env`, no versionado) |
| Package | `io.github.jhoel0521.studyplan` · minSdk 24 · versión objetivo 0.1.0 |
| Metodología | Scrum + Spec-Driven Development con [OpenSpec](https://github.com/Fission-AI/OpenSpec) |

## Estado

| Entrega | Contenido | Estado |
|---|---|---|
| 1 | Cap. 1 a 3.2.2 (introducción, marco teórico, metodología y requisitos) + Auditoría IA | En revisión |
| Final | Capítulos 1 a 5 (el 5 documenta la publicación en Google Play), Auditoría IA, referencias y anexos; 140 páginas, 43 tablas y 39 figuras | Documento generado; falta la revisión humana del texto |

Requisitos de la línea base: 46 historias de usuario, 30 reglas de negocio y 19 requisitos no
funcionales (ISO/IEC 25010:2023), especificados en
[`openspec/specs`](openspec/specs) (la línea base archivada está en [`openspec/changes/archive`](openspec/changes/archive)).

## Objetivos logrados

| Objetivo | Estado | Evidencia |
|---|---|---|
| Guardar la copia de seguridad en Google Drive | **Cumplido** | Épica 10 (HU-35 a HU-38): copia en la carpeta oculta de la app en Drive, cifrado AES-256-GCM opcional con contraseña, restauración y borrado por alcance. El Product Owner la probó en un teléfono real: borró sus datos y los restauró desde Drive |
| Publicar o dejarlo pendiente y documentado | **Cumplido como «pendiente y documentado»** | App creada en Google Play Console, versión 1000 (0.1.0) firmada por el CI en prueba interna, 11 declaraciones de contenido, ficha con ícono, gráfico y 5 capturas, y video público. El capítulo 5 del documento lo registra |
| Finalizar el documento | **Cumplido** | Capítulos 1 a 5 con auditoría de IA, 0 marcas pendientes y diagramas cotejados con el código (327 clases) |

Otros resultados verificables: 456 pruebas unitarias sin fallos, flujo de CI con release firmado por etiqueta
(`vX.Y.Z`), widget de progreso semanal, reportes en PDF y CSV, y una bitácora de auditoría de IA con los errores
de la IA registrados (filas 165 a 171).

### Publicación en Google Play

- Estado: la versión 0.1.0 está en **prueba interna**; la app **no está disponible para el público**.
- Video de demostración: <https://youtu.be/v-vtEj7br2A>
- Política de privacidad: <https://jhoel0521.github.io/studyplan-privacy/>
- Pendiente (ver el capítulo 5 y `docs/publicacion/guia-google-play-y-drive.md`): el botón «Enviar la app para su
  revisión» sigue desactivado por una causa no determinada, la prueba cerrada que Play pide antes de producción y los
  clientes OAuth de Drive para la clave de subida y para Play App Signing, sin los cuales la versión instalada desde
  Play no podría vincular la cuenta de Google.

## Estructura

```
app/                 código Android (Java)
openspec/            specs vigentes (specs/) y cambios en curso (changes/)
docs/documento/      documento de la entrega en Markdown (capitulos/, recursos/, documento.toml) y su salida en dist/
docs/publicacion/     guía de Google Play y Drive y borrador de la ficha de la tienda
docs/presentacion/    generar_presentacion.py: genera la presentación de la defensa (el .pptx no se versiona)
docs/build/          diagramas.py: modelo de dominio, máquina de estados, ciclo Scrum+SDD, wireframes W1–W15
.apa/                motor de apa-kit (genera el Word desde los .md)
docs/auditoria-ia/   bitácora del uso de IA
AGENTS.md            reglas para agentes de IA (CLAUDE.md solo remite a él) · .claude/ .agents/ skills y subagentes
```

## Regenerar la documentación

El documento se escribe en Markdown y se genera con [apa-kit](https://github.com/jhoel0521/apa-kit)
(skills `apa-*` o los comandos de abajo; requiere Python 3.9+).

```bash
python docs/build/diagramas.py                                  # diagramas y wireframes (Pillow)
cp docs/documento/.env.example docs/documento/.env              # completar portada (autor, materia, docente...)
python .apa/scripts/generar_docx.py --doc docs/documento        # genera docs/documento/dist/StudyPlan_Entrega1.docx
openspec validate --specs --strict                              # valida las specs
```

Los documentos finales (`*.docx`, `*.pdf`), `dist/`, `original/` y `docs/documento/.env` **no se versionan**.
Sin `.env`, la portada sale con marcadores genéricos en amarillo. En Word, pulsa Ctrl+A y luego F9 para
actualizar el índice y las listas de tablas y figuras. Si editas un Word a mano, guárdalo con otro nombre
fuera de `dist/` o `generar_docx.py` se negará a sobrescribirlo.

## Compilación, CI y publicación

Compuerta de calidad local (también la que corre el CI):

```bash
./gradlew assembleDebug assembleDebugAndroidTest test lint   # con ANDROID_HOME definido
```

| Workflow | Cuándo corre | Qué hace |
|---|---|---|
| `.github/workflows/ci.yml` | push a `main` y cada pull request | La compuerta anterior; sube los informes si falla. No usa secretos |
| `.github/workflows/release.yml` | al empujar una etiqueta `vX.Y.Z` | Pruebas y lint, AAB y APK firmados, verificación con `apksigner` y un **borrador** de GitHub Release (no público) |

La versión sale de la etiqueta: `vX.Y.Z` da `versionName = X.Y.Z` y `versionCode = X·1.000.000 + Y·1.000 + Z`
(por ejemplo `v1.2.3` da `1002003`). Sin propiedades, el build local usa `1` y `1.0`.

### Crear la clave de subida (una sola vez, en tu máquina)

La clave **nunca** entra al repositorio (`.gitignore` bloquea `*.jks` y `*.keystore`). No la crea la IA.

```bash
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
```

1. Anota las contraseñas en tu gestor de contraseñas y guarda **una copia de `upload.jks` fuera del repositorio**.
2. El SHA-1 de la clave es público y se pide en Google Cloud y en Play Console:
   `keytool -list -v -keystore upload.jks -alias upload` (línea `SHA1`).
3. Carga cuatro **secretos** en GitHub (Settings → Secrets and variables → Actions):

| Secreto | Valor |
|---|---|
| `KEYSTORE_BASE64` | el keystore en base64: `base64 -w0 upload.jks` |
| `KEYSTORE_PASSWORD` | contraseña del keystore |
| `KEY_ALIAS` | el alias (`upload`) |
| `KEY_PASSWORD` | contraseña de la clave |

### Publicar una versión

```bash
git tag v0.1.0 && git push origin v0.1.0     # la etiqueta debe apuntar a una confirmación de main
```

Cuando termina el workflow, abre el borrador en *Releases*, comprueba el AAB y el APK, y publícalo si todo está bien.
Para Google Play, el AAB se sube **a mano** la primera vez; ver `docs/publicacion/guia-google-play-y-drive.md`.

## Auditoría de IA

Por indicación del docente, **todo uso de IA se documenta**. La *Auditoría IA* es un punto especial
que se agrega al índice de **cada entrega**, con estructura fija (A.1 a A.6): declaración de uso,
protocolo humano–IA, sistema OpenSpec, bitácora de decisiones, riesgos y mitigación, y reflexión.

| Herramienta | Uso |
|---|---|
| Claude Code (Anthropic), modelo Claude Opus 5.5 | Entrevista de levantamiento de requisitos, redacción de specs y documentación, scripts de generación |
| OpenSpec 1.14.3 (Fission-AI) | Estructura y validación de especificaciones (SDD) |

- Bitácora acumulativa (cada sesión agrega filas, nunca reescribe las anteriores):
  [docs/auditoria-ia/bitacora.md](docs/auditoria-ia/bitacora.md). Cada fila registra solicitud,
  resultado de la IA, decisión humana y verificación.
- Los errores de la IA (requisitos inventados, supuestos no declarados) se registran siempre: son
  evidencia de control humano.
- Toda referencia, DOI, versión o métrica propuesta por la IA se verifica a mano antes de entregar.
- Regla del proyecto: **la IA no inventa requisitos; si falta información, pregunta.** Ver [AGENTS.md](AGENTS.md).
- Los commits son de autoría del estudiante; la IA se declara aquí y en la bitácora, no como coautora.
