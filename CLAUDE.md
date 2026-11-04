# CLAUDE.md

Lee y cumple primero **@AGENTS.md**: son las reglas comunes del proyecto. Aquí solo va
lo propio de Claude Code.

## Comandos OpenSpec disponibles
- `/opsx:explore`: explorar una idea sin escribir código.
- `/opsx:propose "<idea>"`: crear un change con proposal, specs, design y tasks.
- `/opsx:apply`: implementar un change aprobado.
- `/opsx:archive`: archivar un change terminado.

## Subagentes del proyecto (`.claude/agents/`)
- `analista-requisitos`: redacta y revisa HU, criterios de aceptación y RNF.
- `revisor-java`: revisa código Java/Android contra MVVM, Room y las specs.
- `auditor-ia`: actualiza `docs/auditoria-ia/bitacora.md` y comprueba la trazabilidad.

## Reglas para Claude
- Responde en español.
- Nunca cambies `applicationId`, `namespace`, el lenguaje (Java) ni minSdk.
- No hagas `git commit` ni `git push` sin pedido explícito.
- Antes de implementar, comprueba que existe un change aprobado en `openspec/changes/`.
- Al terminar una sesión, anota en la bitácora qué pidió el usuario, qué generaste y qué quedó pendiente de revisión humana.
- Documentos académicos: se generan con `python docs/build/generar_docx.py`. No edites el .docx a mano desde la IA.
