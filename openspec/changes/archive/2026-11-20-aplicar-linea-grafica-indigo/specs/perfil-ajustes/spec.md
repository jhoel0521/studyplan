## MODIFIED Requirements

### Requirement: HU-02 Ver resumen en la pantalla de inicio
Como estudiante, quiero ver mi avance de la meta semanal, mi próxima sesión y las sesiones de hoy para saber en segundos qué me toca estudiar y cómo voy. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Resumen con datos
- **GIVEN** que existen una meta y sesiones planificadas
- **WHEN** el estudiante abre la pantalla de inicio
- **THEN** el sistema muestra las horas realizadas sobre la meta con una barra de progreso, la próxima sesión Pendiente y las sesiones de hoy con su estado

#### Scenario: Horas de la semana con corte estricto
- **GIVEN** que el domingo anterior el estudiante estudió de 23:00 a 01:00 del lunes y quedó Completada
- **WHEN** abre la pantalla de inicio el lunes
- **THEN** las horas realizadas de la semana actual incluyen solo la hora estudiada desde las 00:00 del lunes (RN-31)

#### Scenario: Acceso rápido
- **GIVEN** que el estudiante está en la pantalla de inicio
- **WHEN** pulsa «Nueva sesión»
- **THEN** el sistema abre el registro de sesión con la fecha de hoy preseleccionada

#### Scenario: Sesión en curso visible
- **GIVEN** que hay una sesión En curso
- **WHEN** el estudiante abre la pantalla de inicio
- **THEN** el sistema muestra un aviso con la materia de esa sesión y la acción «Volver a la sesión», que abre la sesión En curso

#### Scenario: Nombre largo
- **GIVEN** que el nombre del perfil es más largo de lo que cabe en dos líneas
- **WHEN** el estudiante abre la pantalla de inicio
- **THEN** el saludo ocupa como máximo dos líneas y termina en «…» sin desplazar el resto del inicio, y el lector de pantalla anuncia el nombre completo

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

#### Scenario: Línea gráfica en ambos temas
- **GIVEN** que el estudiante usa el tema claro o el oscuro
- **WHEN** recorre las pantallas principales (Inicio, Estudiar ahora, sesión en curso, estadísticas y materias)
- **THEN** la interfaz usa la paleta índigo y verde azulado del tema, y todo texto alcanza un contraste de al menos 4,5:1 contra su fondo en ambos temas

#### Scenario: Estado sin depender del color
- **GIVEN** que una sesión está en enfoque, en pausa o en descanso
- **WHEN** el estudiante ve la pantalla de la sesión en curso
- **THEN** el estado se distingue por su color, su icono y su etiqueta de texto, no solo por el color

#### Scenario: Color de materia solo como acento
- **GIVEN** que una materia tiene un color elegido por el estudiante
- **WHEN** la materia aparece en una tarjeta, una lista o un bloque del calendario
- **THEN** su color se muestra solo como barra, punto o chip tenue, y la tarjeta, su borde y los botones conservan los colores del tema

#### Scenario: Color de materia legible
- **GIVEN** que una materia tiene cualquier color elegido por el estudiante
- **WHEN** su barra o su punto se muestran sobre una superficie clara u oscura
- **THEN** el color alcanza un contraste de al menos 3:1 contra esa superficie, y si el color elegido no lo alcanza se muestra una versión ajustada del mismo tono

#### Scenario: Botones uniformes
- **GIVEN** cualquier pantalla con botones de acción
- **WHEN** se muestran en la misma pantalla
- **THEN** todos tienen la misma altura y el mismo radio, y la importancia de cada acción se indica solo por su estilo (relleno, tonal, contorno o texto)

#### Scenario: Texto ampliado
- **GIVEN** que el estudiante usa el tamaño de fuente más grande del sistema
- **WHEN** ve una pantalla con cronómetro, segmentos o botones de acción
- **THEN** el cronómetro y las etiquetas se ven completas sin recortes ni solapamientos, todos los botones conservan la misma altura, y una etiqueta que no cabe reduce su tamaño de letra o, en las acciones repetitivas, se muestra como icono con su nombre accesible
