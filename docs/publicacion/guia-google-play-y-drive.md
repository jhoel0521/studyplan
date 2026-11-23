# Registro: publicar StudyPlan en Google Play y activar el respaldo en Google Drive

Este documento es un **registro de pasos** para el Product Owner. Describe cómo crear la app en Google (Play Console y
Google Cloud) para publicarla y para que el respaldo en Drive (HU-35 a HU-38) funcione.

> **Aviso de verificación (AGENTS.md §5).** Las pantallas, plazos, costos y reglas de Google cambian. Todo lo marcado con
> **[VERIFICAR]** lo propuso la IA de memoria y **debe comprobarse a mano** en la documentación oficial antes de
> depender de ello. La IA no puede crear cuentas, claves ni secretos; los crea y los carga el Product Owner.
>
> **Nunca** se versionan claves, contraseñas, tokens ni archivos `.json` de cuentas de servicio. Aquí solo se anotan
> nombres y valores públicos (por ejemplo, el nombre de paquete).

## 0. Datos fijos del proyecto

| Dato | Valor |
|---|---|
| Nombre de paquete (`applicationId`) | `io.github.jhoel0521.studyplan` (permanente: no se puede cambiar tras publicar) |
| Nombre de la app | StudyPlan |
| minSdk / targetSdk | 24 / 37 |
| Repositorio | `github.com/jhoel0521/studyplan` |
| Alcance de Drive que usa la app | `drive.appdata` (carpeta oculta de la app) |

## 1. Ruta de pasos (orden con dependencias)

```
 PISTA A: código (IA, con changes aprobados)         PISTA B: consolas (Product Owner, con guía)
 ------------------------------------------         -------------------------------------------
 A1 add-ci-release-firmado (CI + firma)  ---------> B1 Crear la clave de subida + 4 secretos
                                                      |
                                                      v
                                                    B2 Play Console: crear la app, declaraciones,
                                                       primera subida manual del AAB (pista interna)
                                                      |  (aquí aparece el SHA-1 de Play App Signing)
                                                      v
 A2 Épica respaldo (HU-35..38) <-- usa SHA-1 de -- B3 Google Cloud: proyecto, Drive API, consentimiento,
    debug mientras se desarrolla                       3 clientes OAuth Android (debug, subida, Play)
                      \                              /
                       v                            v
                      B4 Pruebas cerradas -> producción en Play
```

Por qué este orden y no «épica, Google, publicar»:
- Google Cloud necesita los **SHA-1** de las firmas, y el de **Play App Signing** solo existe después de crear la app en
  Play Console y subir una primera versión. Por eso B2 va antes del último cliente de B3.
- La épica (A2) se puede programar a la vez que B2 y B3, probando con el SHA-1 de debug; la parte pendiente de Google
  se completa cuando B3 termine.
- La **política de privacidad ya está publicada** (ver apartado 3 y el registro del apartado 8), así que no bloquea B2.

## 2. Clave de subida y firma (Product Owner)

1. Crear la clave de subida con `keytool` en tu propia máquina (instrucciones en el `README.md`, sección «Compilación, CI
   y publicación», que añade el change `add-ci-release-firmado`).
2. Guardar una **copia del keystore y de sus contraseñas fuera del repositorio** (gestor de contraseñas). Si se pierde,
   Play permite restablecer la clave de subida, pero con trámite. **[VERIFICAR]**
3. Obtener el SHA-1 del keystore (es un dato público, no un secreto):
   `keytool -list -v -keystore <archivo.jks> -alias <alias>` y copiar la línea `SHA1`.
4. Usar **Play App Signing**: Google guarda la clave de firma final y tú subes con la clave de subida. **[VERIFICAR]**

## 3. Crear la app en Google Play Console (Product Owner)

Requisitos y pasos **[VERIFICAR todo en Play Console]**:

1. Cuenta de desarrollador de Play (pago único, verificación de identidad). Para **cuentas personales nuevas**, Google ha
   exigido una prueba cerrada con un mínimo de testers durante varias semanas antes de producción: comprobar el requisito
   vigente en la ayuda de Play Console.
2. «Crear app»: nombre, idioma predeterminado, tipo (App), gratuita o de pago, y aceptar las declaraciones.
3. **Política de privacidad pública (URL)**: obligatoria, y más aún si la app accede a Drive. **Ya está publicada en
   <https://jhoel0521.github.io/studyplan-privacy/>** (bilingüe, detecta el idioma). Hay que revisarla cuando se implemente
   el respaldo en Drive (quitar el aviso «en desarrollo»).
