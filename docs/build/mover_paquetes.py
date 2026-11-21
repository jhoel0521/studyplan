"""Mueve las clases a su carpeta destino y reescribe las referencias (RNF-15).

Herramienta TEMPORAL del change ``reorganizar-paquetes-avisos-documentacion``; se borra al cerrar la etapa 3.

Uso (desde la raíz del repositorio):
    python docs/build/mover_paquetes.py --simular                  informa sin tocar nada
    python docs/build/mover_paquetes.py --aplicar [--capas a,b]    mueve y reescribe (git mv)

``--capas`` limita el movimiento a una o varias capas de mapa_paquetes.CAPAS (por ejemplo ``domain``). Las
referencias de las demás capas se reescriben igual, porque apuntan a clases que cambian de paquete.

Qué hace al aplicar:
  * ``git mv`` de cada .java de main, test y androidTest a su carpeta nueva y cambio de ``package``;
  * reescribe ``paquete.Clase`` (imports, referencias completas, {@link}, cadenas) y el nombre relativo con punto
    de manifiesto y layouts (``.adapter.in.ui.Clase``);
  * AÑADE los import de clases que antes compartían paquete con el archivo y ahora están en otra carpeta;
  * pasa a ``public`` las clases sin ``public`` que, tras mover, tienen un cliente en otra carpeta (y sus miembros).
"""
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import mapa_paquetes as mapa  # noqa: E402

RAIZ = mapa.RAIZ
UMBRAL_CARPETA = 15

# Métodos y campos con visibilidad de paquete de clases públicas que se usan desde otra carpeta; se llena a partir
# de los errores de compilación del ensayo.
MIEMBROS_PUBLICOS = {
    "EstadisticasAdapter": ["nombreVisible"],
    "StudyPlanApplication": ["sesionRepository", "materiaRepository"],
    "ServicioSesionEnCurso": ["mostrar", "detener", "estaActivo"],
    "StudyPlanDatabase": ["asegurarMateriaGeneral"],
}
# Miembros protected que se usan desde otra carpeta y pasan a public.
MIEMBROS_PROTEGIDOS = {"ActividadBase": ["app"]}

# Pruebas cuyo nombre no permite deducir la clase que prueban: nombre -> (capa, subcarpeta)
PRUEBAS_MANUALES = {
    # unitarias de servicios
    "ArchivarDesarchivarMateriaServiceTest": ("application/service", "materias"),
    "EditarConsultarMateriaServiceTest": ("application/service", "materias"),
    "EjecucionSesionServiceTest": ("application/service", "tecnicas"),
    "FelicitacionMetasServiceTest": ("application/service", "metas"),
    "MetasServiceTest": ("application/service", "metas"),
    "RecuperacionSalidaServiceTest": ("application/service", "tecnicas"),
    "RepartoTramoServiceTest": ("application/service", "tecnicas"),
    # unitarias de dominio
    "AjustesPomodoroTest": ("domain", "entity"),
    "AjustesRecordatorioTest": ("domain", "entity"),
    "SesionEstudioEnCursoTest": ("domain", "entity"),
    "SesionEstudioSinPlanTest": ("domain", "entity"),
    "CalculadorReporteTest": ("domain", "service"),
    "FechaHoraTest": ("domain", "valueobject"),
    # unitarias de presentación
    "ColorMateriaContrasteTest": ("adapter/in/ui", "comun"),
    "ContrasteTemaTest": ("adapter/in/ui", "comun"),
    "ReglasEstiloLayoutTest": ("adapter/in/ui", "comun"),
    # instrumentadas de presentación
    "AccionesNotificacionEspressoTest": ("adapter/in/ui", "tecnicas"),
    "BloqueoEspressoTest": ("adapter/in/ui", "configuracion"),
    "BloqueoPantallaEspressoTest": ("adapter/in/ui", "tecnicas"),
    "CandadoEspressoTest": ("adapter/in/ui", "tecnicas"),
    "LineaGraficaEspressoTest": ("adapter/in/ui", "comun"),
    "MateriaFormularioRotacionEspressoTest": ("adapter/in/ui", "materias"),
    "MetasEspressoTest": ("adapter/in/ui", "metas"),
    "NavegacionEspressoTest": ("adapter/in/ui", "comun"),
    "NotificacionSesionEspressoTest": ("adapter/in/ui", "tecnicas"),
    "NotificadorRecordatoriosInstrumentalTest": ("adapter/in/ui", "tecnicas"),
    "RecordatoriosEspressoTest": ("adapter/in/ui", "tecnicas"),
    "SesionEnCursoControlesEspressoTest": ("adapter/in/ui", "tecnicas"),
    "SesionesEspressoTest": ("adapter/in/ui", "sesiones"),
    "TecnicasEstudioEspressoTest": ("adapter/in/ui", "tecnicas"),
    "Pruebas": ("adapter/in/ui", "comun"),
    # lo que prueba clases que no se mueven se queda en la raíz de su paquete
    "ReceptorAvisosSesionRecordatorioInstrumentalTest": ("adapter/in/ui", ""),
    "StudyPlanApplicationTest": ("adapter/in/ui", ""),
    "MateriaPersistenciaInstrumentadaTest": ("adapter/out/persistence", ""),
    "Migracion3InstrumentalTest": ("adapter/out/persistence", ""),
    "Migracion4InstrumentalTest": ("adapter/out/persistence", ""),
    "Migracion5InstrumentalTest": ("adapter/out/persistence", ""),
    "Migracion6InstrumentalTest": ("adapter/out/persistence", ""),
    "Migracion7InstrumentalTest": ("adapter/out/persistence", ""),
    "Migracion8InstrumentalTest": ("adapter/out/persistence", ""),
    "MigracionBaseInstrumentadaTest": ("adapter/out/persistence", ""),
    "PersistenciaInstrumentadaTest": ("adapter/out/persistence", ""),
}

