package com.CapitalHumano.trabajointegrador.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CapitalHumano.trabajointegrador.dto.CandidatoRestauracionPreguntaDTO;
import com.CapitalHumano.trabajointegrador.dto.ConflictoPreguntaDTO;
import com.CapitalHumano.trabajointegrador.dto.CompetenciaCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.CrearPreguntaRequest;
import com.CapitalHumano.trabajointegrador.dto.FactorCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.ItemOpcionCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.OpcionRespuestaCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.PonderacionItemRequest;
import com.CapitalHumano.trabajointegrador.dto.PonderacionItemResponseDTO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaCreadaDTO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaDTO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaEliminadaRestaurableDTO;
import com.CapitalHumano.trabajointegrador.entity.Competencia;
import com.CapitalHumano.trabajointegrador.entity.Factor;
import com.CapitalHumano.trabajointegrador.entity.ItemOpcion;
import com.CapitalHumano.trabajointegrador.entity.OpcionRespuesta;
import com.CapitalHumano.trabajointegrador.entity.PonderacionItem;
import com.CapitalHumano.trabajointegrador.entity.Pregunta;
import com.CapitalHumano.trabajointegrador.exception.PreguntaConflictoException;
import com.CapitalHumano.trabajointegrador.exception.PreguntaNoEncontradaException;
import com.CapitalHumano.trabajointegrador.exception.RecursoCatalogoNoEncontradoException;
import com.CapitalHumano.trabajointegrador.repository.CompetenciaDAO;
import com.CapitalHumano.trabajointegrador.repository.FactorDAO;
import com.CapitalHumano.trabajointegrador.repository.ItemOpcionDAO;
import com.CapitalHumano.trabajointegrador.repository.OpcionRespuestaDAO;
import com.CapitalHumano.trabajointegrador.repository.PonderacionItemDAO;
import com.CapitalHumano.trabajointegrador.repository.PreguntaDAO;
import com.CapitalHumano.trabajointegrador.util.NormalizadorLiteralesPregunta;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class GestorPregunta { 
    private static final BigDecimal PUNTAJE_TOTAL = new BigDecimal("10.00");
    private static final String INDICE_NOMBRE_ACTIVO =
            "uk_pregunta_activa_factor_nombre";
    private static final String INDICE_TEXTO_ACTIVO =
            "uk_pregunta_activa_factor_texto";

    private final PreguntaDAO preguntaDAO;
    private final CompetenciaDAO competenciaDAO;
    private final FactorDAO factorDAO;
    private final OpcionRespuestaDAO opcionRespuestaDAO;
    private final ItemOpcionDAO itemOpcionDAO;
    private final PonderacionItemDAO ponderacionItemDAO;

    public List<PreguntaDTO> buscarPreguntas( String competencia, String factor, String pregunta){
        String compFiltro = (competencia == null || competencia.isBlank()) ? null : "%" + competencia.trim().toLowerCase() + "%";
        String factorFiltro = (factor == null || factor.isBlank()) ? null : "%" + factor.trim().toLowerCase() + "%";
        String nombreFiltro = (pregunta == null || pregunta.isBlank()) ? null : "%" + pregunta.trim().toLowerCase() + "%";

        if (compFiltro == null && factorFiltro == null && nombreFiltro == null) {
            throw new IllegalArgumentException("Debe ingresar al menos un criterio de búsqueda.");
        }

        List<Pregunta> preguntaLista = preguntaDAO.buscarPreguntas(compFiltro, factorFiltro, nombreFiltro);
        ArrayList<PreguntaDTO> PreguntaDTOLista = new ArrayList<PreguntaDTO>();

        for (Pregunta p: preguntaLista){
            PreguntaDTOLista.add(generarPreguntaDTO(p));
        }

        return PreguntaDTOLista;

    }

    public PreguntaDTO generarPreguntaDTO(Pregunta pregunta){
        String competencia = pregunta.getFactor().getCompetencia().getNombre();
        String factor = pregunta.getFactor().getNombre();
        String nomPregunta = pregunta.getNombre();
        
        return new PreguntaDTO(competencia, factor, nomPregunta);
    }

    public List<CompetenciaCatalogoDTO> listarCompetenciasDisponibles() {
        return competenciaDAO.findByEliminadoFalseOrderByCodigoAsc().stream()
                .map(competencia -> new CompetenciaCatalogoDTO(
                        competencia.getCodigo(),
                        competencia.getNombre()))
                .toList();
    }

    public List<FactorCatalogoDTO> listarFactoresDisponibles(
            Integer codigoCompetencia) {
        Competencia competencia = competenciaDAO.findById(codigoCompetencia)
                .filter(entidad -> !Boolean.TRUE.equals(entidad.getEliminado()))
                .orElseThrow(() -> new RecursoCatalogoNoEncontradoException(
                        "la competencia",
                        codigoCompetencia));

        return factorDAO
                .findByCompetenciaCodigoAndEliminadoFalseOrderByNroOrdenAsc(
                        competencia.getCodigo())
                .stream()
                .map(factor -> new FactorCatalogoDTO(
                        factor.getCodigo(),
                        factor.getNombre(),
                        factor.getNroOrden()))
                .toList();
    }

    public List<OpcionRespuestaCatalogoDTO> listarOpcionesRespuestaDisponibles() {
        return opcionRespuestaDAO
                .findByEliminadoFalseOrderByIdOpcionRespuestaAsc()
                .stream()
                .map(opcion -> new OpcionRespuestaCatalogoDTO(
                        opcion.getIdOpcionRespuesta(),
                        opcion.getNombre()))
                .toList();
    }

    public List<ItemOpcionCatalogoDTO> listarItemsDisponibles(
            Long idOpcionRespuesta) {
        OpcionRespuesta opcion = opcionRespuestaDAO.findById(idOpcionRespuesta)
                .filter(entidad -> !Boolean.TRUE.equals(entidad.getEliminado()))
                .orElseThrow(() -> new RecursoCatalogoNoEncontradoException(
                        "la opción de respuesta",
                        idOpcionRespuesta));

        return itemOpcionDAO
                .findByOpcionRespuestaIdOpcionRespuestaOrderByOrdenVisualizacionAsc(
                        opcion.getIdOpcionRespuesta())
                .stream()
                .map(item -> new ItemOpcionCatalogoDTO(
                        item.getIdItemOpcion(),
                        item.getDescripcion(),
                        item.getOrdenVisualizacion()))
                .toList();
    }

    @Transactional
    public PreguntaCreadaDTO crearPregunta(CrearPreguntaRequest solicitud) {
        validarIdentificadores(solicitud);

        Competencia competencia = competenciaDAO.findById(solicitud.codigoCompetencia())
                .filter(entidad -> !Boolean.TRUE.equals(entidad.getEliminado()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "La competencia no existe o no está disponible"));
        Factor factor = factorDAO.findById(solicitud.codigoFactor())
                .filter(entidad -> !Boolean.TRUE.equals(entidad.getEliminado()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "El factor no existe o no está disponible"));
        OpcionRespuesta opcion = opcionRespuestaDAO
                .findById(solicitud.idOpcionRespuesta())
                .filter(entidad -> !Boolean.TRUE.equals(entidad.getEliminado()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "La opción de respuesta no existe o no está disponible"));

        String nombre = NormalizadorLiteralesPregunta.normalizarObligatorio(
                solicitud.nombre());
        String texto = NormalizadorLiteralesPregunta.normalizarObligatorio(
                solicitud.pregunta());
        String descripcion = NormalizadorLiteralesPregunta.normalizarDescripcion(
                solicitud.descripcion());
        validarLiterales(nombre, texto, descripcion);

        if (factor.getCompetencia() == null
                || !Objects.equals(
                        factor.getCompetencia().getCodigo(),
                        competencia.getCodigo())) {
            throw new IllegalArgumentException(
                    "El factor seleccionado no pertenece a la competencia indicada");
        }

        List<ItemOpcion> itemsDeOpcion = itemOpcionDAO
                .findByOpcionRespuestaIdOpcionRespuestaOrderByOrdenVisualizacionAsc(
                        opcion.getIdOpcionRespuesta());
        if (itemsDeOpcion.isEmpty()) {
            throw new IllegalArgumentException(
                    "La opción de respuesta no tiene ítems configurados");
        }

        Map<Long, ItemOpcion> itemsPorId = new LinkedHashMap<>();
        for (ItemOpcion item : itemsDeOpcion) {
            itemsPorId.put(item.getIdItemOpcion(), item);
        }
        Map<Long, BigDecimal> ponderaciones = validarPonderaciones(
                solicitud.ponderaciones(),
                itemsPorId);

        boolean nombreDuplicado = preguntaDAO.buscarPreguntaActivaPorNombreYFactor(
                nombre,
                factor.getCodigo()).isPresent();
        boolean textoDuplicado = preguntaDAO.buscarPreguntaActivaPorTextoYFactor(
                texto,
                factor.getCodigo()).isPresent();
        if (nombreDuplicado || textoDuplicado) {
            throw crearConflictoActivo(nombreDuplicado, textoDuplicado);
        }

        List<Pregunta> eliminadasPorNombre =
                preguntaDAO.buscarPreguntasEliminadasPorNombreYFactor(
                        nombre,
                        factor.getCodigo());
        List<Pregunta> eliminadasPorTexto =
                preguntaDAO.buscarPreguntasEliminadasPorTextoYFactor(
                        texto,
                        factor.getCodigo());
        if (!eliminadasPorNombre.isEmpty() || !eliminadasPorTexto.isEmpty()) {
            throw crearConflictoRestauracion(
                    eliminadasPorNombre,
                    eliminadasPorTexto);
        }

        Pregunta nuevaPregunta = Pregunta.builder()
                .nombre(nombre)
                .pregunta(texto)
                .descripcion(descripcion)
                .factor(factor)
                .opcionRespuesta(opcion)
                .eliminado(false)
                .build();

        Pregunta preguntaPersistida;
        try {
            preguntaPersistida = preguntaDAO.saveAndFlush(nuevaPregunta);
        } catch (DataIntegrityViolationException excepcion) {
            String nombreRestriccion = obtenerNombreRestriccion(excepcion);
            if (INDICE_NOMBRE_ACTIVO.equals(nombreRestriccion)) {
                throw crearConflictoActivo(true, false);
            }
            if (INDICE_TEXTO_ACTIVO.equals(nombreRestriccion)) {
                throw crearConflictoActivo(false, true);
            }
            throw excepcion;
        }

        List<PonderacionItem> ponderacionesPersistidas = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entrada : ponderaciones.entrySet()) {
            ponderacionesPersistidas.add(PonderacionItem.builder()
                    .ponderacion(entrada.getValue())
                    .pregunta(preguntaPersistida)
                    .itemOpcion(itemsPorId.get(entrada.getKey()))
                    .build());
        }
        ponderacionItemDAO.saveAllAndFlush(ponderacionesPersistidas);

        List<PonderacionItemResponseDTO> ponderacionesRespuesta =
                ponderaciones.entrySet().stream()
                        .map(entrada -> {
                            ItemOpcion item = itemsPorId.get(entrada.getKey());
                            return new PonderacionItemResponseDTO(
                                    item.getIdItemOpcion(),
                                    item.getDescripcion(),
                                    item.getOrdenVisualizacion(),
                                    entrada.getValue());
                        })
                        .sorted(Comparator.comparing(
                                PonderacionItemResponseDTO::ordenVisualizacion))
                        .toList();

        return new PreguntaCreadaDTO(
                preguntaPersistida.getCodigo(),
                competencia.getCodigo(),
                factor.getCodigo(),
                preguntaPersistida.getNombre(),
                preguntaPersistida.getPregunta(),
                preguntaPersistida.getDescripcion(),
                opcion.getIdOpcionRespuesta(),
                ponderacionesRespuesta);
    }

    @Transactional
    public CandidatoRestauracionPreguntaDTO restaurarPregunta(
            Integer codigoPregunta) {
        if (codigoPregunta == null || codigoPregunta <= 0) {
            throw new IllegalArgumentException(
                    "Debe indicar un código de pregunta válido");
        }

        Pregunta pregunta = preguntaDAO.buscarPorCodigoConBloqueoEscritura(
                        codigoPregunta)
                .orElseThrow(() -> new PreguntaNoEncontradaException(
                        codigoPregunta));
        if (!Boolean.TRUE.equals(pregunta.getEliminado())) {
            throw new PreguntaConflictoException(new ConflictoPreguntaDTO(
                    "PREGUNTA_YA_RESTAURADA",
                    "ESTADO",
                    "La pregunta ya fue restaurada",
                    null));
        }

        String nombreNormalizado =
                NormalizadorLiteralesPregunta.normalizarObligatorio(
                        pregunta.getNombre());
        String textoNormalizado =
                NormalizadorLiteralesPregunta.normalizarObligatorio(
                        pregunta.getPregunta());
        Integer codigoFactor = pregunta.getFactor().getCodigo();

        boolean nombreDuplicado =
                preguntaDAO.buscarPreguntaActivaPorNombreYFactor(
                        nombreNormalizado,
                        codigoFactor).isPresent();
        boolean textoDuplicado =
                preguntaDAO.buscarPreguntaActivaPorTextoYFactor(
                        textoNormalizado,
                        codigoFactor).isPresent();
        if (nombreDuplicado || textoDuplicado) {
            throw crearConflictoActivo(nombreDuplicado, textoDuplicado);
        }

        pregunta.setEliminado(false);
        try {
            preguntaDAO.saveAndFlush(pregunta);
        } catch (DataIntegrityViolationException excepcion) {
            String nombreRestriccion = obtenerNombreRestriccion(excepcion);
            if (INDICE_NOMBRE_ACTIVO.equals(nombreRestriccion)) {
                throw crearConflictoActivo(true, false);
            }
            if (INDICE_TEXTO_ACTIVO.equals(nombreRestriccion)) {
                throw crearConflictoActivo(false, true);
            }
            throw excepcion;
        }

        List<PonderacionItem> ponderaciones =
                ponderacionItemDAO.findByPreguntaCodigoIn(
                        List.of(pregunta.getCodigo()));
        return crearCandidatoRestauracion(pregunta, ponderaciones);
    }

    private void validarIdentificadores(CrearPreguntaRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "La solicitud de creación no puede ser nula");
        }
        if (solicitud.codigoCompetencia() == null
                || solicitud.codigoCompetencia() <= 0) {
            throw new IllegalArgumentException(
                    "Debe indicar una competencia válida");
        }
        if (solicitud.codigoFactor() == null || solicitud.codigoFactor() <= 0) {
            throw new IllegalArgumentException("Debe indicar un factor válido");
        }
        if (solicitud.idOpcionRespuesta() == null
                || solicitud.idOpcionRespuesta() <= 0) {
            throw new IllegalArgumentException(
                    "Debe indicar una opción de respuesta válida");
        }
    }

    private void validarLiterales(
            String nombre,
            String texto,
            String descripcion) {
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (nombre.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre normalizado no puede exceder 100 caracteres");
        }
        if (texto.isBlank()) {
            throw new IllegalArgumentException("El texto de la pregunta es obligatorio");
        }
        if (texto.length() > 150) {
            throw new IllegalArgumentException(
                    "El texto normalizado no puede exceder 150 caracteres");
        }
        if (descripcion != null && descripcion.length() > 500) {
            throw new IllegalArgumentException(
                    "La descripción normalizada no puede exceder 500 caracteres");
        }
    }

    private Map<Long, BigDecimal> validarPonderaciones(
            List<PonderacionItemRequest> recibidas,
            Map<Long, ItemOpcion> itemsPorId) {
        if (recibidas == null || recibidas.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe informar las ponderaciones de todos los ítems");
        }

        Map<Long, BigDecimal> normalizadas = new LinkedHashMap<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (PonderacionItemRequest recibida : recibidas) {
            if (recibida == null) {
                throw new IllegalArgumentException(
                        "La lista no puede contener ponderaciones nulas");
            }
            Long idItem = recibida.idItemOpcion();
            if (idItem == null || idItem <= 0) {
                throw new IllegalArgumentException(
                        "Cada ponderación debe identificar un ítem válido");
            }
            if (normalizadas.containsKey(idItem)) {
                throw new IllegalArgumentException(
                        "No se puede ponderar dos veces el mismo ítem");
            }
            if (!itemsPorId.containsKey(idItem)) {
                throw new IllegalArgumentException(
                        "Uno de los ítems no pertenece a la opción seleccionada");
            }
            BigDecimal valor = recibida.ponderacion();
            if (valor == null) {
                throw new IllegalArgumentException(
                        "La ponderación de cada ítem es obligatoria");
            }
            if (valor.signum() < 0) {
                throw new IllegalArgumentException(
                        "Las ponderaciones no pueden ser negativas");
            }

            BigDecimal normalizado;
            try {
                normalizado = valor.setScale(2, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException excepcion) {
                throw new IllegalArgumentException(
                        "La ponderación no puede requerir redondeo a dos decimales",
                        excepcion);
            }
            normalizadas.put(idItem, normalizado);
            suma = suma.add(normalizado);
        }

        for (Long idItem : itemsPorId.keySet()) {
            if (!normalizadas.containsKey(idItem)) {
                throw new IllegalArgumentException(
                        "Debe incluir exactamente una ponderación por cada ítem");
            }
        }
        if (suma.compareTo(PUNTAJE_TOTAL) != 0) {
            throw new IllegalArgumentException(
                    "La suma de las ponderaciones debe ser exactamente 10.00");
        }
        return normalizadas;
    }

    private PreguntaConflictoException crearConflictoActivo(
            boolean nombreDuplicado,
            boolean textoDuplicado) {
        String campo = campoCoincidencia(nombreDuplicado, textoDuplicado);
        String mensaje = "Ya existe una pregunta activa coincidente por " + campo;
        return new PreguntaConflictoException(new ConflictoPreguntaDTO(
                "PREGUNTA_DUPLICADA_ACTIVA",
                campo,
                mensaje,
                null));
    }

    private PreguntaConflictoException crearConflictoRestauracion(
            List<Pregunta> eliminadasPorNombre,
            List<Pregunta> eliminadasPorTexto) {
        Map<Integer, Pregunta> candidatasPorCodigo = new TreeMap<>();
        for (Pregunta candidata : eliminadasPorNombre) {
            candidatasPorCodigo.put(candidata.getCodigo(), candidata);
        }
        for (Pregunta candidata : eliminadasPorTexto) {
            candidatasPorCodigo.put(candidata.getCodigo(), candidata);
        }

        List<Integer> codigos = new ArrayList<>(candidatasPorCodigo.keySet());
        Map<Integer, List<PonderacionItem>> ponderacionesPorPregunta = new HashMap<>();
        for (PonderacionItem ponderacion :
                ponderacionItemDAO.findByPreguntaCodigoIn(codigos)) {
            ponderacionesPorPregunta
                    .computeIfAbsent(
                            ponderacion.getPregunta().getCodigo(),
                            codigo -> new ArrayList<>())
                    .add(ponderacion);
        }

        List<CandidatoRestauracionPreguntaDTO> candidatas =
                candidatasPorCodigo.values().stream()
                        .map(candidata -> crearCandidatoRestauracion(
                                candidata,
                                ponderacionesPorPregunta.getOrDefault(
                                        candidata.getCodigo(),
                                        List.of())))
                        .toList();
        String campo = campoCoincidencia(
                !eliminadasPorNombre.isEmpty(),
                !eliminadasPorTexto.isEmpty());
        String mensaje =
                "La coincidencia corresponde a una pregunta eliminada y requiere restauración";
        PreguntaEliminadaRestaurableDTO restauracion =
                new PreguntaEliminadaRestaurableDTO(candidatas, campo, mensaje);

        return new PreguntaConflictoException(new ConflictoPreguntaDTO(
                "PREGUNTA_REQUIERE_RESTAURACION",
                campo,
                mensaje,
                restauracion));
    }

    private CandidatoRestauracionPreguntaDTO crearCandidatoRestauracion(
            Pregunta candidata,
            List<PonderacionItem> ponderaciones) {
        List<PonderacionItemResponseDTO> ponderacionesDTO = ponderaciones.stream()
                .map(ponderacion -> {
                    ItemOpcion item = ponderacion.getItemOpcion();
                    return new PonderacionItemResponseDTO(
                            item.getIdItemOpcion(),
                            item.getDescripcion(),
                            item.getOrdenVisualizacion(),
                            ponderacion.getPonderacion());
                })
                .sorted(Comparator.comparing(
                        PonderacionItemResponseDTO::ordenVisualizacion))
                .toList();

        return new CandidatoRestauracionPreguntaDTO(
                candidata.getCodigo(),
                candidata.getFactor().getCompetencia().getCodigo(),
                candidata.getFactor().getCodigo(),
                candidata.getNombre(),
                candidata.getPregunta(),
                candidata.getDescripcion(),
                candidata.getOpcionRespuesta().getIdOpcionRespuesta(),
                candidata.getOpcionRespuesta().getNombre(),
                ponderacionesDTO);
    }

    private String campoCoincidencia(boolean nombre, boolean texto) {
        if (nombre && texto) {
            return "NOMBRE_Y_TEXTO";
        }
        return nombre ? "NOMBRE" : "TEXTO";
    }

    private String obtenerNombreRestriccion(Throwable excepcion) {
        Throwable actual = excepcion;
        while (actual != null) {
            if (actual instanceof ConstraintViolationException violacion) {
                return violacion.getConstraintName();
            }
            actual = actual.getCause();
        }
        return null;
    }
}
