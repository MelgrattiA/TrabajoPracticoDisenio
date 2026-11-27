package com.CapitalHumano.trabajointegrador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.CapitalHumano.trabajointegrador.config.PostgreSqlTestConfiguration;
import com.CapitalHumano.trabajointegrador.dto.CandidatoRestauracionPreguntaDTO;
import com.CapitalHumano.trabajointegrador.dto.CrearPreguntaRequest;
import com.CapitalHumano.trabajointegrador.dto.PonderacionItemRequest;
import com.CapitalHumano.trabajointegrador.entity.Competencia;
import com.CapitalHumano.trabajointegrador.entity.Factor;
import com.CapitalHumano.trabajointegrador.entity.ItemOpcion;
import com.CapitalHumano.trabajointegrador.entity.OpcionRespuesta;
import com.CapitalHumano.trabajointegrador.entity.PonderacionItem;
import com.CapitalHumano.trabajointegrador.entity.Pregunta;
import com.CapitalHumano.trabajointegrador.exception.PreguntaConflictoException;
import com.CapitalHumano.trabajointegrador.repository.CompetenciaDAO;
import com.CapitalHumano.trabajointegrador.repository.FactorDAO;
import com.CapitalHumano.trabajointegrador.repository.ItemOpcionDAO;
import com.CapitalHumano.trabajointegrador.repository.OpcionRespuestaDAO;
import com.CapitalHumano.trabajointegrador.repository.PonderacionItemDAO;
import com.CapitalHumano.trabajointegrador.repository.PreguntaDAO;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgreSqlTestConfiguration.class)
class GestorPreguntaRestauracionIntegrationTest {

    private static final AtomicInteger CODIGO_TEST = new AtomicInteger(982000);

    private final ThreadLocal<Boolean> esAltaConcurrente =
            ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<Boolean> esRestauracionConcurrente =
            ThreadLocal.withInitial(() -> false);

    @Autowired
    private GestorPregunta gestorPregunta;

    @Autowired
    private CompetenciaDAO competenciaDAO;

    @Autowired
    private FactorDAO factorDAO;

    @Autowired
    private OpcionRespuestaDAO opcionRespuestaDAO;

    @Autowired
    private ItemOpcionDAO itemOpcionDAO;

    @Autowired
    private PonderacionItemDAO ponderacionItemDAO;

    @MockitoSpyBean
    private PreguntaDAO preguntaDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void restauraConservandoElRegistroYLasPonderacionesOriginales() {
        Contexto contexto = crearContexto();
        Pregunta original = crearPregunta(
                contexto,
                "  Nombre Original  ",
                "  ¿Texto original?  ",
                " Descripción Original ",
                true);
        agregarPonderaciones(
                contexto,
                original,
                new BigDecimal("4.25"),
                new BigDecimal("5.75"));

        CandidatoRestauracionPreguntaDTO restaurada =
                gestorPregunta.restaurarPregunta(original.getCodigo());

        assertThat(restaurada.codigo()).isEqualTo(original.getCodigo());
        assertThat(restaurada.codigoCompetencia())
                .isEqualTo(contexto.codigoCompetencia());
        assertThat(restaurada.codigoFactor()).isEqualTo(contexto.codigoFactor());
        assertThat(restaurada.nombre()).isEqualTo("  Nombre Original  ");
        assertThat(restaurada.pregunta()).isEqualTo("  ¿Texto original?  ");
        assertThat(restaurada.descripcion()).isEqualTo(" Descripción Original ");
        assertThat(restaurada.idOpcionRespuesta()).isEqualTo(contexto.idOpcion());
        assertThat(restaurada.nombreOpcionRespuesta())
                .isEqualTo(contexto.nombreOpcion());
        assertThat(restaurada.ponderaciones())
                .extracting(ponderacion -> ponderacion.ponderacion())
                .containsExactly(
                        new BigDecimal("4.25"),
                        new BigDecimal("5.75"));

        Boolean eliminada = jdbcTemplate.queryForObject(
                "SELECT eliminado FROM pregunta WHERE codigo = ?",
                Boolean.class,
                original.getCodigo());
        Integer cantidadPreguntas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pregunta WHERE codigo = ?",
                Integer.class,
                original.getCodigo());
        List<PonderacionItem> ponderacionesPersistidas =
                ponderacionItemDAO.findByPreguntaCodigoIn(
                        List.of(original.getCodigo()));

