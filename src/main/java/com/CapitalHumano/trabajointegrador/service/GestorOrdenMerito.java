package com.CapitalHumano.trabajointegrador.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CapitalHumano.trabajointegrador.dto.ClasificacionCuestionariosDTO;
import com.CapitalHumano.trabajointegrador.dto.OrdenMeritoDTO;
import com.CapitalHumano.trabajointegrador.dto.OrdenMeritoIncompletoDTO;
import com.CapitalHumano.trabajointegrador.entity.CaracteristicaPuesto;
import com.CapitalHumano.trabajointegrador.entity.Cuestionario;
import com.CapitalHumano.trabajointegrador.dto.PuestoOrdenMeritoDTO;
import com.CapitalHumano.trabajointegrador.dto.ReporteOrdenMeritoDTO;
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

    private static final String ESTADO_COMPLETO = "Completo";
    private static final List<String> ESTADOS_SIN_COMPLETAR =
            List.of("Activo", "En Proceso", "Incompleto", "Sin Contestar");

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

    @Transactional(readOnly = true)
    public ClasificacionCuestionariosDTO clasificarCuestionariosPorPuesto(
            String codigoPuesto) {
        String codigoValidado = validarCodigoPuesto(codigoPuesto);
        List<CaracteristicaPuesto> caracteristicas =
                obtenerCaracteristicasPuesto(codigoValidado);
        validarCaracteristicasConfiguradas(caracteristicas);
        List<Cuestionario> cuestionarios = obtenerCuestionarios(codigoValidado);
        return clasificarCuestionarios(
                codigoValidado, caracteristicas, cuestionarios);
    }

    @Transactional(readOnly = true)
    public ReporteOrdenMeritoDTO generarReporteOrdenMerito(String codigoPuesto) {
        String codigoValidado = validarCodigoPuesto(codigoPuesto);
        List<CaracteristicaPuesto> caracteristicas =
                obtenerCaracteristicasPuesto(codigoValidado);
        validarCaracteristicasConfiguradas(caracteristicas);
        List<Cuestionario> cuestionarios = obtenerCuestionarios(codigoValidado);
        ClasificacionCuestionariosDTO clasificacion =
                clasificarCuestionarios(
                        codigoValidado, caracteristicas, cuestionarios);

        Map<String, Cuestionario> cuestionariosPorClave = new HashMap<>();
        cuestionarios.forEach(cuestionario ->
                cuestionariosPorClave.put(cuestionario.getClave(), cuestionario));

        List<OrdenMeritoDTO> aprobados = new ArrayList<>();
        for (int indice = 0;
                indice < clasificacion.cuestionariosAprobados().size();
                indice++) {
            String clave = clasificacion.cuestionariosAprobados().get(indice);
            aprobados.add(aOrdenMeritoDTO(
                    buscarCuestionario(cuestionariosPorClave, clave),
                    indice + 1,
                    true));
        }

        List<OrdenMeritoDTO> desaprobados =
                clasificacion.cuestionariosDesaprobados().stream()
                        .map(clave -> aOrdenMeritoDTO(
                                buscarCuestionario(cuestionariosPorClave, clave),
                                null,
                                false))
                        .toList();

        Map<String, List<OrdenMeritoIncompletoDTO>> sinCompletarPorEstado =
                new LinkedHashMap<>();
        clasificacion.cuestionariosSinCompletarPorEstado().forEach(
                (estado, claves) -> sinCompletarPorEstado.put(
                        estado,
                        claves.stream()
                                .map(clave -> aOrdenMeritoIncompletoDTO(
                                        buscarCuestionario(
                                                cuestionariosPorClave, clave)))
                                .toList()));

        return new ReporteOrdenMeritoDTO(
                aprobados, desaprobados, sinCompletarPorEstado);
    }

    private ClasificacionCuestionariosDTO clasificarCuestionarios(
            String codigoPuesto,
            List<CaracteristicaPuesto> caracteristicas,
            List<Cuestionario> cuestionarios) {
        Map<String, List<String>> sinCompletarPorEstado = new LinkedHashMap<>();
        ESTADOS_SIN_COMPLETAR.forEach(estado ->
                sinCompletarPorEstado.put(estado, new ArrayList<>()));
        List<Cuestionario> cuestionariosAprobados = new ArrayList<>();
        List<String> desaprobados = new ArrayList<>();
        Map<String, ClasificacionCuestionariosDTO.InconsistenciaCuestionarioDTO>
                inconsistentes = new LinkedHashMap<>();

        if (cuestionarios.isEmpty()) {
            return new ClasificacionCuestionariosDTO(
                    List.of(), desaprobados, sinCompletarPorEstado, inconsistentes);
        }

        List<String> clavesCuestionarios = cuestionarios.stream()
                .map(Cuestionario::getClave)
                .toList();
        Map<String, Map<Integer, ResultadoCompetencia>> resultadosPorCuestionario =
                new HashMap<>();
        obtenerResultadosCompetencia(codigoPuesto, clavesCuestionarios)
                .forEach(resultado -> resultadosPorCuestionario
                        .computeIfAbsent(
                                resultado.getId().getClaveCuestionario(),
                                clave -> new HashMap<>())
                        .put(
                                resultado.getId().getCodigoCompetencia(),
                                resultado));

        for (Cuestionario cuestionario : cuestionarios) {
            String claveCuestionario = cuestionario.getClave();
            String estado = cuestionario.getEstado().getNombre();
            if (!ESTADO_COMPLETO.equals(estado)) {
                sinCompletarPorEstado
                        .computeIfAbsent(estado, clave -> new ArrayList<>())
                        .add(claveCuestionario);
                continue;
            }

            Map<Integer, ResultadoCompetencia> resultados =
                    resultadosPorCuestionario.getOrDefault(
                            claveCuestionario, Map.of());
            List<Integer> competenciasFaltantes = new ArrayList<>();
            boolean cumplePonderacionesMinimas = true;
            for (CaracteristicaPuesto caracteristica : caracteristicas) {
                Integer codigoCompetencia =
                        caracteristica.getCompetencia().getCodigo();
                ResultadoCompetencia resultado =
                        resultados.get(codigoCompetencia);
                if (resultado == null || resultado.getPuntajeObtenido() == null) {
                    competenciasFaltantes.add(codigoCompetencia);
                    continue;
                }
                if (resultado.getPuntajeObtenido()
                        .compareTo(caracteristica.getPonderacionMinima()) < 0) {
                    cumplePonderacionesMinimas = false;
                }
            }

            boolean puntajeFinalAusente = cuestionario.getPuntajeFinal() == null;
            if (!competenciasFaltantes.isEmpty() || puntajeFinalAusente) {
                inconsistentes.put(
                        claveCuestionario,
                        new ClasificacionCuestionariosDTO.InconsistenciaCuestionarioDTO(
                                competenciasFaltantes,
                                puntajeFinalAusente));
                continue;
            }

            if (cumplePonderacionesMinimas) {
                cuestionariosAprobados.add(cuestionario);
            } else {
                desaprobados.add(claveCuestionario);
            }
        }

        cuestionariosAprobados.sort(
                Comparator.comparing(
                                Cuestionario::getPuntajeFinal,
                                Comparator.reverseOrder())
                        .thenComparing(cuestionario ->
                                cuestionario.getCandidato().getNroCandidato())
                        .thenComparing(
                                Cuestionario::getFechaInicio,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Cuestionario::getClave));
        List<String> aprobados = cuestionariosAprobados.stream()
                .map(Cuestionario::getClave)
                .toList();

        return new ClasificacionCuestionariosDTO(
                aprobados, desaprobados, sinCompletarPorEstado, inconsistentes);
    }

    private void validarCaracteristicasConfiguradas(
            List<CaracteristicaPuesto> caracteristicas) {
        if (caracteristicas.isEmpty()) {
            throw new IllegalStateException(
                    "El puesto no tiene competencias configuradas");
        }
    }

    private Cuestionario buscarCuestionario(
            Map<String, Cuestionario> cuestionariosPorClave,
            String claveCuestionario) {
        Cuestionario cuestionario = cuestionariosPorClave.get(claveCuestionario);
        if (cuestionario == null) {
            throw new IllegalStateException(
                    "No se encontró el cuestionario clasificado: "
                            + claveCuestionario);
        }
        return cuestionario;
    }

    private OrdenMeritoDTO aOrdenMeritoDTO(
            Cuestionario cuestionario,
            Integer posicion,
            Boolean superaMinimo) {
        return new OrdenMeritoDTO(
                cuestionario.getClave(),
                posicion,
                cuestionario.getCandidato().getNombre(),
                cuestionario.getCandidato().getApellido(),
                cuestionario.getCandidato().getTipoDocumento(),
                cuestionario.getCandidato().getNroDocumento(),
                cuestionario.getFechaInicio(),
                cuestionario.getFechaFinalizacion(),
                cuestionario.getCantidadAccesos(),
                cuestionario.getPuntajeFinal(),
                superaMinimo);
    }

    private OrdenMeritoIncompletoDTO aOrdenMeritoIncompletoDTO(
            Cuestionario cuestionario) {
        return new OrdenMeritoIncompletoDTO(
                cuestionario.getClave(),
                cuestionario.getCandidato().getNombre(),
                cuestionario.getCandidato().getApellido(),
                cuestionario.getCandidato().getTipoDocumento(),
                cuestionario.getCandidato().getNroDocumento(),
                cuestionario.getFechaInicio(),
                cuestionario.getFechaUltimoIngreso(),
                cuestionario.getCantidadAccesos());
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
