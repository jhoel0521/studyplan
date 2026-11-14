# Tasks

## 1. Línea base de requisitos (Entrega 1)

- [x] 1.1 Levantar requisitos con entrevista al Product Owner y registrar las respuestas en `docs/auditoria-ia/bitacora.md`; verificar que cada decisión tiene su respuesta asociada
- [x] 1.2 Registrar HU-01 a HU-45, RN-01 a RN-30 y RNF-01 a RNF-19 en `docs/build/requisitos.py` y verificar que el script importa sin errores
- [x] 1.3 Generar las specs con `python docs/build/gen_specs.py` y verificar que existen 10 `spec.md`
- [x] 1.4 Ejecutar `openspec validate add-requisitos-base --strict` y verificar que no hay errores
- [x] 1.5 Generar el modelo de dominio, la máquina de estados y los wireframes W1 a W15 con `python docs/build/diagramas.py` y verificar los PNG en `docs/diagramas/`
- [x] 1.6 Generar `docs/entrega1/StudyPlan_Entrega1.docx` con `python docs/build/generar_docx.py` y verificar que no quedan referencias cruzadas sin resolver (el script falla si las hay)

## 2. Revisión humana

- [x] 2.1 En Word: Ctrl+A y F9 (actualizar campos) y verificar que el índice, la lista de tablas y la lista de figuras muestran números de página
- [x] 2.2 El estudiante revisa cada HU y sus criterios y verifica que puede explicar su valor en la defensa (el estudiante declaró el 13-nov-2026 haber revisado HU-01 a HU-04 y HU-38; las demás HU las revisa al abrir el change de su épica)
- [x] 2.3 Confirmar o corregir los supuestos pendientes (RN-12, RN-13, RN-29, valores objetivo de los RNF) y verificar que ninguna regla queda como «Derivada (confirmar)»
- [ ] 2.4 Verificar cada referencia bibliográfica (DOI o URL) y completar la portada, verificando que no queda texto resaltado en amarillo
- [ ] 2.5 El docente valida el alcance de la Entrega 1 y se registra su retroalimentación en la bitácora

## Workflow follow-up

- Archivar el change con `openspec archive add-requisitos-base` cuando el docente apruebe la línea base.
- Crear un change de implementación por épica (por ejemplo, `add-gestion-materias`) para el Sprint 1.
