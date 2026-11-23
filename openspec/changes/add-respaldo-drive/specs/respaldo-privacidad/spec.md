# respaldo-privacidad Delta Specification

## MODIFIED Requirements

### Requirement: HU-35 Configurar copia de seguridad en Google Drive
Como estudiante, quiero respaldar mis datos en mi Google Drive con frecuencia Nunca, Diaria, Semanal, Mensual o Solo manual para no perder mi historial si cambio o pierdo el teléfono. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 8 pts).

#### Scenario: Configurar
- **GIVEN** que el estudiante está en Ajustes > Copia de seguridad
- **WHEN** inicia sesión/autoriza su cuenta Google con scope `drive.appdata` y elige frecuencia Semanal
- **THEN** el sistema guarda el correo vinculado, programa la tarea periódica en `WorkManager` con restricción de red y muestra la fecha y el tamaño del último respaldo (`RN-19`)

#### Scenario: Manual
- **GIVEN** que hay una cuenta Google vinculada
- **WHEN** el estudiante pulsa «Respaldar ahora»
- **THEN** el sistema serializa la instantánea completa de Room, la cifra si hay contraseña, la sube a `appDataFolder` en Google Drive y actualiza fecha y tamaño del último respaldo

#### Scenario: Sin conexión
- **GIVEN** que no hay internet a la hora programada
- **WHEN** llega el momento del respaldo periódico
- **THEN** `WorkManager` reintenta cuando haya conexión (`Result.retry()` con restricción `CONNECTED`) y la app sigue funcionando sin cambios offline-first

#### Scenario: Apagado por defecto
- **GIVEN** que el estudiante instala la app y no ha tocado Copia de seguridad
- **WHEN** la app se ejecuta
- **THEN** la frecuencia es «Nunca», no hay cuenta vinculada y no se programa ni se envía nada a Google (`RN-19`)

#### Scenario: Respaldo fallido visible
- **GIVEN** que la autorización de Google caducó o fue revocada, o Drive devolvió un error
- **WHEN** falla un respaldo programado o manual
- **THEN** el sistema conserva los datos locales sin cambios, no reintenta en bucle una autorización que exige al usuario, y muestra en Ajustes que el último respaldo falló con la acción «Volver a autorizar»

#### Scenario: Desvincular
- **GIVEN** que hay una cuenta Google vinculada
- **WHEN** el estudiante pulsa «Desvincular»
- **THEN** el sistema cancela la tarea periódica, olvida el correo y la contraseña guardada, deja la frecuencia en «Nunca» y no borra el respaldo ya subido

### Requirement: HU-36 Cifrar el respaldo con contraseña opcional
Como estudiante, quiero proteger mi respaldo con una contraseña opcional para que nadie, ni siquiera Google, pueda leer mis datos. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts).

#### Scenario: Con contraseña
- **GIVEN** que el estudiante define una contraseña de respaldo
- **WHEN** se ejecuta un respaldo
- **THEN** el archivo se cifra en el teléfono con `PBKDF2WithHmacSHA256` y `AES-256-GCM` antes de subirse (`RN-20`), sin guardar la contraseña en texto plano en `SharedPreferences` ni en Room

#### Scenario: Advertencia
- **GIVEN** que el estudiante va a definir la contraseña
- **WHEN** confirma la contraseña
- **THEN** el sistema muestra la advertencia explícita de que, si la olvida, el respaldo no podrá recuperarse

### Requirement: HU-37 Restaurar copia de seguridad
Como estudiante, quiero restaurar mis datos desde Drive al instalar la app en un teléfono para continuar con mi historial. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Restaurar
- **GIVEN** que existe un respaldo en `appDataFolder` de la cuenta Google
- **WHEN** el estudiante inicia en un teléfono nuevo o desde Ajustes, autoriza su cuenta y elige «Restaurar»
- **THEN** el sistema descarga el respaldo, reemplaza de forma transaccional materias, sesiones, eventos, tramos, metas, perfil y ajustes, y navega a Inicio

#### Scenario: Contraseña
- **GIVEN** que el respaldo descargado está cifrado
- **WHEN** el estudiante ingresa una contraseña incorrecta
- **THEN** la verificación de etiqueta GCM falla sin corromper los datos locales y permite reintentar

#### Scenario: Sin respaldo
- **GIVEN** que la cuenta autorizada no tiene respaldo en `appDataFolder`
- **WHEN** el estudiante elige «Restaurar»
- **THEN** el sistema informa que no hay respaldo, no modifica los datos locales y permite continuar sin restaurar

#### Scenario: Respaldo de una versión más nueva
- **GIVEN** que el respaldo tiene una versión de formato mayor que la que entiende la app
- **WHEN** el estudiante intenta restaurar
- **THEN** el sistema no restaura, no modifica los datos locales e indica que debe actualizar StudyPlan

#### Scenario: Restauración consistente
- **GIVEN** que se restaura un respaldo válido
- **WHEN** termina la transacción
- **THEN** los ajustes ligados al dispositivo (bloqueo con huella o PIN y No molestar) quedan en su valor por defecto, se vuelven a programar los recordatorios y avisos, y no queda ninguna sesión «En curso» heredada del otro teléfono

### Requirement: HU-38 Borrar mi información
Como estudiante, quiero borrar mis datos eligiendo si se borran del teléfono, de Drive o de ambos para tener control total sobre mi información. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Elegir alcance
- **GIVEN** que el estudiante está en Ajustes > Privacidad
- **WHEN** elige «Borrar de Drive» o «Borrar del teléfono y de Drive» y confirma dos veces
- **THEN** el sistema elimina el archivo de respaldo en `appDataFolder` (y/o los datos locales según alcance) y, si borró el teléfono, vuelve a la pantalla de primer uso (`RN-21`)

#### Scenario: Cancelar
- **GIVEN** que aparece la segunda confirmación
- **WHEN** el estudiante cancela
- **THEN** no se borra nada ni en local ni en Drive
