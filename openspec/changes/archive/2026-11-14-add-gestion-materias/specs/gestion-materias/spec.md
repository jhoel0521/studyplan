## ADDED Requirements

### Requirement: HU-46 Desarchivar materia
Como estudiante, quiero reactivar una materia archivada para volver a usarla cuando retome la asignatura. El sistema SHALL cumplir los escenarios siguientes (prioridad Could, 2 pts; la prioridad y los puntos son una propuesta de la IA que el Product Owner debe confirmar).

#### Scenario: Desarchivar
- **GIVEN** que existe una materia archivada
- **WHEN** el estudiante elige «Desarchivar»
- **THEN** la materia vuelve a la lista de activas y a las listas de selección

#### Scenario: Nombre ya en uso
- **GIVEN** que existe una materia archivada «Física» y una materia activa «física»
- **WHEN** el estudiante intenta desarchivar la archivada
- **THEN** el sistema lo rechaza con «Ya existe una materia con ese nombre» y sugiere renombrarla antes (RN-02)
