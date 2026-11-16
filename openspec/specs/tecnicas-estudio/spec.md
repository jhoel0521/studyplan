# tecnicas-estudio Specification

## Purpose
Acompaña la ejecución de una sesión con cronómetro, Pomodoro y No molestar, y registra los eventos de estudio (salidas, regresos, tiempo extra, estudio con la app cerrada) para medir la concentración y el respeto del tiempo.

## Requirements

### Requirement: HU-22 Cronometrar sesión en curso
Como estudiante, quiero iniciar, pausar y detener la sesión y ver el tiempo transcurrido mientras estoy en la app para registrar el tiempo real estudiado. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Cronometrar
- **GIVEN** que existe una sesión Pendiente de hoy y no hay otra sesión En curso
- **WHEN** el estudiante pulsa Iniciar y, tras 45 minutos efectivos, Detener
- **THEN** la sesión pasa por En curso a Completada con 45 minutos reales

#### Scenario: Detención en el primer minuto (0 minutos)
- **GIVEN** que la sesión está En curso con menos de 60 segundos acumulados
- **WHEN** el estudiante pulsa Detener (o declara «No, terminé al salir»)
- **THEN** la sesión pasa a Completada con 0 minutos reales sin error de validación

#### Scenario: Sesión única en curso
- **GIVEN** que ya existe una sesión A en estado En curso
- **WHEN** el estudiante intenta iniciar una sesión B en estado Pendiente
- **THEN** el sistema bloquea el inicio simultáneo y presenta el diálogo de recuperación sobre la sesión A para cerrarla o continuar con ella

#### Scenario: App cerrada por completo
- **GIVEN** que la sesión está En curso
- **WHEN** el estudiante cierra la app desde recientes y la abre 20 minutos después
- **THEN** la sesión sigue En curso y el tiempo se calcula desde las marcas de tiempo guardadas (RN-22), tomando la última marca persistida conocida si el proceso fue terminado antes de grabar salida

#### Scenario: Duración distinta a la planificada
- **GIVEN** que la sesión se planificó para 60 minutos
- **WHEN** el estudiante la detiene a los 39 minutos (o a las 3 h 28 min)
- **THEN** se registra la duración real y la diferencia se muestra en el reporte (RN-26)

### Requirement: HU-23 Estudiar con técnica Pomodoro configurable
Como estudiante, quiero ejecutar mi sesión en bloques Pomodoro con tiempos configurables para mantener la concentración con descansos programados. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Ciclo
- **GIVEN** que la configuración es 25/5/15 cada 4
- **WHEN** el estudiante inicia la sesión en modo Pomodoro
- **THEN** el sistema deriva las fases de forma determinista desde las marcas de tiempo activas y alterna trabajo y descansos con aviso sonoro o vibración en cada cambio

#### Scenario: Solo trabajo suma
- **GIVEN** que se completaron 4 pomodoros
- **WHEN** el estudiante detiene la sesión
- **THEN** se registran 100 minutos reales, sin contar descansos (RN-11)

#### Scenario: Configurar
- **GIVEN** que el estudiante está en Ajustes > Pomodoro
- **WHEN** cambia el trabajo a 50 minutos mientras otra sesión ya está En curso
- **THEN** la sesión ya iniciada conserva su snapshot original de parámetros y solo las siguientes sesiones usan 50 minutos (RN-17)

#### Scenario: Seguir tras el plan
- **GIVEN** que se completaron los pomodoros que caben en el tiempo planificado
- **WHEN** el estudiante quiere seguir
- **THEN** el sistema ofrece «+1 pomodoro» o «tiempo extra libre» y lo registra como tiempo extra (RN-25)

### Requirement: HU-24 Activar modo No molestar durante la sesión
Como estudiante, quiero que el teléfono silencie notificaciones mientras estudio para evitar distracciones. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Global
- **GIVEN** que No molestar está activado en Ajustes y la app tiene el permiso
- **WHEN** el estudiante inicia una sesión con preferencia «Heredar ajuste global»
- **THEN** el sistema guarda el filtro previo, activa No molestar y lo restaura al pausar, detener, cambiar de sesión o cerrar por vencimiento (RN-18)

#### Scenario: Forzar por sesión
- **GIVEN** que No molestar está desactivado globalmente
- **WHEN** el estudiante selecciona «Activar en esta sesión» antes de iniciar
- **THEN** solo esa sesión activa No molestar sin alterar la configuración global

#### Scenario: Forzar desactivación por sesión
- **GIVEN** que No molestar está activado globalmente
- **WHEN** el estudiante selecciona «Desactivar en esta sesión» antes de iniciar
- **THEN** esa sesión inicia sin activar No molestar y sin modificar la configuración global

#### Scenario: Sin permiso
- **GIVEN** que la app no tiene acceso a No molestar
- **WHEN** el estudiante activa la opción
- **THEN** el sistema explica el permiso y abre la configuración del sistema para concederlo sin bloquear el resto de funciones (RNF-18)

#### Scenario: Permiso revocado durante la sesión
- **GIVEN** que el usuario revocó el permiso de No molestar mientras la sesión estaba En curso
- **WHEN** se pausa, detiene o cierra por vencimiento la sesión
- **THEN** el sistema comprueba el permiso vigente y omite la restauración sin lanzar excepción ni abortar el cierre (RNF-18)

### Requirement: HU-39 Registrar eventos de estudio
Como estudiante, quiero que la app registre los eventos de mi sesión (inicio, pausas, salidas y regresos, tiempo extra, cambios, fin) para saber qué tan concentrado estuve y cuánto respeté mi tiempo de estudio. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Línea de tiempo
- **GIVEN** que una sesión terminó
- **WHEN** el estudiante abre su detalle
- **THEN** el sistema muestra la línea de tiempo de eventos con su hora y tipo

