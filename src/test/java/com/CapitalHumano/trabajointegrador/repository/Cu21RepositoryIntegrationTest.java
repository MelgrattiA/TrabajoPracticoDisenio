package com.CapitalHumano.trabajointegrador.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.CapitalHumano.trabajointegrador.config.PostgreSqlTestConfiguration;
import com.CapitalHumano.trabajointegrador.entity.Competencia;
import com.CapitalHumano.trabajointegrador.entity.Factor;
import com.CapitalHumano.trabajointegrador.entity.ItemOpcion;
import com.CapitalHumano.trabajointegrador.entity.OpcionRespuesta;
import com.CapitalHumano.trabajointegrador.entity.Pregunta;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgreSqlTestConfiguration.class)
class Cu21RepositoryIntegrationTest {

    @Autowired
    private CompetenciaDAO competenciaDAO;

    @Autowired
    private FactorDAO factorDAO;

    @Autowired
    private OpcionRespuestaDAO opcionRespuestaDAO;

    @Autowired
    private ItemOpcionDAO itemOpcionDAO;

    @Autowired
    private PreguntaDAO preguntaDAO;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional
    void obtieneCompetenciasDisponibles() {
        Competencia disponible = crearCompetencia(971001, false);
        Competencia eliminada = crearCompetencia(971002, true);

        List<Integer> codigos = competenciaDAO.findByEliminadoFalseOrderByCodigoAsc()
                .stream()
                .map(Competencia::getCodigo)
                .toList();

        assertThat(codigos)
                .contains(disponible.getCodigo())
                .doesNotContain(eliminada.getCodigo());
    }

    @Test
    @Transactional
    void obtieneFactoresActivosPorCompetenciaEnOrden() {
        Competencia competencia = crearCompetencia(971101, false);
        crearFactor(competencia, 971103, 3, false);
        crearFactor(competencia, 971102, 1, false);
        crearFactor(competencia, 971104, 2, true);
        Competencia otraCompetencia = crearCompetencia(971105, false);
        crearFactor(otraCompetencia, 971106, 1, false);

        List<Integer> codigos = factorDAO
                .findByCompetenciaCodigoAndEliminadoFalseOrderByNroOrdenAsc(
                        competencia.getCodigo())
                .stream()
                .map(Factor::getCodigo)
                .toList();

        assertThat(codigos).containsExactly(971102, 971103);
    }

    @Test
    @Transactional
    void obtieneOpcionesDeRespuestaDisponibles() {
        OpcionRespuesta disponible = crearOpcion("OPCION DISPONIBLE", false);
        OpcionRespuesta eliminada = crearOpcion("OPCION ELIMINADA", true);

        List<Long> ids = opcionRespuestaDAO
                .findByEliminadoFalseOrderByIdOpcionRespuestaAsc()
                .stream()
                .map(OpcionRespuesta::getIdOpcionRespuesta)
                .toList();

        assertThat(ids)
                .contains(disponible.getIdOpcionRespuesta())
                .doesNotContain(eliminada.getIdOpcionRespuesta());
    }

    @Test
    @Transactional
    void obtieneItemsDeOpcionEnOrdenVisualizacion() {
        OpcionRespuesta opcion = crearOpcion("OPCION ORDENADA", false);
        crearItem(opcion, 3, "TERCERO");
        crearItem(opcion, 1, "PRIMERO");
        crearItem(opcion, 2, "SEGUNDO");

        List<Integer> ordenes = itemOpcionDAO
                .findByOpcionRespuestaIdOpcionRespuestaOrderByOrdenVisualizacionAsc(
                        opcion.getIdOpcionRespuesta())
                .stream()
                .map(ItemOpcion::getOrdenVisualizacion)
                .toList();

        assertThat(ordenes).containsExactly(1, 2, 3);
    }

    @Test
    @Transactional
    void encuentraPreguntasActivasPorNombreYTextoNormalizados() {
        Factor factor = crearFactor(
                crearCompetencia(971201, false), 971202, 1, false);
        OpcionRespuesta opcion = crearOpcion("OPCION DUPLICADOS", false);
        Pregunta pregunta = crearPregunta(
                factor,
                opcion,
                "  acción Ñandú áé  ",
                "  ¿qué árbol? íóü  ",
                false);
        Factor otroFactor = crearFactor(
                crearCompetencia(971203, false), 971204, 1, false);

        assertThat(preguntaDAO.buscarPreguntaActivaPorNombreYFactor(
                "ACCIÓN ÑANDÚ ÁÉ",
                factor.getCodigo()))
                .contains(pregunta);
        assertThat(preguntaDAO.buscarPreguntaActivaPorTextoYFactor(
                "¿QUÉ ÁRBOL? ÍÓÜ",
                factor.getCodigo()))
                .contains(pregunta);
        assertThat(preguntaDAO.buscarPreguntaActivaPorNombreYFactor(
                "ACCIÓN ÑANDÚ ÁÉ",
                otroFactor.getCodigo()))
                .isEmpty();
    }

    @Test
    @Transactional
    void consultasActivasExcluyenPreguntasEliminadas() {
        Factor factor = crearFactor(
                crearCompetencia(971301, false), 971302, 1, false);
        OpcionRespuesta opcion = crearOpcion("OPCION ELIMINADA", false);
        crearPregunta(
                factor,
                opcion,
                "  nombre eliminado ñ  ",
                "  texto eliminado á  ",
                true);

        assertThat(preguntaDAO.buscarPreguntaActivaPorNombreYFactor(
                "NOMBRE ELIMINADO Ñ",
                factor.getCodigo()))
                .isEmpty();
        assertThat(preguntaDAO.buscarPreguntaActivaPorTextoYFactor(
                "TEXTO ELIMINADO Á",
                factor.getCodigo()))
                .isEmpty();
    }