SUFIJOS_PRUEBA = ("EspressoTest", "InstrumentadoTest", "InstrumentalTest", "Test")
CANDIDATOS = ("", "Activity", "ViewModel", "Service", "Adapter", "Dialogo", "Repository", "RepositoryRoom", "Dao",
              "Entity", "Sistema", "Receptor")


class Clase:
    def __init__(self, ruta, origen):
        self.ruta = ruta
        self.origen = origen  # main | test | androidTest
        self.nombre = ruta.stem
        self.texto = ruta.read_text(encoding="utf-8")
        m = re.search(r"^package\s+([\w.]+);", self.texto, re.M)
        self.paquete = m.group(1)
        self.sub = self.paquete[len(mapa.PAQUETE_RAIZ) + 1:] if self.paquete != mapa.PAQUETE_RAIZ else ""
        self.publica = bool(re.search(
            r"^public\s+(?:final\s+|abstract\s+)*(?:class|interface|enum|record)\s+" + self.nombre + r"\b",
            self.texto, re.M))
        self.nuevo_sub = self.sub

    @property
    def nuevo_paquete(self):
        return mapa.PAQUETE_RAIZ + ("." + self.nuevo_sub if self.nuevo_sub else "")

    @property
    def nueva_ruta(self):
        base = {"main": mapa.BASE_MAIN, "test": mapa.BASE_TEST, "androidTest": mapa.BASE_ANDROID_TEST}[self.origen]
        return base.joinpath(*self.nuevo_sub.split(".")) / (self.nombre + ".java") if self.nuevo_sub \
            else base / (self.nombre + ".java")


def recolectar():
    clases = []
    for origen, base in (("main", mapa.BASE_MAIN), ("test", mapa.BASE_TEST), ("androidTest", mapa.BASE_ANDROID_TEST)):
        for ruta in sorted(base.rglob("*.java")):
            clases.append(Clase(ruta, origen))
    return clases


def sin_comentarios(texto):
    texto = re.sub(r"/\*.*?\*/", " ", texto, flags=re.S)
    texto = re.sub(r"//[^\n]*", " ", texto)
    return re.sub(r'"(?:\\.|[^"\\])*"', '""', texto)