4. Formulario **Seguridad de los datos (Data safety)**: declarar los datos que se manejan y que el respaldo es opcional,
   va solo al Drive del usuario y se puede cifrar con contraseña.
5. Clasificación de contenido, público objetivo, declaración de anuncios (no hay), acceso a la app (sin inicio de sesión
   obligatorio; el de Google es solo para el respaldo) y ficha de la tienda: descripción, ícono 512 × 512, gráfico de
   función y capturas de pantalla (ver `docs/documento`, proceso de capturas censuradas).
6. Primera versión: subir el **AAB firmado a mano** en una pista (interna o cerrada). La automatización con cuenta de
   servicio solo funciona después de esta primera subida. **[VERIFICAR]**
7. Anotar, en *Integridad de la app → Firma de apps*, el **SHA-1 del certificado de firma de la app** (el que usa Google
   para firmar lo que se distribuye): se necesita en el paso 4.

## 4. Google Cloud para el respaldo en Drive (Product Owner, con ayuda guiada)

Con la misma cuenta de Google **[VERIFICAR cada paso en la consola]**:

1. Crear un proyecto nuevo (por ejemplo, «StudyPlan»).
2. **Habilitar «Google Drive API»** para ese proyecto.
3. **Pantalla de consentimiento OAuth:** tipo *Externo*; nombre de la app, correo de soporte, logo, enlace a la política
   de privacidad y dominio autorizado si aplica.
4. **Alcances:** solo `https://www.googleapis.com/auth/drive.appdata`. No pedir `drive` ni `drive.file`.
5. **Estado de publicación:** en *Pruebas* solo entran usuarios de prueba declarados (hasta 100) y los permisos pueden
   caducar a los 7 días; para usuarios reales hay que pasar a *En producción*, y Google puede pedir **verificación de la
   marca/app**. Comprobar si `drive.appdata` la exige.
6. Crear **clientes OAuth de tipo Android**, uno por cada firma con la que se distribuya, cada uno con el nombre de
   paquete `io.github.jhoel0521.studyplan` y su SHA-1:
   - la **clave de subida** (compilaciones de release del CI, firmadas por ti);
   - el **certificado de firma de Play App Signing** (la app instalada desde Play);
   - la **clave de debug** (desarrollo local), solo mientras se prueba.
7. Anotar el ID de cliente. Es un dato público (no un secreto) y se podrá guardar en la configuración de la app; las
   claves y los secretos jamás.

## 5. Qué decide el código (change futuro, requiere aprobación)

- Inicio de sesión y autorización de Google con las APIs actuales de Android (Credential Manager y la API de
  autorización de Google Identity Services; el antiguo «Google Sign-In» está obsoleto). **[VERIFICAR]**
- Subir, listar, bajar y borrar en la carpeta `appDataFolder` de Drive con la API REST.
- Cifrado local con AES-256-GCM y clave PBKDF2 antes de subir (RN-20, RNF-12); sin dependencias para el cifrado.
- Programación de la frecuencia (RN-19): WorkManager o AlarmManager. WorkManager no está en el catálogo y requiere
  aprobación humana (AGENTS.md §2).
- Desactivar o acotar `android:allowBackup` para que el respaldo automático de Android no contradiga «nadie, ni Google,
  puede leer mis datos» (RNF-12).

## 6. Lista de comprobación

| # | Tarea | Quién |
|---|---|---|
| 1 | Crear la clave de subida y guardar la copia | Product Owner |
| 2 | Cargar los 4 secretos en GitHub | Product Owner |
| 3 | Crear la app en Play Console y rellenar las declaraciones | Product Owner |
| 4 | Redactar y publicar la política de privacidad | **Hecho** (2026-11-22) |
| 5 | Primera subida manual del AAB a una pista de pruebas | Product Owner |
| 6 | Crear el proyecto de Google Cloud, habilitar Drive y el consentimiento | Product Owner (con guía) |
| 7 | Crear los clientes OAuth Android con los SHA-1 | Product Owner |
| 8 | Implementar el respaldo | IA, con un change aprobado |
| 9 | Pruebas cerradas y paso a producción | Product Owner |

## 7. Qué puede hacer la IA con el navegador