        assertThat(eliminada).isFalse();
        assertThat(cantidadPreguntas).isEqualTo(1);
        assertThat(ponderacionesPersistidas)
                .extracting(PonderacionItem::getPonderacion)
                .containsExactlyInAnyOrder(
                        new BigDecimal("4.25"),
                        new BigDecimal("5.75"));
        assertThat(ponderacionesPersistidas)
                .extracting(ponderacion ->
                        ponderacion.getPregunta().getCodigo())
                .containsOnly(original.getCodigo());
    }

    @Test
    void rechazaRestauracionConNombreActivoDuplicado() {
        Contexto contexto = crearContexto();
        Pregunta eliminada = crearPregunta(
                contexto, "NOMBRE ORIGINAL", "TEXTO ORIGINAL", null, true);
        crearPregunta(
                contexto, "NOMBRE ORIGINAL", "OTRO TEXTO", null, false);

        PreguntaConflictoException conflicto =
                capturarConflicto(eliminada.getCodigo());

        assertThat(conflicto.getConflicto().codigo())
                .isEqualTo("PREGUNTA_DUPLICADA_ACTIVA");
        assertThat(conflicto.getConflicto().campoCoincidencia())
                .isEqualTo("NOMBRE");
        assertQueSigueEliminada(eliminada.getCodigo());
    }

    @Test
    void rechazaRestauracionConTextoActivoDuplicado() {
        Contexto contexto = crearContexto();
        Pregunta eliminada = crearPregunta(
                contexto, "NOMBRE ORIGINAL", "TEXTO ORIGINAL", null, true);
        crearPregunta(
                contexto, "OTRO NOMBRE", "TEXTO ORIGINAL", null, false);

        PreguntaConflictoException conflicto =
                capturarConflicto(eliminada.getCodigo());

        assertThat(conflicto.getConflicto().codigo())
                .isEqualTo("PREGUNTA_DUPLICADA_ACTIVA");
        assertThat(conflicto.getConflicto().campoCoincidencia())
                .isEqualTo("TEXTO");
        assertQueSigueEliminada(eliminada.getCodigo());
    }

    @Test
    void informaConflictosSimultaneosDeNombreYTexto() {
        Contexto contexto = crearContexto();
        Pregunta eliminada = crearPregunta(
                contexto, "NOMBRE ORIGINAL", "TEXTO ORIGINAL", null, true);
        crearPregunta(
                contexto, "NOMBRE ORIGINAL", "TEXTO ORIGINAL", null, false);

        PreguntaConflictoException conflicto =
                capturarConflicto(eliminada.getCodigo());

        assertThat(conflicto.getConflicto().campoCoincidencia())
                .isEqualTo("NOMBRE_Y_TEXTO");
        assertQueSigueEliminada(eliminada.getCodigo());
    }

    @Test
    void rechazaPreguntaInexistente() {
        Throwable error = catchThrowable(
                () -> gestorPregunta.restaurarPregunta(Integer.MAX_VALUE));

        assertThat(error)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    void identificaUnaPreguntaQueYaFueRestaurada() {
        Contexto contexto = crearContexto();
        Pregunta activa = crearPregunta(
                contexto, "NOMBRE ACTIVO", "TEXTO ACTIVO", null, false);

        PreguntaConflictoException conflicto =
                capturarConflicto(activa.getCodigo());

        assertThat(conflicto.getConflicto().codigo())
                .isEqualTo("PREGUNTA_YA_RESTAURADA");
    }

    @Test
    void dosRestauracionesSimultaneasSoloRestauranUnaVez() throws Exception {
        Contexto contexto = crearContexto();
        Pregunta eliminada = crearPregunta(
                contexto, "NOMBRE CONCURRENTE", "TEXTO CONCURRENTE", null, true);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch listas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        try {
            Future<Object> primera = executor.submit(() -> {
                listas.countDown();
                iniciar.await();
                return intentarRestaurar(eliminada.getCodigo());
            });
            Future<Object> segunda = executor.submit(() -> {
                listas.countDown();
                iniciar.await();
                return intentarRestaurar(eliminada.getCodigo());
            });

            assertThat(listas.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            List<Object> resultados = List.of(
                    primera.get(20, TimeUnit.SECONDS),
                    segunda.get(20, TimeUnit.SECONDS));

            assertThat(resultados.stream()
                    .filter(CandidatoRestauracionPreguntaDTO.class::isInstance)
                    .count())
                    .isEqualTo(1);
            assertThat(resultados.stream()
                    .filter(PreguntaConflictoException.class::isInstance)
                    .map(PreguntaConflictoException.class::cast)
                    .map(conflicto -> conflicto.getConflicto().codigo()))
                    .containsExactly("PREGUNTA_YA_RESTAURADA");
            assertQueEstaActiva(eliminada.getCodigo());
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void altaConcurrenteConRestauracionSeRechazaPorIndiceUnico()
            throws Exception {
        Contexto contexto = crearContexto();
        Pregunta eliminada = crearPregunta(
                contexto, "NOMBRE CARRERA", "TEXTO CARRERA", null, true);
        agregarPonderaciones(contexto, eliminada, new BigDecimal("10.00"));

        AtomicInteger consultasAlta = new AtomicInteger();
        CountDownLatch duplicadosComprobados = new CountDownLatch(1);
        CountDownLatch continuarAlta = new CountDownLatch(1);
        doAnswer(invocacion -> {
            if (esAltaConcurrente.get() && consultasAlta.incrementAndGet() == 2) {
                duplicadosComprobados.countDown();
                esperar(continuarAlta, "La alta concurrente no pudo continuar");
            }
            return Optional.empty();
        }).when(preguntaDAO)
                .buscarPreguntaActivaPorNombreYFactor(anyString(), anyInt());
        doAnswer(invocacion -> {
            if (esAltaConcurrente.get() && consultasAlta.incrementAndGet() == 2) {
                duplicadosComprobados.countDown();
                esperar(continuarAlta, "La alta concurrente no pudo continuar");
            }
            return Optional.empty();
        }).when(preguntaDAO)
                .buscarPreguntaActivaPorTextoYFactor(anyString(), anyInt());

        CrearPreguntaRequest solicitud = new CrearPreguntaRequest(
                contexto.codigoCompetencia(),
                contexto.codigoFactor(),
                eliminada.getNombre(),
                eliminada.getPregunta(),
                eliminada.getDescripcion(),
                contexto.idOpcion(),
                List.of(
                        new PonderacionItemRequest(
                                contexto.items().getFirst().getIdItemOpcion(),
                                new BigDecimal("5.00")),
                        new PonderacionItemRequest(
                                contexto.items().get(1).getIdItemOpcion(),
                                new BigDecimal("5.00"))));
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<Object> alta = executor.submit(() -> {
                esAltaConcurrente.set(true);
                try {
                    return intentarCrear(solicitud);
                } finally {
                    esAltaConcurrente.remove();
                }
            });

            assertThat(duplicadosComprobados.await(10, TimeUnit.SECONDS)).isTrue();
            CandidatoRestauracionPreguntaDTO restaurada =
                    gestorPregunta.restaurarPregunta(eliminada.getCodigo());
            assertThat(restaurada.codigo()).isEqualTo(eliminada.getCodigo());
            continuarAlta.countDown();

            Object resultado = alta.get(20, TimeUnit.SECONDS);
            assertThat(resultado).isInstanceOf(PreguntaConflictoException.class);
            assertThat(((PreguntaConflictoException) resultado)
                    .getConflicto().codigo())
                    .isEqualTo("PREGUNTA_DUPLICADA_ACTIVA");
            assertThat(cantidadPorNombre(contexto, eliminada.getNombre()))
                    .isEqualTo(1);
        } finally {
            continuarAlta.countDown();
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOMBRE", "TEXTO"})
    void revierteLaRestauracionSiUnaPreguntaActivaAdquiereUnLiteral(
            String campoEnConflicto) throws Exception {
        Contexto contexto = crearContexto();
        String nombreOriginal = "NOMBRE ORIGINAL " + contexto.codigoFactor();
        String textoOriginal = "TEXTO ORIGINAL " + contexto.codigoFactor();
        Pregunta eliminada = crearPregunta(
                contexto, nombreOriginal, textoOriginal, "DESCRIPCIÓN ORIGINAL", true);
        agregarPonderaciones(contexto, eliminada, new BigDecimal("10.00"));

        AtomicInteger consultasRestauracion = new AtomicInteger();
        CountDownLatch duplicadosComprobados = new CountDownLatch(1);
        CountDownLatch continuarRestauracion = new CountDownLatch(1);
        doAnswer(invocacion -> {
            if (esRestauracionConcurrente.get()
                    && consultasRestauracion.incrementAndGet() == 2) {
                duplicadosComprobados.countDown();
                esperar(
                        continuarRestauracion,
                        "La restauración concurrente no pudo continuar");
            }
            return Optional.empty();
        }).when(preguntaDAO)
                .buscarPreguntaActivaPorNombreYFactor(anyString(), anyInt());
        doAnswer(invocacion -> {
            if (esRestauracionConcurrente.get()
                    && consultasRestauracion.incrementAndGet() == 2) {
                duplicadosComprobados.countDown();
                esperar(
                        continuarRestauracion,
                        "La restauración concurrente no pudo continuar");
            }
            return Optional.empty();
        }).when(preguntaDAO)
                .buscarPreguntaActivaPorTextoYFactor(anyString(), anyInt());

        String nombreCompetidor = campoEnConflicto.equals("NOMBRE")
                ? nombreOriginal
                : "NOMBRE COMPETIDOR " + contexto.codigoFactor();
        String textoCompetidor = campoEnConflicto.equals("TEXTO")
                ? textoOriginal
                : "TEXTO COMPETIDOR " + contexto.codigoFactor();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<Object> restauracion = executor.submit(() -> {
                esRestauracionConcurrente.set(true);
                try {
                    return intentarRestaurar(eliminada.getCodigo());
                } finally {
                    esRestauracionConcurrente.remove();
                }
            });

            assertThat(duplicadosComprobados.await(10, TimeUnit.SECONDS)).isTrue();
            crearPregunta(
                    contexto,
                    nombreCompetidor,
                    textoCompetidor,
                    "DESCRIPCIÓN COMPETIDOR",
                    false);
            continuarRestauracion.countDown();

            Object resultado = restauracion.get(20, TimeUnit.SECONDS);
            assertThat(resultado).isInstanceOf(PreguntaConflictoException.class);
            PreguntaConflictoException conflicto =
                    (PreguntaConflictoException) resultado;
            assertThat(conflicto.getConflicto().codigo())
                    .isEqualTo("PREGUNTA_DUPLICADA_ACTIVA");
            assertThat(conflicto.getConflicto().campoCoincidencia())
                    .isEqualTo(campoEnConflicto);
            assertQueSigueEliminadaConDatosOriginales(
                    eliminada,
                    "DESCRIPCIÓN ORIGINAL");
        } finally {
            continuarRestauracion.countDown();
            executor.shutdownNow();
        }
    }

    private Contexto crearContexto() {
        int codigo = CODIGO_TEST.getAndAdd(10);
        Competencia competencia = competenciaDAO.saveAndFlush(
                Competencia.builder()
                        .codigo(codigo)
                        .nombre("COMPETENCIA RESTAURACIÓN " + codigo)
                        .descripcion("PRUEBA CU21")
                        .eliminado(false)
                        .build());
        Factor factor = factorDAO.saveAndFlush(
                Factor.builder()
                        .codigo(codigo + 1)
                        .nombre("FACTOR RESTAURACIÓN " + codigo)
                        .descripcion("PRUEBA CU21")
                        .nroOrden(1)
                        .competencia(competencia)
                        .eliminado(false)
                        .build());
        OpcionRespuesta opcion = opcionRespuestaDAO.saveAndFlush(
                OpcionRespuesta.builder()
                        .nombre("OPCIÓN RESTAURACIÓN " + codigo)
                        .descripcion("PRUEBA CU21")
                        .eliminado(false)
                        .build());
        List<ItemOpcion> items = new ArrayList<>();
        for (int orden = 1; orden <= 2; orden++) {
            items.add(itemOpcionDAO.saveAndFlush(
                    ItemOpcion.builder()
                            .ordenVisualizacion(orden)
                            .descripcion("ÍTEM " + codigo + " " + orden)
                            .opcionRespuesta(opcion)
                            .build()));
        }
        return new Contexto(
                competencia.getCodigo(),
                factor.getCodigo(),
                opcion.getIdOpcionRespuesta(),
                opcion.getNombre(),
                items);
    }

    private Pregunta crearPregunta(
            Contexto contexto,
            String nombre,
            String texto,
            String descripcion,
            boolean eliminada) {
        Factor factor = factorDAO.findById(contexto.codigoFactor()).orElseThrow();
        OpcionRespuesta opcion =
                opcionRespuestaDAO.findById(contexto.idOpcion()).orElseThrow();
        return preguntaDAO.saveAndFlush(Pregunta.builder()
                .nombre(nombre)
                .pregunta(texto)
                .descripcion(descripcion)
                .factor(factor)
                .opcionRespuesta(opcion)
                .eliminado(eliminada)
                .build());
    }

    private void agregarPonderaciones(
            Contexto contexto,
            Pregunta pregunta,
            BigDecimal... valores) {
        List<PonderacionItem> ponderaciones = new ArrayList<>();
        for (int indice = 0; indice < valores.length; indice++) {
            ponderaciones.add(PonderacionItem.builder()
                    .ponderacion(valores[indice])
                    .pregunta(pregunta)
                    .itemOpcion(contexto.items().get(indice))
                    .build());
        }
        ponderacionItemDAO.saveAllAndFlush(ponderaciones);
    }

    private PreguntaConflictoException capturarConflicto(Integer codigo) {
        Throwable error = catchThrowable(
                () -> gestorPregunta.restaurarPregunta(codigo));
        assertThat(error).isInstanceOf(PreguntaConflictoException.class);
        return (PreguntaConflictoException) error;
    }

    private Object intentarRestaurar(Integer codigo) {
        try {
            return gestorPregunta.restaurarPregunta(codigo);
        } catch (RuntimeException excepcion) {
            return excepcion;
        }
    }

    private Object intentarCrear(CrearPreguntaRequest solicitud) {
        try {
            return gestorPregunta.crearPregunta(solicitud);
        } catch (RuntimeException excepcion) {
            return excepcion;
        }
    }

    private void assertQueSigueEliminada(Integer codigo) {
        Boolean eliminada = jdbcTemplate.queryForObject(
                "SELECT eliminado FROM pregunta WHERE codigo = ?",
                Boolean.class,
                codigo);
        assertThat(eliminada).isTrue();
    }

    private void assertQueEstaActiva(Integer codigo) {
        Boolean eliminada = jdbcTemplate.queryForObject(
                "SELECT eliminado FROM pregunta WHERE codigo = ?",
                Boolean.class,
                codigo);
        assertThat(eliminada).isFalse();
    }

    private void assertQueSigueEliminadaConDatosOriginales(
            Pregunta original,
            String descripcionOriginal) {
        Pregunta persistida = preguntaDAO.findById(original.getCodigo()).orElseThrow();
        assertThat(persistida.getEliminado()).isTrue();
        assertThat(persistida.getNombre()).isEqualTo(original.getNombre());
        assertThat(persistida.getPregunta()).isEqualTo(original.getPregunta());
        assertThat(persistida.getDescripcion()).isEqualTo(descripcionOriginal);
        assertThat(persistida.getFactor().getCodigo())
                .isEqualTo(original.getFactor().getCodigo());
        assertThat(persistida.getOpcionRespuesta().getIdOpcionRespuesta())
                .isEqualTo(original.getOpcionRespuesta().getIdOpcionRespuesta());

        List<PonderacionItem> ponderaciones =
                ponderacionItemDAO.findByPreguntaCodigoIn(
                        List.of(original.getCodigo()));
        assertThat(ponderaciones)
                .extracting(PonderacionItem::getPonderacion)
                .containsExactly(new BigDecimal("10.00"));
    }

    private int cantidadPorNombre(Contexto contexto, String nombre) {
        return jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM pregunta
                WHERE codigo_factor = ?
                  AND upper(btrim(nombre)) = upper(btrim(?))
                """,
                Integer.class,
                contexto.codigoFactor(),
                nombre);
    }

    private void esperar(CountDownLatch latch, String mensaje) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(mensaje);
            }
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(mensaje, excepcion);
        }
    }

    private record Contexto(
            Integer codigoCompetencia,
            Integer codigoFactor,
            Long idOpcion,
            String nombreOpcion,
            List<ItemOpcion> items) {
    }
}