def asignar_destinos(clases):
    """Fija ``nuevo_sub`` de cada clase de main; devuelve la lista de problemas."""
    problemas = []
    listas = mapa.listas_por_epica()
    for c in clases:
        if c.origen != "main":
            continue
        capa = c.sub.replace(".", "/")
        if capa not in mapa.CAPAS:
            continue
        try:
            capa_destino, sub = mapa.destino_main(capa, c.nombre, c.texto, listas)
        except ValueError as e:
            problemas.append(str(e))
            continue
        c.nuevo_sub = capa_destino.replace("/", ".") + ("." + sub if sub else "")
    return problemas


def asignar_pruebas(clases):
    """Las pruebas van junto a la clase que prueban (ya reubicada)."""
    por_nombre = {c.nombre: c for c in clases if c.origen == "main"}
    for c in clases:
        if c.origen == "main":
            continue
        capa = c.sub.replace(".", "/")
        if capa not in mapa.CAPAS:
            continue
        objetivo = None
        if c.nombre in PRUEBAS_MANUALES:
            capa_m, sub_m = PRUEBAS_MANUALES[c.nombre]
            c.nuevo_sub = capa_m.replace("/", ".") + ("." + sub_m if sub_m else "")
            continue
        base = c.nombre
        for suf in SUFIJOS_PRUEBA:
            if base.endswith(suf) and len(base) > len(suf):
                base = base[:-len(suf)]
                break
        base = re.sub(r"^(Fake)", "", base) if c.nombre.startswith("Fake") else base
        for cand in CANDIDATOS:
            for nombre in (base + cand, cand + base):
                if nombre in por_nombre and por_nombre[nombre].sub.replace(".", "/") == capa:
                    objetivo = por_nombre[nombre]
                    break
            if objetivo:
                break
        if objetivo:
            c.nuevo_sub = objetivo.nuevo_sub


def mapa_de_nombres(clases):
    """(sub_viejo, nombre) -> sub_nuevo para las clases que se mueven."""
    return {(c.sub, c.nombre): c.nuevo_sub for c in clases if c.nuevo_sub != c.sub}


def usa_nombre(texto_sin_comentarios, texto, nombre):
    if re.search(r"\b" + nombre + r"\b", texto_sin_comentarios):
        return True
    return bool(re.search(r"(?:\{@(?:link|linkplain)\s+|@see\s+)" + nombre + r"\b", texto))


def clientes_de(clases):
    """Para cada clase, los archivos que la nombran estando en su mismo paquete actual (acceso implícito)."""
    por_sub = defaultdict(list)
    for c in clases:
        por_sub[c.sub].append(c)
    limpios = {id(c): sin_comentarios(c.texto) for c in clases}
    clientes = defaultdict(list)
    for sub, grupo in por_sub.items():
        for objetivo in grupo:
            for cliente in grupo:
                if cliente is objetivo:
                    continue
                if usa_nombre(limpios[id(cliente)], cliente.texto, objetivo.nombre):
                    clientes[id(objetivo)].append(cliente)
    return clientes, limpios


def ajustar_no_publicas(clases, clientes):
    """Clases sin ``public``: con un único cliente (carpeta) van con él; con varios, a comun. Devuelve el informe."""
    filas = []
    for c in clases:
        if c.publica or c.origen != "main" or c.sub.replace(".", "/") not in mapa.CAPAS:
            continue
        usuarios = [u for u in clientes.get(id(c), []) if u.origen == "main"]
        carpetas = sorted({u.nuevo_sub for u in usuarios})
        filas.append((c, usuarios, carpetas))
        if not carpetas:
            continue
        if len(carpetas) == 1:
            c.nuevo_sub = carpetas[0]
        elif c.sub == "adapter.out.persistence":
            c.nuevo_sub = "adapter.out.persistence.repository"
        else:
            c.nuevo_sub = c.sub + ".comun"
    return filas


