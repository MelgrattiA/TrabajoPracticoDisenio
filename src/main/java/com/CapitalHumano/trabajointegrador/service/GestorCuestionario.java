package com.CapitalHumano.trabajointegrador.service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CapitalHumano.trabajointegrador.dto.RegistroActualizacionDeEstadoDTO;
import com.CapitalHumano.trabajointegrador.dto.BloquePreguntaDTO;
import com.CapitalHumano.trabajointegrador.dto.EstadoInstruccionesCuestionarioDTO;
import com.CapitalHumano.trabajointegrador.dto.OpcionRespuestaDTO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaBloqueDTO;
import com.CapitalHumano.trabajointegrador.dto.RespuestaBloqueDTO;
import com.CapitalHumano.trabajointegrador.dto.RespuestaPreguntaDTO;
import com.CapitalHumano.trabajointegrador.entity.Cuestionario;
import com.CapitalHumano.trabajointegrador.entity.RegistroActualizacionDeEstado;
import com.CapitalHumano.trabajointegrador.entity.BloquePregunta;
import com.CapitalHumano.trabajointegrador.entity.Pregunta;
import com.CapitalHumano.trabajointegrador.entity.PreguntaBloque;
import com.CapitalHumano.trabajointegrador.entity.PonderacionItem;
import com.CapitalHumano.trabajointegrador.entity.ResultadoCompetencia;
import com.CapitalHumano.trabajointegrador.entity.ResultadoCompetenciaId;
import com.CapitalHumano.trabajointegrador.entity.CaracteristicaPuesto;
import com.CapitalHumano.trabajointegrador.entity.Competencia;
import com.CapitalHumano.trabajointegrador.entity.ConfiguracionSistema;
import com.CapitalHumano.trabajointegrador.entity.Factor;
import com.CapitalHumano.trabajointegrador.repository.BloquePreguntaDAO;
import com.CapitalHumano.trabajointegrador.repository.CaracteristicaPuestoDAO;
import com.CapitalHumano.trabajointegrador.repository.CuestionarioDAO;
import com.CapitalHumano.trabajointegrador.repository.EstadoCuestionarioDAO;
import com.CapitalHumano.trabajointegrador.repository.FactorDAO;
import com.CapitalHumano.trabajointegrador.repository.PreguntaDAO;
import com.CapitalHumano.trabajointegrador.repository.RegistroActualizacionDeEstadoDAO;
import com.CapitalHumano.trabajointegrador.repository.PreguntaBloqueDAO;
import com.CapitalHumano.trabajointegrador.repository.ItemOpcionDAO;
import com.CapitalHumano.trabajointegrador.repository.PonderacionItemDAO;
import com.CapitalHumano.trabajointegrador.repository.ResultadoCompetenciaDAO;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;
import java.math.RoundingMode;


@Service
public class GestorCuestionario {

    private static final String ESTADO_NORMAL = "normal";
    private static final String ESTADO_MAX_ACCESOS = "max-accesos";
    private static final String ESTADO_TIEMPO_COMPLETAR = "tiempo-completar";
    private static final String ESTADO_TIEMPO_INICIO = "tiempo-inicio";
    private static final String ESTADO_COMPLETO = "completo";

    private final CuestionarioDAO cuestionarioDAO;
    private final EstadoCuestionarioDAO estadoCuestionarioDAO;
    private final RegistroActualizacionDeEstadoDAO registroActualizacionDeEstadoDAO;
    private final BloquePreguntaDAO bloquePreguntaDAO;
    private final PreguntaBloqueDAO preguntaBloqueDAO;
    private final CaracteristicaPuestoDAO caracteristicaPuestoDAO;
    private final FactorDAO factorDAO;
    private final PreguntaDAO preguntaDAO;
    private final ItemOpcionDAO itemOpcionDAO;
    private final PonderacionItemDAO ponderacionItemDAO;
    private final ResultadoCompetenciaDAO resultadoCompetenciaDAO;
    private final ConfiguracionSistemaService configuracionSistemaService;