#### Scenario: Persistencia
- **GIVEN** que se registra un evento
- **WHEN** el sistema operativo termina la app justo después
- **THEN** el evento permanece guardado (RN-22)

### Requirement: HU-40 Recibir aviso al salir de la app durante una sesión
Como estudiante, quiero recibir una notificación para volver cuando salgo de la app con una sesión en curso para no perder el hilo ni olvidar la sesión abierta. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Salida
- **GIVEN** que hay una sesión En curso
- **WHEN** el estudiante sale de la app para atender una llamada
- **THEN** se registra el evento «salida», la sesión NO se pausa y aparece la notificación «Tu sesión sigue en curso — volver» (RN-23)

#### Scenario: Regreso
- **GIVEN** que el estudiante salió de la app
- **WHEN** toca la notificación o abre la app
- **THEN** se registra el evento «regreso»

### Requirement: HU-41 Declarar estudio con la app cerrada
Como estudiante, quiero indicar al volver si seguí estudiando después de salir de la app y hasta qué hora para registrar mi estudio real aunque haya olvidado el celular o estudiado en grupo. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Volver más tarde
- **GIVEN** que salió de la app a las 8:45 con la sesión En curso y vuelve a las 17:00
- **WHEN** el estudiante supera el bloqueo biométrico/PIN si está activo y responde «Sí, hasta las 11:00» con la nota «en grupo» en el diálogo «Saliste a las 8:45, ¿seguiste estudiando?»
- **THEN** se registra el tramo 8:45–11:00 como «estudio con la app cerrada» sumándolo íntegro incluso si la sesión inició en Pomodoro, y la sesión pasa a Completada (RN-24)

#### Scenario: No siguió
- **GIVEN** que aparece la pregunta al volver
- **WHEN** el estudiante responde «No, terminé al salir»
- **THEN** la sesión se completa con la hora de salida como fin (admitiendo 0 minutos si ocurrió en el primer minuto)

#### Scenario: Sigue estudiando
- **GIVEN** que aparece la pregunta al volver
- **WHEN** el estudiante responde «Sigo estudiando»
- **THEN** el tramo fuera de la app se registra como estudio con la app cerrada y la sesión continúa En curso

#### Scenario: Al día siguiente
- **GIVEN** que la sesión quedó En curso desde ayer (dentro de 3 días)
- **WHEN** el estudiante abre la app
- **THEN** se hace la misma pregunta y la sesión no se marcó No realizada (RN-29)

#### Scenario: Vencimiento tras más de 3 días
- **GIVEN** que la sesión quedó En curso por más de 3 días sin regreso ni declaración
- **WHEN** el estudiante abre la app o se ejecuta la verificación de vencimiento
- **THEN** el sistema cierra automáticamente la sesión como Completada en la última marca registrada dentro de la app y restaura No molestar (RN-29)

### Requirement: HU-42 Continuar con tiempo extra al llegar al fin planificado
Como estudiante, quiero que al llegar la hora de fin la app me pregunte si sigo estudiando para aprovechar mi inspiración sin perder el registro. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Pregunta
- **GIVEN** que la sesión planificada termina a las 11:00 y sigue En curso
- **WHEN** llegan las 11:00
- **THEN** aparece la notificación «¿Sigues estudiando?» con [Sigo] y [Terminé] sin duplicar en pantalla el aviso de extensión Pomodoro si las fases ya habían concluido (RN-25)

#### Scenario: Sin respuesta
- **GIVEN** que el estudiante no responde (o denegó notificaciones y regresa luego a la app)
- **WHEN** pasa el tiempo
- **THEN** la sesión sigue En curso y el tiempo posterior se marca como tiempo extra

### Requirement: HU-43 Decidir al llegar la hora de otra sesión
Como estudiante, quiero que, si sigo estudiando cuando empieza mi siguiente sesión, la app me avise y me deje seguir o cambiar para pasar de una materia a otra sin cortar mi concentración. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Cambiar
- **GIVEN** que Matemáticas está En curso y a las 12:00 empieza Inglés
- **WHEN** el estudiante elige «Cambiar a Inglés»
- **THEN** Matemáticas pasa a Completada a las 12:00 e Inglés pasa a En curso sin detener el tiempo (RN-27)

#### Scenario: Seguir
- **GIVEN** que aparece el aviso
- **WHEN** el estudiante elige «Seguir con Matemáticas»
- **THEN** Inglés queda Pendiente y sigue su ciclo normal

### Requirement: HU-44 Repartir un tramo continuo entre sesiones
Como estudiante, quiero repartir al detener el tiempo estudiado entre la sesión actual y las siguientes del día para reflejar que terminé una materia y seguí con otra sin tocar el celular. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts).

#### Scenario: Repartir
- **GIVEN** que Matemáticas (10:00–11:00) estuvo En curso hasta las 12:10 e Inglés estaba planificada de 11:00 a 12:00
- **WHEN** el estudiante detiene y elige «Repartir» fijando la hora de corte a las 11:05
- **THEN** el sistema segmenta los intervalos efectivos trabajados (sin convertir pausas ni descansos Pomodoro en tiempo real) y completa ambas sesiones en una sola transacción atómica (RN-28)

#### Scenario: Sin sesiones siguientes
- **GIVEN** que no hay sesiones Pendientes consecutivas
- **WHEN** el estudiante detiene
- **THEN** no se ofrece repartir