def validar(clases):
    problemas = []
    destinos = defaultdict(list)
    for c in clases:
        destinos[(c.nuevo_paquete, c.nombre)].append(c)
    for clave, grupo in destinos.items():
        if len(grupo) > 1:
            problemas.append("colisión de nombre en %s.%s: %s" % (clave[0], clave[1], [g.origen for g in grupo]))
    minus = defaultdict(set)
    for c in clases:
        minus[(c.origen, c.nuevo_paquete.lower(), c.nombre.lower())].add(c.nombre)
    for clave, nombres in minus.items():
        if len(nombres) > 1:
            problemas.append("colisión por mayúsculas: %s" % sorted(nombres))
    return problemas


def simular(clases, filas, problemas):
    print("== Recuento por carpeta destino (main)")
    conteo = defaultdict(int)
    for c in clases:
        if c.origen == "main":
            conteo[c.nuevo_sub] += 1
    for sub in sorted(conteo):
        aviso = "  <-- supera %d archivos" % UMBRAL_CARPETA if conteo[sub] > UMBRAL_CARPETA else ""
        print("  %-50s %3d%s" % (sub, conteo[sub], aviso))
    print("\n== Clases sin 'public' y quién las usa (nueva carpeta de la clase -> carpetas de sus clientes)")
    for c, usuarios, carpetas in filas:
        print("  %-34s %-40s <- %s" % (c.nombre, c.nuevo_sub or "(raíz)", ", ".join(carpetas) or "sin clientes"))
    print("\n== Pruebas que no se pudieron asociar a una clase (se quedan en su paquete)")
    for c in clases:
        if c.origen != "main" and c.sub.replace(".", "/") in mapa.CAPAS and c.nuevo_sub == c.sub \
                and c.nombre not in ("ArquitecturaTest", "EstructuraPaquetesTest"):
            print("  %s/%s" % (c.origen, c.nombre))
    print("\n== Problemas")
    if not problemas:
        print("  ninguno")
    for p in problemas:
        print("  " + p)
    movidos = sum(1 for c in clases if c.nuevo_sub != c.sub)
    print("\nClases que cambian de paquete: %d de %d" % (movidos, len(clases)))


def reescribir_referencias(texto, nombres):
    patron = re.compile(
        r"(?<![\w])(domain|application\.port\.in|application\.port\.out|application\.service|"
        r"adapter\.in\.ui|adapter\.out\.persistence|adapter\.out\.system|adapter\.out\.security)"
        r"\.([A-Z]\w*)(?![\w])")

    def sust(m):
        nuevo = nombres.get((m.group(1), m.group(2)))
        return (nuevo + "." + m.group(2)) if nuevo else m.group(0)

    texto = patron.sub(sust, texto)

    # rutas de archivo escritas como cadenas: "adapter/in/ui/Clase.java"
    ruta = re.compile(
        r"(?<![\w])(domain|application/port/in|application/port/out|application/service|"
        r"adapter/in/ui|adapter/out/persistence|adapter/out/system|adapter/out/security)/([A-Z]\w*)\.java")

    def sust_ruta(m):
        nuevo = nombres.get((m.group(1).replace("/", "."), m.group(2)))
        return (nuevo.replace(".", "/") + "/" + m.group(2) + ".java") if nuevo else m.group(0)

    return ruta.sub(sust_ruta, texto)


def agregar_imports(c, clases_por_sub, limpios, antes_sub):
    """Añade los import de las clases del paquete viejo de ``c`` que ahora están en otra carpeta."""
    texto = c.texto
    necesarios = []
    for otra in clases_por_sub.get(antes_sub, []):
        if otra is c or otra.nuevo_sub == c.nuevo_sub:
            continue
        if re.search(r"^import\s+(?:static\s+)?[\w.]*\." + otra.nombre + r"(?:\.[\w*]+)?;", texto, re.M):
            continue
        if re.search(r"^(?:\s*)(?:public\s+|final\s+|abstract\s+|static\s+)*(?:class|interface|enum)\s+" +
                     otra.nombre + r"\b", texto, re.M):
            continue
        if usa_nombre(limpios[id(c)], c.texto, otra.nombre):
            necesarios.append("import %s.%s;" % (otra.nuevo_paquete, otra.nombre))
    if not necesarios:
        return texto
    lineas = texto.split("\n")
    ultimo = max((i for i, l in enumerate(lineas) if l.startswith("import ")), default=None)
    if ultimo is None:
        idx = next(i for i, l in enumerate(lineas) if l.startswith("package "))
        lineas[idx + 1:idx + 1] = [""] + sorted(necesarios)
    else:
        lineas[ultimo + 1:ultimo + 1] = sorted(necesarios)
    return "\n".join(lineas)


