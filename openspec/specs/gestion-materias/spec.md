# gestion-materias Specification

## Purpose
Permite registrar, editar, listar y archivar las materias que agrupan las sesiones de estudio.

## Requirements

### Requirement: HU-06 Registrar materia
Como estudiante, quiero registrar una materia con su nombre, un color y, opcionalmente, el docente para organizar mis sesiones de estudio por asignatura. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Registro válido
- **GIVEN** que el estudiante está en el formulario de nueva materia
- **WHEN** ingresa un nombre válido, elige un color y guarda
- **THEN** el sistema crea la materia y la muestra en la lista

#### Scenario: Nombre duplicado
- **GIVEN** que ya existe una materia activa «Matemáticas»
- **WHEN** el estudiante registra «matemáticas»
- **THEN** el sistema rechaza el registro con «Ya existe una materia con ese nombre» (RN-02)

### Requirement: HU-07 Editar materia
Como estudiante, quiero modificar el nombre, el color o el docente de una materia para corregir errores o reflejar cambios del semestre. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 2 pts).

#### Scenario: Edición
- **GIVEN** que existe una materia con sesiones
- **WHEN** el estudiante cambia su nombre y color
- **THEN** las sesiones siguen asociadas a la materia y el calendario muestra el nuevo color

#### Scenario: Estudios varios protegida
- **GIVEN** que existe la materia «Estudios varios», registrada por defecto
- **WHEN** el estudiante abre sus opciones
- **THEN** el sistema no ofrece cambiar su nombre ni asignarle meta semanal (RN-32)

### Requirement: HU-08 Archivar o eliminar materia
Como estudiante, quiero archivar una materia terminada o eliminar una registrada por error para mantener limpia mi lista sin perder mi historial. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Archivar
- **GIVEN** que la materia tiene sesiones
- **WHEN** el estudiante elige «Archivar»
- **THEN** la materia deja de aparecer en las listas de selección y sus sesiones se conservan en reportes y estadísticas

#### Scenario: Eliminar protegida
- **GIVEN** que la materia tiene sesiones
- **WHEN** el estudiante intenta eliminarla
- **THEN** el sistema no lo permite y sugiere archivarla (RN-03)

#### Scenario: Eliminar sin historial
- **GIVEN** que la materia no tiene sesiones
- **WHEN** el estudiante la elimina y confirma
- **THEN** el sistema la elimina definitivamente

#### Scenario: Estudios varios siempre disponible
- **GIVEN** que existe la materia «Estudios varios»
- **WHEN** el estudiante busca archivarla o eliminarla
- **THEN** el sistema no ofrece esas acciones, para que siempre se pueda estudiar sin registrar una materia (RN-32)

### Requirement: HU-09 Listar materias con horas acumuladas
Como estudiante, quiero ver mis materias activas con las horas estudiadas en cada una para detectar a qué asignaturas les dedico poco tiempo. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 2 pts).

#### Scenario: Lista
- **GIVEN** que existen materias activas
- **WHEN** el estudiante abre Materias
- **THEN** el sistema muestra cada materia con su color, nombre y horas realizadas, en orden alfabético, con «Estudios varios» al final

#### Scenario: Progreso de la meta de la materia
- **GIVEN** que Historia tiene una meta de 4 h y esta semana se estudiaron 3 h
- **WHEN** el estudiante abre Materias
- **THEN** la fila de Historia muestra «3 / 4 h» con una barra al 75 % (HU-27)

#### Scenario: Lista vacía
- **GIVEN** que no existen materias registradas por el estudiante
- **WHEN** el estudiante abre Materias
- **THEN** el sistema muestra «Estudios varios» e invita a registrar la primera materia

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
