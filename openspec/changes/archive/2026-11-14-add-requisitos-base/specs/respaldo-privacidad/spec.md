# Spec Delta — Respaldo y privacidad (E10)

## Purpose

Protege los datos del estudiante con copias de seguridad en Google Drive configurables y opcionalmente cifradas, restauración y borrado selectivo de la información.

## ADDED Requirements

### Requirement: HU-35 Configurar copia de seguridad en Google Drive
Como estudiante, quiero respaldar mis datos en mi Google Drive con frecuencia Nunca, Diaria, Semanal, Mensual o Solo manual para no perder mi historial si cambio o pierdo el teléfono. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 8 pts).

#### Scenario: Configurar
- **GIVEN** que el estudiante está en Ajustes > Copia de seguridad
- **WHEN** inicia sesión con su cuenta Google y elige frecuencia Semanal
- **THEN** el sistema programa el respaldo semanal y muestra la fecha y el tamaño del último respaldo (RN-19)

#### Scenario: Manual
- **GIVEN** que hay una cuenta Google vinculada
- **WHEN** el estudiante pulsa «Respaldar ahora»
- **THEN** el sistema sube el respaldo y muestra el resultado

#### Scenario: Sin conexión
- **GIVEN** que no hay internet a la hora programada
- **WHEN** llega el momento del respaldo
- **THEN** el sistema lo reintenta cuando haya conexión y la app sigue funcionando sin cambios

### Requirement: HU-36 Cifrar el respaldo con contraseña opcional
Como estudiante, quiero proteger mi respaldo con una contraseña opcional para que nadie, ni siquiera Google, pueda leer mis datos. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts).

#### Scenario: Con contraseña
- **GIVEN** que el estudiante define una contraseña de respaldo
- **WHEN** se ejecuta un respaldo
- **THEN** el archivo se cifra en el teléfono antes de subirse (RN-20)

#### Scenario: Advertencia
- **GIVEN** que el estudiante va a definir la contraseña
- **WHEN** confirma la contraseña
- **THEN** el sistema advierte que, si la olvida, el respaldo no podrá recuperarse

### Requirement: HU-37 Restaurar copia de seguridad
Como estudiante, quiero restaurar mis datos desde Drive al instalar la app en un teléfono para continuar con mi historial. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Restaurar
- **GIVEN** que existe un respaldo en la cuenta Google
- **WHEN** el estudiante instala la app, inicia sesión y elige «Restaurar»
- **THEN** el sistema recupera materias, sesiones, metas y ajustes

#### Scenario: Contraseña
- **GIVEN** que el respaldo está cifrado
- **WHEN** el estudiante ingresa una contraseña incorrecta
- **THEN** el sistema no restaura y permite reintentar

### Requirement: HU-38 Borrar mi información
Como estudiante, quiero borrar mis datos eligiendo si se borran del teléfono, de Drive o de ambos para tener control total sobre mi información. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Elegir alcance
- **GIVEN** que el estudiante está en Ajustes > Privacidad
- **WHEN** elige «Borrar del teléfono y de Drive» y confirma dos veces
- **THEN** el sistema elimina los datos locales y el respaldo, y vuelve a la pantalla de primer uso (RN-21)

#### Scenario: Cancelar
- **GIVEN** que aparece la segunda confirmación
- **WHEN** el estudiante cancela
- **THEN** no se borra nada