def publicar_clase(texto, nombre):
    texto = re.sub(r"^((?:final\s+|abstract\s+)*(?:class|interface|enum)\s+" + nombre + r"\b)",
                   r"public \1", texto, count=1, flags=re.M)
    if re.search(r"^public\s+(?:final\s+|abstract\s+)*(?:interface|enum)\s+" + nombre, texto, re.M):
        return texto
    return publicar_todos_los_miembros(texto)


# Clases públicas cuyos miembros con visibilidad de paquete usan las pruebas desde otra carpeta: pasan a public.
CLASES_CON_TODOS_LOS_MIEMBROS_PUBLICOS = {"StudyPlanApplication", "NotificadorSesion"}


def publicar_todos_los_miembros(texto):
    """Miembros del primer nivel de la clase (sangría de 4 espacios) sin modificador de acceso pasan a public."""
    miembro = re.compile(
        r"^(    )(?!public\b|private\b|protected\b|@|//|/\*|\*|\}|return\b|case\b|default\b|static\s*\{)"
        r"((?:static\s+|final\s+|abstract\s+|synchronized\s+)*(?:" + TIPO + r"\s+)+\w+\s*[(=;])",
        re.M)
    return miembro.sub(r"\1public \2", texto)


def publicar_anidados(texto):
    """Tipos anidados sin modificador de acceso (indentación de 4) pasan a public."""
    return re.sub(r"^(    )((?:static\s+|final\s+|abstract\s+)*(?:class|interface|enum)\s+\w)", r"\1public \2", texto,
                  flags=re.M)


def publicar_protegidos(texto, nombre):
    for miembro in MIEMBROS_PROTEGIDOS.get(nombre, []):
        texto = re.sub(r"^(    )protected(\s+(?:static\s+|final\s+)*(?:" + TIPO + r"\s+)?" + miembro + r"\s*\()",
                       r"\1public\2", texto, flags=re.M)
    return texto


# Tipo de un miembro: nombre con puntos, genéricos y arreglos (sin espacios de sangría).
TIPO = r"[\w.]+(?:<[^>\n]*>)?(?:\[\])*"


def publicar_miembros(texto, nombre):
    """Pasa a public los miembros nombrados que están en el primer nivel de la clase (sangría de 4 espacios)."""
    for miembro in MIEMBROS_PUBLICOS.get(nombre, []):
        texto = re.sub(r"^(    )(?!public\b|private\b|protected\b)((?:static\s+|final\s+|synchronized\s+)*(?:" + TIPO +
                       r"\s+)?" + miembro + r"\s*[(=;])", r"\1public \2", texto, flags=re.M)
    return texto


