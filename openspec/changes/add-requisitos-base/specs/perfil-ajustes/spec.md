# Spec Delta — Perfil, inicio y ajustes (E1)

## Purpose

Permite al estudiante identificarse localmente, ver su resumen diario y configurar la aplicación: tema, idioma, bloqueo de acceso y widget.

## ADDED Requirements

### Requirement: HU-01 Configurar perfil inicial
Como estudiante, quiero registrar mi nombre y mi meta semanal predeterminada la primera vez que abro la app para recibir una experiencia personalizada sin crear una cuenta. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 2 pts).

#### Scenario: Primer uso
- **GIVEN** que la aplicación se abre por primera vez
- **WHEN** el estudiante ingresa su nombre y una meta semanal válida y confirma
- **THEN** el sistema guarda el perfil en el teléfono y muestra la pantalla de inicio con el saludo «¡Hola, <nombre>!»

#### Scenario: Nombre vacío
- **GIVEN** que el formulario de perfil está abierto
- **WHEN** el estudiante intenta confirmar sin nombre
- **THEN** el sistema muestra «Ingresa tu nombre» y no guarda el perfil

### Requirement: HU-02 Ver resumen en la pantalla de inicio
Como estudiante, quiero ver mi avance de la meta semanal, mi próxima sesión y las sesiones de hoy para saber en segundos qué me toca estudiar y cómo voy. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Resumen con datos
- **GIVEN** que existen una meta y sesiones planificadas
- **WHEN** el estudiante abre la pantalla de inicio
- **THEN** el sistema muestra las horas realizadas sobre la meta con una barra de progreso, la próxima sesión Pendiente y las sesiones de hoy con su estado

#### Scenario: Acceso rápido
- **GIVEN** que el estudiante está en la pantalla de inicio
- **WHEN** pulsa «Nueva sesión»
- **THEN** el sistema abre el registro de sesión con la fecha de hoy preseleccionada

### Requirement: HU-03 Configurar tema e idioma
Como estudiante, quiero elegir tema claro, oscuro o del sistema e idioma español o inglés para usar la app con comodidad visual y en mi idioma. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Tema
- **GIVEN** que el estudiante está en Ajustes
- **WHEN** elige el tema oscuro
- **THEN** toda la aplicación se muestra con el tema oscuro y la elección se conserva al reiniciar

#### Scenario: Idioma
- **GIVEN** que el estudiante está en Ajustes
- **WHEN** elige inglés
- **THEN** todos los textos de la aplicación se muestran en inglés

### Requirement: HU-04 Bloquear la app con huella o PIN
Como estudiante, quiero proteger el acceso a la aplicación con mi huella o el PIN del teléfono para que nadie más vea mis datos de estudio. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Activar bloqueo
- **GIVEN** que el teléfono tiene huella o PIN configurado
- **WHEN** el estudiante activa el bloqueo en Ajustes
- **THEN** cada vez que la app vuelve a primer plano solicita autenticarse antes de mostrar datos

#### Scenario: Sin método de bloqueo
- **GIVEN** que el teléfono no tiene huella ni PIN
- **WHEN** el estudiante intenta activar el bloqueo
- **THEN** el sistema explica que debe configurar un bloqueo de pantalla y no activa la opción

### Requirement: HU-05 Ver progreso en un widget
Como estudiante, quiero agregar un widget en la pantalla de inicio del teléfono con mi progreso semanal y la próxima sesión para ver mi avance sin abrir la aplicación. El sistema SHALL cumplir los escenarios siguientes (prioridad Could, 5 pts).

#### Scenario: Widget
- **GIVEN** que el estudiante agregó el widget de StudyPlan
- **WHEN** completa una sesión
- **THEN** el widget actualiza las horas realizadas sobre la meta

#### Scenario: Abrir desde el widget
- **GIVEN** que el widget está visible
- **WHEN** el estudiante lo toca
- **THEN** se abre la pantalla de inicio de la aplicación
