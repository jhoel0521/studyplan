package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.metas.DefinirMetaSemanal;
import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Implementa {@link DefinirMetaSemanal}: fija la meta semanal total para la semana actual
 * (lunes a domingo) sin alterar las semanas pasadas ni la meta predeterminada del perfil
 * ({@code HU-25}, {@code RN-14}, {@code RN-15}, {@code RN-33}).
 */
public final class DefinirMetaSemanalService implements DefinirMetaSemanal {

    private final MetaSemanalRepository metas;
    private final Reloj reloj;

    public DefinirMetaSemanalService(MetaSemanalRepository metas, Reloj reloj) {
        this.metas = Objects.requireNonNull(metas, "metas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Resultado<MetaSemanal> ejecutar(double horasObjetivo) {
        Fecha lunes = reloj.hoy().lunesDeSemana();
        MetaSemanal existente = metas.buscar(lunes, null).orElse(null);
        Resultado<MetaSemanal> res = existente == null
                ? MetaSemanal.crear(lunes, null, horasObjetivo)
                : existente.conObjetivo(horasObjetivo);
        if (!res.esValido()) {
            return res;
        }
        return Resultado.ok(metas.guardar(res.valor()));
    }
}