def aplicar(clases, capas):
    nombres = mapa_de_nombres(clases)
    por_sub = defaultdict(list)
    for c in clases:
        por_sub[c.sub].append(c)
    limpios = {id(c): sin_comentarios(c.texto) for c in clases}
    antes = {id(c): c.sub for c in clases}
    movibles = [c for c in clases if c.nuevo_sub != c.sub and (not capas or c.sub.replace(".", "/") in capas)]
    # las clases fuera de las capas pedidas no se mueven
    for c in clases:
        if c not in movibles:
            c.nuevo_sub = c.sub
    nombres = mapa_de_nombres(clases)
    nombres_viejo_nuevo = {(c.sub, c.nombre): c.nuevo_sub for c in clases if c.nuevo_sub != c.sub}
    sin_public_movidas = [c for c in clases if not c.publica and c.nuevo_sub != c.sub and c.origen == "main"]
    cambios_de_clientes = {}
    for c in clases:
        texto = reescribir_referencias(c.texto, nombres_viejo_nuevo)
        texto = re.sub(r"^package\s+[\w.]+;", "package %s;" % c.nuevo_paquete, texto, count=1, flags=re.M)
        c.texto = texto
    for c in clases:
        c.texto = agregar_imports(c, por_sub, limpios, antes[id(c)])
    # visibilidad: clases sin public con clientes en otra carpeta
    for c in clases:
        if c.publica or c.origen == "main" and c.nuevo_sub == c.sub and c not in sin_public_movidas:
            pass
    for objetivo in clases:
        if objetivo.publica:
            c_texto = objetivo.texto
            c_texto = publicar_miembros(c_texto, objetivo.nombre)
            if objetivo.nombre in CLASES_CON_TODOS_LOS_MIEMBROS_PUBLICOS:
                c_texto = publicar_todos_los_miembros(c_texto)
            c_texto = publicar_protegidos(c_texto, objetivo.nombre)
            objetivo.texto = c_texto
            continue
        clientes = [k for k in por_sub[antes[id(objetivo)]] if k is not objetivo and
                    usa_nombre(limpios[id(k)], k.texto, objetivo.nombre) and k.nuevo_sub != objetivo.nuevo_sub]
        if clientes:
            objetivo.texto = publicar_clase(objetivo.texto, objetivo.nombre)
            objetivo.texto = publicar_anidados(objetivo.texto)
            objetivo.texto = publicar_miembros(objetivo.texto, objetivo.nombre)
            objetivo.publica = True
            print("  publica: %s (clientes en %s)" % (objetivo.nombre, sorted({k.nuevo_sub for k in clientes})))
    # escribir y mover
    for c in clases:
        destino = c.nueva_ruta
        if destino != c.ruta:
            destino.parent.mkdir(parents=True, exist_ok=True)
            subprocess.run(["git", "mv", str(c.ruta), str(destino)], cwd=RAIZ, check=True)
        destino.write_text(c.texto, encoding="utf-8", newline="\n")
    reescribir_xml(nombres_viejo_nuevo)
    print("Movimiento aplicado: %d clases." % len(movibles))


def reescribir_xml(nombres):
    patron = re.compile(r"\.(adapter\.in\.ui|adapter\.out\.persistence|adapter\.out\.system)\.([A-Z]\w*)\b")
    archivos = [RAIZ / "app" / "src" / "main" / "AndroidManifest.xml"]
    archivos += sorted((RAIZ / "app" / "src" / "main" / "res").glob("layout*/*.xml"))
    for ruta in archivos:
        texto = ruta.read_text(encoding="utf-8")

        def sust(m):
            nuevo = nombres.get((m.group(1), m.group(2)))
            return ("." + nuevo + "." + m.group(2)) if nuevo else m.group(0)

        nuevo_texto = patron.sub(sust, texto)
        if nuevo_texto != texto:
            ruta.write_text(nuevo_texto, encoding="utf-8", newline="\n")


def main():
    if "--simular" not in sys.argv and "--aplicar" not in sys.argv:
        sys.exit(__doc__)
    capas = None
    if "--capas" in sys.argv:
        capas = set(sys.argv[sys.argv.index("--capas") + 1].split(","))
    clases = recolectar()
    problemas = asignar_destinos(clases)
    clientes, _ = clientes_de(clases)
    for _ in range(6):
        antes = [c.nuevo_sub for c in clases]
        filas = ajustar_no_publicas(clases, clientes)
        if antes == [c.nuevo_sub for c in clases]:
            break
    asignar_pruebas(clases)
    problemas += validar(clases)
    if "--simular" in sys.argv:
        simular(clases, filas, problemas)
        return 1 if problemas else 0
    if problemas:
        for p in problemas:
            print("PROBLEMA:", p)
        return 1
    aplicar(clases, capas)
    return 0


if __name__ == "__main__":
    sys.exit(main())
