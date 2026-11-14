package io.github.jhoel0521.studyplan.domain;

/**
 * Datos de la pantalla de Inicio: el nombre del estudiante y el progreso de la meta semanal.
 *
 * <p>Implementa, de forma parcial: HU-02 (Ver resumen en la pantalla de inicio) y RN-15 (se usa la meta
 * predeterminada del perfil). Las horas realizadas son 0 hasta que existan sesiones; quedan pendientes los
 * escenarios «Resumen con datos» y «Acceso rápido» del change de sesiones.
 */
public final class ResumenInicio {

    private final String nombre;
    private final double horasRealizadas;
    private final double metaSemanalHoras;

    public ResumenInicio(String nombre, double horasRealizadas, double metaSemanalHoras) {
        this.nombre = nombre;
        this.horasRealizadas = horasRealizadas;
        this.metaSemanalHoras = metaSemanalHoras;
    }

    public String nombre() {
        return nombre;
    }

    public double horasRealizadas() {
        return horasRealizadas;
    }

    public double metaSemanalHoras() {
        return metaSemanalHoras;
    }

    /** @return el avance de la meta como porcentaje entero entre 0 y 100 */
    public int porcentajeProgreso() {
        if (metaSemanalHoras <= 0) {
            return 0;
        }
        long porcentaje = Math.round(horasRealizadas / metaSemanalHoras * 100);
        return (int) Math.max(0, Math.min(100, porcentaje));
    }
}