    public GestorCuestionario(
            CuestionarioDAO cuestionarioDAO,
            EstadoCuestionarioDAO estadoCuestionarioDAO,
            RegistroActualizacionDeEstadoDAO registroActualizacionDeEstadoDAO,
            BloquePreguntaDAO bloquePreguntaDAO,
            PreguntaBloqueDAO preguntaBloqueDAO,
            CaracteristicaPuestoDAO caracteristicaPuestoDAO,
            FactorDAO factorDAO,
            PreguntaDAO preguntaDAO,
            ItemOpcionDAO itemOpcionDAO,
            PonderacionItemDAO ponderacionItemDAO,
            ResultadoCompetenciaDAO resultadoCompetenciaDAO,
            ConfiguracionSistemaService configuracionSistemaService) {
        this.cuestionarioDAO = cuestionarioDAO;
        this.estadoCuestionarioDAO = estadoCuestionarioDAO;
        this.registroActualizacionDeEstadoDAO = registroActualizacionDeEstadoDAO;
        this.bloquePreguntaDAO = bloquePreguntaDAO;
        this.preguntaBloqueDAO = preguntaBloqueDAO;
        this.caracteristicaPuestoDAO = caracteristicaPuestoDAO;
        this.factorDAO = factorDAO;
        this.preguntaDAO = preguntaDAO;
        this.itemOpcionDAO = itemOpcionDAO;
        this.ponderacionItemDAO = ponderacionItemDAO;
        this.resultadoCompetenciaDAO = resultadoCompetenciaDAO;
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @Transactional
    public BloquePreguntaDTO iniciarCuestionario(String claveCuestionario) {
        ConfiguracionSistema configuracion =
                configuracionSistemaService.obtenerConfiguracion();

        // La clave generada al crear la evaluación identifica el cuestionario existente.
        Cuestionario cuestionario = cuestionarioDAO.findById(claveCuestionario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el cuestionario indicado"));
        Integer estadoActual = cuestionario.getEstado().getIdEstadoCuestionario();
        if (estadoActual != 1 && estadoActual != 2) {
            throw new IllegalStateException(
                    "El cuestionario no está disponible para iniciar o continuar");
        }

        EstadoInstruccionesCuestionarioDTO disponibilidad =
                validarAccesoYTiempo(cuestionario, configuracion);
        if (!disponibilidad.isPuedeContinuar()) {
            throw new IllegalStateException(disponibilidad.getMensaje());
        }

        // Los bloques se generan al aceptar, evitando duplicarlos si ya existían.
        if (cuestionario.getEstado().getIdEstadoCuestionario() == 1) {
            if (!bloquePreguntaDAO.existsByCuestionarioClave(claveCuestionario)) {
                asociarPreguntasAZar(
                        cuestionario,
                        configuracion.getCantidadPreguntasPorBloque(),
                        configuracion.getCantidadPreguntasPorFactor());
            }
        } else {
            // Al reingresar a un cuestionario en proceso se conserva el bloque pendiente.
            recuperarBloquePendiente(cuestionario);
        }

        // Se usa la misma fecha y hora para inicio e último ingreso.
        LocalDateTime ahora = LocalDateTime.now();

        // El ingreso inicia el plazo de respuesta la primera vez que se acepta.
        if (cuestionario.getFechaInicio() == null) {
            cuestionario.setFechaInicio(ahora);
        }

        // Se actualiza la fecha de último ingreso y se incrementa la cantidad de accesos.
        cuestionario.setFechaUltimoIngreso(ahora);
        cuestionario.setCantidadAccesos(cuestionario.getCantidadAccesos() + 1);

        // Cada ingreso válido deja el cuestionario en estado En Proceso.
        cuestionario.setEstado(estadoCuestionarioDAO.findById(2)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado En Proceso")));

        // Se guarda el inicio antes de devolver el primer bloque pendiente.
        cuestionarioDAO.save(cuestionario);
        return obtenerBloquePendiente(claveCuestionario);
    }

    public Cuestionario actualizarEstadoCuestionario(
            Cuestionario cuestionario,
            Integer idEstado,
            String accion) {

        // Se actualiza el estado del cuestionario.
        cuestionario.setEstado(estadoCuestionarioDAO.findById(idEstado)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado del cuestionario")));

        // Se guarda el cuestionario actualizado.
        Cuestionario cuestionarioActualizado = cuestionarioDAO.save(cuestionario);

        // Se crea un DTO para el registro de actualización del estado.
        LocalDateTime ahora = LocalDateTime.now();
        RegistroActualizacionDeEstadoDTO registroDTO =
                new RegistroActualizacionDeEstadoDTO(
                        LocalDate.from(ahora),
                        LocalTime.from(ahora),
                        accion);

        // Se guarda el registro de actualización del estado.
        guardarRegistro(registroDTO);

        return cuestionarioActualizado;
    }

    @Transactional
    public EstadoInstruccionesCuestionarioDTO obtenerEstadoInstrucciones(String claveCuestionario) {
        ConfiguracionSistema configuracion =
                configuracionSistemaService.obtenerConfiguracion();
        Cuestionario cuestionario = cuestionarioDAO.findById(claveCuestionario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el cuestionario indicado"));

        Integer idEstado = cuestionario.getEstado().getIdEstadoCuestionario();

        if (idEstado == 1 || idEstado == 2) {
            return validarAccesoYTiempo(cuestionario, configuracion);
        }

        // Se valida el estado del cuestionario y se devuelve el mensaje correspondiente.
        if (idEstado == 4) {
            boolean excedioAccesos = (cuestionario.getCantidadAccesos() != null)
                    && cuestionario.getCantidadAccesos()
                            >= configuracion.getMaximoAccesos();
            if (excedioAccesos) {
                return new EstadoInstruccionesCuestionarioDTO(
                        ESTADO_MAX_ACCESOS,
                        "Superaste la cantidad maxima de accesos permitidos al cuestionario.",
                        false,
                        configuracion.getInstruccionesCuestionario(),
                        configuracion.getCantidadPreguntasPorBloque(),
                        configuracion.getCantidadPreguntasPorFactor(),
                        configuracion.getMaximoAccesos(),
                        configuracion.getTiempoMaximoCompletarDias(),
                        configuracion.getTiempoMaximoActivoDias());
            }
            return new EstadoInstruccionesCuestionarioDTO(
                    ESTADO_TIEMPO_COMPLETAR,
                    "Se supero el tiempo limite para terminar de completar el cuestionario.",
                    false,
                    configuracion.getInstruccionesCuestionario(),
                    configuracion.getCantidadPreguntasPorBloque(),
                    configuracion.getCantidadPreguntasPorFactor(),
                    configuracion.getMaximoAccesos(),
                    configuracion.getTiempoMaximoCompletarDias(),
                    configuracion.getTiempoMaximoActivoDias());
        }

        // Se valida el estado del cuestionario y se devuelve el mensaje correspondiente.
        if (idEstado == 5) {
            return new EstadoInstruccionesCuestionarioDTO(
                    ESTADO_TIEMPO_INICIO,
                    "Se supero el tiempo limite para iniciar el cuestionario.",
                    false,
                    configuracion.getInstruccionesCuestionario(),
                    configuracion.getCantidadPreguntasPorBloque(),
                    configuracion.getCantidadPreguntasPorFactor(),
                    configuracion.getMaximoAccesos(),
                    configuracion.getTiempoMaximoCompletarDias(),
                    configuracion.getTiempoMaximoActivoDias());
        }

        // Se valida el estado del cuestionario y se devuelve el mensaje correspondiente.
        if (idEstado == 3) {
            return new EstadoInstruccionesCuestionarioDTO(
                    ESTADO_COMPLETO,
                    "Este cuestionario ya fue completado.",
                    false,
                    configuracion.getInstruccionesCuestionario(),
                    configuracion.getCantidadPreguntasPorBloque(),
                    configuracion.getCantidadPreguntasPorFactor(),
                    configuracion.getMaximoAccesos(),
                    configuracion.getTiempoMaximoCompletarDias(),
                    configuracion.getTiempoMaximoActivoDias());
        }

        throw new IllegalStateException("El cuestionario posee un estado no soportado");
    }

    private EstadoInstruccionesCuestionarioDTO validarAccesoYTiempo(
            Cuestionario cuestionario,
            ConfiguracionSistema configuracion) {
        Integer idEstado = cuestionario.getEstado().getIdEstadoCuestionario();

        // Verificar acceso
        if (!cuestionario.verificarAcceso(configuracion.getMaximoAccesos())) {
            actualizarEstadoCuestionario(
                    cuestionario,
                    4,
                    "Cuestionario incompleto por superar accesos máximos");
            return new EstadoInstruccionesCuestionarioDTO(
                    ESTADO_MAX_ACCESOS,
                    "Superaste la cantidad maxima de accesos permitidos al cuestionario.",
                    false,
                    configuracion.getInstruccionesCuestionario(),
                    configuracion.getCantidadPreguntasPorBloque(),
                    configuracion.getCantidadPreguntasPorFactor(),
                    configuracion.getMaximoAccesos(),
                    configuracion.getTiempoMaximoCompletarDias(),
                    configuracion.getTiempoMaximoActivoDias());
        }

        LocalDateTime fechaInicioLimite = idEstado == 1
                ? cuestionario.getFechaCreacion()
                : cuestionario.getFechaInicio();
        int diasLimite = idEstado == 1
                ? configuracion.getTiempoMaximoActivoDias()
                : configuracion.getTiempoMaximoCompletarDias();
        if (fechaInicioLimite != null
                && LocalDateTime.now().isAfter(fechaInicioLimite.plusDays(diasLimite))) {
            boolean estabaEnProceso = idEstado == 2;
            actualizarEstadoCuestionario(
                    cuestionario,
                    estabaEnProceso ? 4 : 5,
                    "Cuestionario vencido por superar el tiempo máximo");
            return new EstadoInstruccionesCuestionarioDTO(
                    estabaEnProceso
                            ? ESTADO_TIEMPO_COMPLETAR
                            : ESTADO_TIEMPO_INICIO,
                    estabaEnProceso
                            ? "Se supero el tiempo limite para terminar de completar el cuestionario."
                            : "Se supero el tiempo limite para iniciar el cuestionario.",
                    false,
                    configuracion.getInstruccionesCuestionario(),
                    configuracion.getCantidadPreguntasPorBloque(),
                    configuracion.getCantidadPreguntasPorFactor(),
                    configuracion.getMaximoAccesos(),
                    configuracion.getTiempoMaximoCompletarDias(),
                    configuracion.getTiempoMaximoActivoDias());
        }

        return new EstadoInstruccionesCuestionarioDTO(
                ESTADO_NORMAL,
                "",
                true,
                configuracion.getInstruccionesCuestionario(),
                configuracion.getCantidadPreguntasPorBloque(),
                configuracion.getCantidadPreguntasPorFactor(),
                configuracion.getMaximoAccesos(),
                configuracion.getTiempoMaximoCompletarDias(),
                configuracion.getTiempoMaximoActivoDias());
    }

    public List<Pregunta> asociarPreguntasAZar(Cuestionario cuestionario,
                 int cantidadPreguntasPorBloque,
                 int cantidadPreguntasPorFactor) {


        if (cantidadPreguntasPorBloque <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de preguntas por bloque debe ser mayor que cero");
        }
        if (cantidadPreguntasPorFactor <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de preguntas por factor debe ser mayor que cero");
        }

        List<Pregunta> preguntasSeleccionadas = new ArrayList<>();

        // Se buscan las competencias evaluadas para el puesto del cuestionario.
        List<Competencia> competencias =
                caracteristicaPuestoDAO.buscarCompetenciasEvaluadas(
                        cuestionario.getPuesto().getCodigoPuesto());


        // Se recorren las competencias y se seleccionan preguntas aleatorias de cada factor.
        for (Competencia competencia : competencias) {

            // Se buscan los factores de la competencia
            List<Factor> factores =
                    factorDAO.findByCompetenciaCodigoAndEliminadoFalseOrderByNroOrdenAsc(
                            competencia.getCodigo());

            // Se recorren los factores y se seleccionan preguntas aleatorias de cada factor
            for (Factor factor : factores) {

                List<Pregunta> preguntas =
                        preguntaDAO.findByFactorCodigoAndEliminadoFalse(
                                factor.getCodigo());


                if (preguntas.size() < cantidadPreguntasPorFactor) {
                    continue;
                }

                Collections.shuffle(preguntas);
                preguntasSeleccionadas.addAll(
                        preguntas.subList(0, cantidadPreguntasPorFactor));
            }
        }

        if (preguntasSeleccionadas.isEmpty()) {
            throw new IllegalStateException(
                    "No hay factores con la cantidad mínima de preguntas configurada");
        }

        // Se mezclan las preguntas seleccionadas
        Collections.shuffle(preguntasSeleccionadas);


        int nroOrdenBloque = 1;
        for (int inicio = 0;
                inicio < preguntasSeleccionadas.size();
                inicio += cantidadPreguntasPorBloque) {

            // Se calcula el índice final del bloque porque
            // puede que no haya suficientes preguntas para completar un bloque, si no hay suficientes preguntas,
            // se toma el resto de las preguntas.
            int fin = Math.min(
                    inicio + cantidadPreguntasPorBloque,
                    preguntasSeleccionadas.size());

            // Se crea un nuevo bloque de preguntas y se guarda en la base de datos
            BloquePregunta bloque = BloquePregunta.builder()
                    .cuestionario(cuestionario)
                    .nroOrden(nroOrdenBloque++)
                    .build();
            bloque = bloquePreguntaDAO.save(bloque);

            // Se guardan las preguntas del bloque en la base de datos
            int ordenEnBloque = 1;
            for (int i = inicio; i < fin; i++) {
                preguntaBloqueDAO.save(PreguntaBloque.builder()
                        .bloquePregunta(bloque)
                        .pregunta(preguntasSeleccionadas.get(i))
                        .ordenEnBloque(ordenEnBloque++)
                        .itemOpcionSeleccionada(null)
                        .build());
            }
        }

        return preguntasSeleccionadas;
    }

    public List<Pregunta> recuperarBloquePendiente(Cuestionario cuestionario) {
        BloquePregunta bloquePendiente = bloquePreguntaDAO
                .buscarBloquePendiente(cuestionario.getClave())
                .orElseThrow(() -> new IllegalStateException(
                        "No existe un bloque pendiente para el cuestionario"));

        return preguntaBloqueDAO.buscarPreguntasPendientes(bloquePendiente.getIdBloquePregunta());
    }

    @Transactional
    public BloquePreguntaDTO obtenerBloquePendiente(String claveCuestionario) {
        BloquePregunta bloque = bloquePreguntaDAO.buscarBloquePendiente(claveCuestionario)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe un bloque pendiente para el cuestionario"));
        return convertirBloque(bloque);
    }

    @Transactional
    public RespuestaBloqueDTO responderBloque(
            String claveCuestionario,
            Long idBloque,
            List<RespuestaPreguntaDTO> respuestas) {

        // Se busca el cuestionario y se valida que esté en proceso.
        Cuestionario cuestionario = cuestionarioDAO.findById(claveCuestionario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el cuestionario indicado"));
        if (cuestionario.getEstado().getIdEstadoCuestionario() != 2) {
            throw new IllegalStateException(
                    "El cuestionario no se encuentra en proceso");
        }

        // Se busca el bloque y se valida que pertenezca al cuestionario.
        BloquePregunta bloque = bloquePreguntaDAO.findById(idBloque)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el bloque indicado"));
        if (!bloque.getCuestionario().getClave().equals(cuestionario.getClave())) {
            throw new IllegalArgumentException("El bloque no pertenece al cuestionario indicado");
        }

        // Se obtienen las preguntas del bloque y se valida que todas hayan sido respondidas.
        List<PreguntaBloque> preguntas = preguntaBloqueDAO
                .findByBloquePreguntaIdBloquePreguntaOrderByOrdenEnBloqueAsc(idBloque);
        if (respuestas == null || respuestas.size() != preguntas.size()) {
            return new RespuestaBloqueDTO(
                    false,
                    false,
                    null,
                    // Se obtienen los IDs de las preguntas que no tienen respuesta.
                    preguntas.stream()
                            .filter(p -> p.getItemOpcionSeleccionada() == null)
                            .map(PreguntaBloque::getIdPreguntaBloque)
                            .toList());
        }

        // Se valida que no se respondan dos veces la misma pregunta y que las opciones sean válidas.
        Set<Long> idsRespondidos = new HashSet<>();
        for (PreguntaBloque preguntaBloque : preguntas) {
            // Se busca la respuesta correspondiente a la pregunta del bloque.
            RespuestaPreguntaDTO respuesta = respuestas.stream()
                    .filter(r -> r != null
                            && preguntaBloque.getIdPreguntaBloque().equals(r.getIdPreguntaBloque()))
                    .findFirst()
                    .orElse(null);

            // Se valida que la respuesta no sea nula y que tenga un ID de opción válido.
            if (respuesta == null || respuesta.getIdItemOpcion() == null) {
                return new RespuestaBloqueDTO(
                        false,
                        false,
                        null,
                        List.of(preguntaBloque.getIdPreguntaBloque()));
            }
            // Se valida que no se responda dos veces la misma pregunta.
            if (!idsRespondidos.add(respuesta.getIdPreguntaBloque())) {
                throw new IllegalArgumentException("No se puede responder dos veces la misma pregunta");
            }

            // Se valida que la opción seleccionada corresponda a la pregunta.
            var item = itemOpcionDAO.findById(respuesta.getIdItemOpcion())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La opción indicada no existe"));
            if (!item.getOpcionRespuesta().getIdOpcionRespuesta()
                    .equals(preguntaBloque.getPregunta().getOpcionRespuesta().getIdOpcionRespuesta())) {
                throw new IllegalArgumentException(
                        "La opción no corresponde a la pregunta indicada");
            }
            // Se guarda la opción seleccionada.
            preguntaBloque.setItemOpcionSeleccionada(item);
        }

        // Se guardan todas las preguntas del bloque con sus respuestas.
        preguntaBloqueDAO.saveAll(preguntas);

        // Se busca el siguiente bloque pendiente, si no hay, se completa el cuestionario.
        BloquePregunta siguiente = bloquePreguntaDAO
                .buscarBloquePendiente(claveCuestionario)
                .orElse(null);
        if (siguiente == null) {
            calcularPuntajes(cuestionario);
            actualizarEstadoCuestionario(
                    cuestionario,
                    3,
                    "Cuestionario completado");
            cuestionario.setFechaFinalizacion(LocalDateTime.now());
            cuestionarioDAO.save(cuestionario);
        }
        // Se devuelve la respuesta indicando que se guardó correctamente y si el cuestionario está completo.
        return new RespuestaBloqueDTO(
                true,
                siguiente == null,
                siguiente == null ? null : convertirBloque(siguiente),
                List.of());
    }

    private void calcularPuntajes(Cuestionario cuestionario) {
        // Se buscan todas las respuestas del cuestionario.
        List<PreguntaBloque> respuestas = preguntaBloqueDAO
                .buscarRespondidasPorCuestionario(cuestionario.getClave());
        if (respuestas.isEmpty()) {
            throw new IllegalStateException(
                    "No hay respuestas para calcular el puntaje del cuestionario");
        }

        // Se calcula la suma de ponderaciones por competencia y factor.
        Map<Integer, Map<Integer, BigDecimal>> sumaPorCompetenciaYFactor = new HashMap<>();

        // Se recorre cada respuesta y se obtiene la ponderación correspondiente.
        for (PreguntaBloque respuesta : respuestas) {
            // Se busca la ponderación del item seleccionado para la pregunta.
            PonderacionItem ponderacion = ponderacionItemDAO
                    .findByPreguntaCodigoAndItemOpcionIdItemOpcion(
                            respuesta.getPregunta().getCodigo(),
                            respuesta.getItemOpcionSeleccionada().getIdItemOpcion())
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe ponderación para la respuesta seleccionada de la pregunta "
                                    + respuesta.getPregunta().getCodigo()));

            // Se obtiene el código de la competencia y del factor de la pregunta.
            Integer codigoCompetencia = respuesta.getPregunta().getFactor()
                    .getCompetencia().getCodigo();
            Integer codigoFactor = respuesta.getPregunta().getFactor().getCodigo();

            // Se acumula la ponderación en la suma por competencia y factor.
            sumaPorCompetenciaYFactor
                    .computeIfAbsent(codigoCompetencia, clave -> new HashMap<>())
                    .merge(codigoFactor, ponderacion.getPonderacion(), BigDecimal::add);
        }

        // Se buscan las competencias evaluadas para el puesto del cuestionario.
        List<CaracteristicaPuesto> caracteristicas =
                caracteristicaPuestoDAO.buscarCaracteristicaPuesto(
                        cuestionario.getPuesto().getCodigoPuesto());
        if (caracteristicas.isEmpty()) {
            throw new IllegalStateException(
                    "El puesto no tiene competencias evaluadas");
        }

        // Se crean los resultados de las competencias.
        List<ResultadoCompetencia> resultados = new ArrayList<>();
        BigDecimal sumaCompetencias = BigDecimal.ZERO;

        // Se calcula el puntaje final por competencia y se guarda en la tabla resultado_competencia.
        for (CaracteristicaPuesto caracteristica : caracteristicas) {

            Integer codigoCompetencia = caracteristica.getCompetencia().getCodigo();

            Map<Integer, BigDecimal> sumasFactores =
                    sumaPorCompetenciaYFactor.get(codigoCompetencia);

            if (sumasFactores == null || sumasFactores.isEmpty()) {
                throw new IllegalStateException(
                        "No hay respuestas para la competencia " + codigoCompetencia);
            }

            // Se calcula el puntaje final de la competencia como el promedio de los puntajes de sus factores.
            BigDecimal sumaFactores = BigDecimal.ZERO;
            for (BigDecimal sumaFactor : sumasFactores.values()) {
                sumaFactores = sumaFactores.add(
                        sumaFactor.divide(
                                BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
            }

            // Se calcula el puntaje final de la competencia como el promedio de los puntajes de sus factores.
            BigDecimal totalCompetencia = sumaFactores
                    .divide(BigDecimal.valueOf(sumasFactores.size()), 2, RoundingMode.HALF_UP);

            // Se crea el ID compuesto para el resultado de la competencia.
            ResultadoCompetenciaId id = ResultadoCompetenciaId.builder()
                    .claveCuestionario(cuestionario.getClave())
                    .codigoPuesto(cuestionario.getPuesto().getCodigoPuesto())
                    .codigoCompetencia(codigoCompetencia)
                    .build();

            // Se crea el resultado de la competencia y se agrega a la lista de resultados.
            resultados.add(ResultadoCompetencia.builder()
                    .id(id)
                    .puntajeObtenido(totalCompetencia)
                    .cuestionario(cuestionario)
                    .caracteristicaPuesto(caracteristica)
                    .build());
            // Se acumula el puntaje de la competencia para calcular el puntaje final del cuestionario.
            sumaCompetencias = sumaCompetencias.add(totalCompetencia);
        }

        // Se guardan los resultados de las competencias y se calcula el puntaje final del cuestionario.
        resultadoCompetenciaDAO.saveAll(resultados);
        cuestionario.setPuntajeFinal(
                sumaCompetencias.divide(
                        BigDecimal.valueOf(resultados.size()), 2, RoundingMode.HALF_UP));
    }


    // Convierte un bloque de preguntas a su DTO correspondiente.
    private BloquePreguntaDTO convertirBloque(BloquePregunta bloque) {
        List<PreguntaBloqueDTO> preguntas = preguntaBloqueDAO
                .findByBloquePreguntaIdBloquePreguntaOrderByOrdenEnBloqueAsc(
                        bloque.getIdBloquePregunta())
                .stream()
                .map(this::convertirPreguntaBloque)
                .toList();
        return new BloquePreguntaDTO(
                bloque.getIdBloquePregunta(),
                bloque.getNroOrden(),
                bloquePreguntaDAO.countByCuestionarioClave(
                        bloque.getCuestionario().getClave()),
                preguntas);
    }

    // Convierte una pregunta de bloque a su DTO correspondiente.
    private PreguntaBloqueDTO convertirPreguntaBloque(PreguntaBloque preguntaBloque) {
        List<OpcionRespuestaDTO> opciones = preguntaBloque.getPregunta()
                .getOpcionRespuesta()
                .getIdOpcionRespuesta() == null
                ? List.of()
                : itemOpcionDAO
                .findByOpcionRespuestaIdOpcionRespuestaAndOpcionRespuestaEliminadoFalseOrderByOrdenVisualizacionAsc(
                        preguntaBloque.getPregunta().getOpcionRespuesta().getIdOpcionRespuesta())
                .stream()
                .map(item -> new OpcionRespuestaDTO(
                        item.getIdItemOpcion(),
                        item.getDescripcion(),
                        item.getOrdenVisualizacion()))
                .toList();
        return new PreguntaBloqueDTO(
                preguntaBloque.getIdPreguntaBloque(),
                preguntaBloque.getOrdenEnBloque(),
                preguntaBloque.getPregunta().getCodigo(),
                preguntaBloque.getPregunta().getNombre(),
                preguntaBloque.getPregunta().getPregunta(),
                opciones);
    }

    // Guarda un registro de actualización de estado.
    private void guardarRegistro(RegistroActualizacionDeEstadoDTO registroDTO) {

        registroActualizacionDeEstadoDAO.save(RegistroActualizacionDeEstado.builder()
                .fecha(registroDTO.getFecha())
                .hora(registroDTO.getHora())
                .accion(registroDTO.getAccion())
                .build());
    }

    public List<Integer> buscarActivoOEnProcesoLista(List<Integer> listaIdCandidatos){
        return cuestionarioDAO.buscarActivosOenProceso(listaIdCandidatos);
    }
}