    @Test
    @Transactional
    void devuelveTodasLasPreguntasEliminadasCoincidentes() {
        Factor factor = crearFactor(
                crearCompetencia(971401, false), 971402, 1, false);
        OpcionRespuesta opcion = crearOpcion("OPCION CANDIDATOS", false);
        crearPregunta(factor, opcion, "NOMBRE REPETIDO", "TEXTO UNO", true);
        crearPregunta(factor, opcion, "  nombre repetido  ", "TEXTO DOS", true);
        crearPregunta(factor, opcion, "NOMBRE TRES", "TEXTO REPETIDO", true);
        crearPregunta(factor, opcion, "NOMBRE CUATRO", "  texto repetido  ", true);

        List<Pregunta> coincidenciasPorNombre =
                preguntaDAO.buscarPreguntasEliminadasPorNombreYFactor(
                "NOMBRE REPETIDO",
                factor.getCodigo());
        assertThat(coincidenciasPorNombre).hasSize(2);
        assertThat(coincidenciasPorNombre).allSatisfy(candidata -> {
            Factor factorCandidato = candidata.getFactor();
            assertThat(Hibernate.isInitialized(factorCandidato)).isTrue();
            assertThat(Hibernate.isInitialized(factorCandidato.getCompetencia()))
                    .isTrue();
            assertThat(Hibernate.isInitialized(candidata.getOpcionRespuesta()))
                    .isTrue();
        });

        assertThat(preguntaDAO.buscarPreguntasEliminadasPorTextoYFactor(
                "TEXTO REPETIDO",
                factor.getCodigo()))
                .hasSize(2);
    }

    @Test
    void bloqueoPesimistaImpideUnaActualizacionConcurrenteHastaLiberarlo()
            throws Exception {
        Factor factor = crearFactor(
                crearCompetencia(971501, false), 971502, 1, false);
        OpcionRespuesta opcion = crearOpcion("OPCION BLOQUEO", false);
        Pregunta pregunta = crearPregunta(
                factor, opcion, "PREGUNTA BLOQUEADA", "TEXTO BLOQUEADO", false);
        Integer codigo = pregunta.getCodigo();

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);
        CountDownLatch bloqueoAdquirido = new CountDownLatch(1);
        CountDownLatch liberarBloqueo = new CountDownLatch(1);
        CountDownLatch actualizacionIniciada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> transaccionConBloqueo = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        Pregunta bloqueada = preguntaDAO
                                .buscarPorCodigoConBloqueoEscritura(codigo)
                                .orElseThrow();
                        assertThat(entityManager.getLockMode(bloqueada))
                                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
                        bloqueoAdquirido.countDown();
                        esperar(liberarBloqueo);
                    }));

            assertThat(bloqueoAdquirido.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> transaccionActualizadora = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        Pregunta aActualizar = preguntaDAO.findById(codigo)
                                .orElseThrow();
                        aActualizar.setNombre("PREGUNTA ACTUALIZADA");
                        actualizacionIniciada.countDown();
                        preguntaDAO.flush();
                    }));

            assertThat(actualizacionIniciada.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() ->
                    transaccionActualizadora.get(500, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            liberarBloqueo.countDown();
            transaccionConBloqueo.get(5, TimeUnit.SECONDS);
            transaccionActualizadora.get(5, TimeUnit.SECONDS);
        } finally {
            liberarBloqueo.countDown();
            executor.shutdownNow();
        }

        assertThat(preguntaDAO.findById(codigo))
                .get()
                .extracting(Pregunta::getNombre)
                .isEqualTo("PREGUNTA ACTUALIZADA");
    }

    private Competencia crearCompetencia(Integer codigo, boolean eliminada) {
        return competenciaDAO.saveAndFlush(Competencia.builder()
                .codigo(codigo)
                .nombre("COMPETENCIA " + codigo)
                .descripcion("PRUEBA CU21")
                .eliminado(eliminada)
                .build());
    }

    private Factor crearFactor(
            Competencia competencia,
            Integer codigo,
            Integer orden,
            boolean eliminado) {
        return factorDAO.saveAndFlush(Factor.builder()
                .codigo(codigo)
                .nombre("FACTOR " + codigo)
                .descripcion("PRUEBA CU21")
                .nroOrden(orden)
                .competencia(competencia)
                .eliminado(eliminado)
                .build());
    }

    private OpcionRespuesta crearOpcion(String nombre, boolean eliminada) {
        return opcionRespuestaDAO.saveAndFlush(OpcionRespuesta.builder()
                .nombre(nombre)
                .descripcion("PRUEBA CU21")
                .eliminado(eliminada)
                .build());
    }

    private ItemOpcion crearItem(
            OpcionRespuesta opcion,
            Integer orden,
            String descripcion) {
        return itemOpcionDAO.saveAndFlush(ItemOpcion.builder()
                .ordenVisualizacion(orden)
                .descripcion(descripcion)
                .opcionRespuesta(opcion)
                .build());
    }

    private Pregunta crearPregunta(
            Factor factor,
            OpcionRespuesta opcion,
            String nombre,
            String texto,
            boolean eliminada) {
        return preguntaDAO.saveAndFlush(Pregunta.builder()
                .nombre(nombre)
                .pregunta(texto)
                .descripcion("PRUEBA CU21")
                .factor(factor)
                .opcionRespuesta(opcion)
                .eliminado(eliminada)
                .build());
    }

    private void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Se agoto la espera del bloqueo pesimista");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Se interrumpio la prueba del bloqueo pesimista",
                    exception);
        }
    }
}