Navegar las consolas contigo y rellenar formularios **cuando lo confirmes paso a paso**. No escribe contraseñas ni
códigos de verificación, no acepta términos, no crea cuentas, claves ni proyectos sin tu confirmación explícita, y no
publica nada en Play sin tu aprobación.

## 8. Registro de lo hecho

| Fecha | Paso | Resultado | Quién |
|---|---|---|---|
| 2026-11-22 | Redacción de esta guía (sin cuentas ni claves creadas) | Pendiente de verificación manual | IA propuso, Product Owner revisa |
| 2026-11-22 | Política de privacidad publicada con GitHub Pages (`github.com/jhoel0521/studyplan-privacy`) | URL: https://jhoel0521.github.io/studyplan-privacy/ (200, bilingüe) | IA creó el repositorio con `gh` con la confirmación del Product Owner |
| 2026-11-23 | Borrador de la ficha de Play: textos es/en, respuestas de Data safety y clasificación, permisos a justificar (`docs/publicacion/ficha-play-store.md`); revisión de la cuenta de Play Console (cuenta personal, sin la app StudyPlan; acción pendiente «declarar condición de comerciante» antes de feb. 2027) | Borrador, todo `[VERIFICAR]` en la consola; ícono 512 y gráfico 1024×500 ya existen, faltan las capturas | IA propuso (solo lectura en la consola), Product Owner decide |
| 2026-11-23 | App «StudyPlan» creada en Play Console (paquete `io.github.jhoel0521.studyplan`, aplicación, gratuita; declaraciones de políticas y de exportación de EE. UU. aceptadas por orden del Product Owner). Capturas censuradas en `docs/documento/recursos/capturas/publicacion/` (`play-01` a `play-03`, no versionadas). El panel indica que antes de producción hace falta una prueba cerrada | Hecho | IA operó el formulario con confirmación del Product Owner |
| 2026-11-23 | Firma y release: los cuatro secretos (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) los creó el Product Owner el 2026-11-22 en su otra computadora (Linux) con su clave de subida; la IA solo vio nombres y fechas con `gh secret list`, nunca valores. El primer `v0.1.0` (commit anterior a la épica de respaldo) se borró por orden del Product Owner, junto con su borrador. Se volvió a crear `v0.1.0` sobre `7477f7e` (épica completa): el workflow Release terminó con éxito, incluido `apksigner verify`, y dejó un borrador con `app-release.aab` (4,96 MB) y `app-release.apk` (4,49 MB) firmados | Hecho | Product Owner (clave y secretos); la IA empujó `main` y la etiqueta con su orden |
| 2026-11-23 | Play Console: versión 0.1.0 (código 1000) publicada en prueba interna, sin verificadores; clave de subida registrada. Declaraciones guardadas: política de privacidad y anuncios (no contiene). Ficha en español guardada como borrador con descripciones, ícono 512, gráfico 1024×500 y 5 capturas de teléfono (Inicio, Calendario, Estadísticas, Materias, Copia de seguridad) tomadas del teléfono del Product Owner, con el nombre y el correo tapados y recortadas a 2:1. Capturas de los pasos en `docs/documento/recursos/capturas/publicacion/` (no versionadas). Pendiente: 9 declaraciones restantes, traducción al inglés, cliente OAuth de Drive con la huella de Play App Signing y actualizar la política de privacidad | Hecho en parte | IA operó la consola con confirmación del Product Owner |
| 2026-11-23 | Play Console: las 11 declaraciones de Contenido de la app quedaron guardadas («Ya estás al día»): privacidad, anuncios, detalles de acceso (No), clasificación IARC (Todas las edades), público objetivo (solo mayores de 18), seguridad de los datos (no recopila ni comparte; criterio de la ayuda de Play sobre subir al Drive del propio usuario), ID de publicidad, apps gubernamentales, funciones financieras, apps de salud y servicio en primer plano. Para este último se grabó en el emulador un video de 6 min 36 s con datos de ejemplo y se publicó en YouTube como **público** (https://youtu.be/v-vtEj7br2A), por decisión del Product Owner; el canal pasó a llamarse «Jhoel dev». Quedan: categoría Productividad, ficha en inglés, cliente OAuth de Drive con la huella de Play App Signing, actualizar la política de privacidad y enviar a revisión | Hecho en parte | IA operó la consola y el emulador con confirmación del Product Owner |
