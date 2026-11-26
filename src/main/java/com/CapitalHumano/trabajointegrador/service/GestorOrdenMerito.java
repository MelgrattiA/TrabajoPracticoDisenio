package com.CapitalHumano.trabajointegrador.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.CapitalHumano.trabajointegrador.entity.CaracteristicaPuesto;
import com.CapitalHumano.trabajointegrador.entity.Cuestionario;
import com.CapitalHumano.trabajointegrador.dto.PuestoOrdenMeritoDTO;
import com.CapitalHumano.trabajointegrador.entity.ResultadoCompetencia;
import com.CapitalHumano.trabajointegrador.repository.CaracteristicaPuestoDAO;
import com.CapitalHumano.trabajointegrador.repository.CuestionarioDAO;
import com.CapitalHumano.trabajointegrador.repository.PuestoDAO;
import com.CapitalHumano.trabajointegrador.repository.PuestoOrdenMeritoProjection;
import com.CapitalHumano.trabajointegrador.repository.ResultadoCompetenciaDAO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GestorOrdenMerito {

    private final PuestoDAO puestoDAO;
    private final CuestionarioDAO cuestionarioDAO;
    private final CaracteristicaPuestoDAO caracteristicaPuestoDAO;
    private final ResultadoCompetenciaDAO resultadoCompetenciaDAO;

    public List<Cuestionario> obtenerCuestionarios(String codigoPuesto) {
        return cuestionarioDAO.buscarTodosPorCodigoPuesto(
                validarCodigoPuesto(codigoPuesto));
    }

    public List<CaracteristicaPuesto> obtenerCaracteristicasPuesto(
            String codigoPuesto) {
        return caracteristicaPuestoDAO.buscarCaracteristicasPorCodigoPuesto(
                validarCodigoPuesto(codigoPuesto));
    }

    public List<ResultadoCompetencia> obtenerResultadosCompetencia(
            String codigoPuesto,
            List<String> clavesCuestionarios) {
        String codigoValidado = validarCodigoPuesto(codigoPuesto);
        if (clavesCuestionarios == null) {
            throw new IllegalArgumentException(
                    "La lista de claves de cuestionarios no puede ser nula");
        }
        if (clavesCuestionarios.isEmpty()) {
            return List.of();
        }
        if (clavesCuestionarios.stream().anyMatch(clave ->
                clave == null || clave.isBlank())) {
            throw new IllegalArgumentException(
                    "Las claves de cuestionarios no pueden ser nulas ni vacías");
        }

        return resultadoCompetenciaDAO.buscarPorPuestoYCuestionarios(
                codigoValidado,
                clavesCuestionarios.stream().distinct().toList());
    }

    public List<PuestoOrdenMeritoDTO> buscarPuestos(
            String codigo,
            String nombre,
            String empresa) {
        String codigoFiltro = normalizarFiltro(codigo);
        String nombreFiltro = normalizarFiltro(nombre);
        String empresaFiltro = normalizarFiltro(empresa);

        if (codigoFiltro == null && nombreFiltro == null && empresaFiltro == null) {
            throw new IllegalArgumentException(
                    "Debe ingresar al menos un criterio de búsqueda");
        }

        return puestoDAO.buscarPuestosOrdenMerito(
                        codigoFiltro,
                        nombreFiltro,
                        empresaFiltro)
                .stream()
                .map(this::aDTO)
                .toList();
    }

    private String normalizarFiltro(String filtro) {
        return filtro == null || filtro.isBlank() ? null : filtro.trim();
    }

    private String validarCodigoPuesto(String codigoPuesto) {
        if (codigoPuesto == null || codigoPuesto.isBlank()) {
            throw new IllegalArgumentException(
                    "El código del puesto es obligatorio");
        }
        return codigoPuesto.trim();
    }

    private PuestoOrdenMeritoDTO aDTO(PuestoOrdenMeritoProjection puesto) {
        return new PuestoOrdenMeritoDTO(
                puesto.getCodigoPuesto(),
                puesto.getNombrePuesto(),
                puesto.getEmpresa(),
                puesto.getCandidatos(),
                puesto.getEvaluacionesCompletas());
    }
}
