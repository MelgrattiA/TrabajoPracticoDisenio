package com.CapitalHumano.trabajointegrador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.EntityManager;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.CapitalHumano.trabajointegrador.config.PostgreSqlTestConfiguration;
import com.CapitalHumano.trabajointegrador.dto.CrearPreguntaRequest;
import com.CapitalHumano.trabajointegrador.dto.PonderacionItemRequest;
import com.CapitalHumano.trabajointegrador.dto.PreguntaCreadaDTO;
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
class GestorPreguntaAltaIntegrationTest {

    private static final AtomicInteger CODIGO_TEST = new AtomicInteger(981000);

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

    @MockitoSpyBean
    private PreguntaDAO preguntaDAO;

    @MockitoSpyBean
    private PonderacionItemDAO ponderacionItemDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void creaPreguntaConIdentityYPonderacionesPersistidas() {
        Contexto contexto = crearContexto(2);
        CrearPreguntaRequest solicitud = solicitud(
                contexto,
                "  pregunta válida  ",
                "  ¿qué opción elegís?  ",
                null,
                valores(contexto, "4.75", "5.25"));

        PreguntaCreadaDTO creada = gestorPregunta.crearPregunta(solicitud);

        assertThat(creada.codigo()).isNotNull();
        assertThat(creada.nombre()).isEqualTo("PREGUNTA VÁLIDA");
        assertThat(creada.pregunta()).isEqualTo("¿QUÉ OPCIÓN ELEGÍS?");
        assertThat(creada.descripcion()).isNull();
        assertThat(creada.ponderaciones())
                .extracting(ponderacion -> ponderacion.ponderacion())
                .containsExactly(new BigDecimal("4.75"), new BigDecimal("5.25"));

        Pregunta persistida = preguntaDAO.findById(creada.codigo()).orElseThrow();
        List<PonderacionItem> ponderaciones =
                ponderacionItemDAO.findByPreguntaCodigoIn(
                        List.of(creada.codigo()));
        assertThat(persistida.getEliminado()).isFalse();
        assertThat(ponderaciones).hasSize(2);
        assertThat(ponderaciones)
                .allSatisfy(ponderacion ->
                        assertThat(ponderacion.getPonderacion().scale()).isEqualTo(2));
        assertThat(ponderaciones.stream()
                .map(PonderacionItem::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(new BigDecimal("10.00")))
                .isZero();
    }

    @Test
    void rechazaCompetenciaInexistente() {
        Contexto contexto = crearContexto(1);
        CrearPreguntaRequest solicitud = new CrearPreguntaRequest(
                Integer.MAX_VALUE,
                contexto.codigoFactor(),
                "NOMBRE",
                "TEXTO",
                null,
                contexto.idOpcion(),
                valores(contexto, "10.00"));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("competencia");
    }

    @Test
    void rechazaFactorInexistente() {
        Contexto contexto = crearContexto(1);
        CrearPreguntaRequest solicitud = new CrearPreguntaRequest(
                contexto.codigoCompetencia(),
                Integer.MAX_VALUE,
                "NOMBRE",
                "TEXTO",
                null,
                contexto.idOpcion(),
                valores(contexto, "10.00"));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("factor");
    }

    @Test
    void rechazaFactorDeOtraCompetencia() {
        Contexto contexto = crearContexto(1);
        Competencia otraCompetencia = crearCompetencia(siguienteCodigo(), false);
        Factor otroFactor = crearFactor(
                otraCompetencia,
                siguienteCodigo(),
                false);
        CrearPreguntaRequest solicitud = new CrearPreguntaRequest(
                contexto.codigoCompetencia(),
                otroFactor.getCodigo(),
                "NOMBRE",
                "TEXTO",
                null,
                contexto.idOpcion(),
                valores(contexto, "10.00"));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void rechazaOpcionInexistente() {
        Contexto contexto = crearContexto(1);
        CrearPreguntaRequest solicitud = new CrearPreguntaRequest(
                contexto.codigoCompetencia(),
                contexto.codigoFactor(),
                "NOMBRE",
                "TEXTO",
                null,
                Long.MAX_VALUE,
                valores(contexto, "10.00"));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opción");
    }

    @Test
    void rechazaOpcionEliminada() {
        Contexto contexto = crearContexto(1);
        OpcionRespuesta opcion = opcionRespuestaDAO.findById(contexto.idOpcion())
                .orElseThrow();
        opcion.setEliminado(true);
        opcionRespuestaDAO.saveAndFlush(opcion);
        CrearPreguntaRequest solicitud = solicitud(
                contexto,
                "NOMBRE",
                "TEXTO",
                null,
                valores(contexto, "10.00"));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disponible");
    }

    @Test
    void rechazaItemDeOtraOpcion() {
        Contexto contexto = crearContexto(1);
        Contexto otraOpcion = crearContexto(1);
        CrearPreguntaRequest solicitud = solicitud(
                contexto,
                "NOMBRE",
                "TEXTO",
                null,
                List.of(new PonderacionItemRequest(
                        otraOpcion.items().getFirst().getIdItemOpcion(),
                        new BigDecimal("10.00"))));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void rechazaItemsFaltantes() {
        Contexto contexto = crearContexto(2);
        CrearPreguntaRequest solicitud = solicitud(
                contexto,
                "NOMBRE",
                "TEXTO",
                null,
                List.of(new PonderacionItemRequest(
                        contexto.items().getFirst().getIdItemOpcion(),
                        new BigDecimal("10.00"))));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cada ítem");
    }

    @Test
    void rechazaIdsDeItemsDuplicados() {
        Contexto contexto = crearContexto(1);
        Long idItem = contexto.items().getFirst().getIdItemOpcion();
        CrearPreguntaRequest solicitud = solicitud(
                contexto,
                "NOMBRE",
                "TEXTO",
                null,
                List.of(
                        new PonderacionItemRequest(idItem, new BigDecimal("5.00")),
                        new PonderacionItemRequest(idItem, new BigDecimal("5.00"))));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dos veces");
    }

    @Test
    void rechazaColeccionOPonderacionesNulas() {
        Contexto contexto = crearContexto(1);
        CrearPreguntaRequest sinColeccion = solicitud(
                contexto, "NOMBRE A", "TEXTO A", null, null);
        CrearPreguntaRequest conElementoNulo = solicitud(
                contexto,
                "NOMBRE B",
                "TEXTO B",
                null,
                Arrays.asList((PonderacionItemRequest) null));
        CrearPreguntaRequest conValorNulo = solicitud(
                contexto,
                "NOMBRE C",
                "TEXTO C",
                null,
                List.of(new PonderacionItemRequest(
                        contexto.items().getFirst().getIdItemOpcion(),
                        null)));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(sinColeccion))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gestorPregunta.crearPregunta(conElementoNulo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulas");
        assertThatThrownBy(() -> gestorPregunta.crearPregunta(conValorNulo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatoria");
    }

    @Test
    void rechazaPonderacionesNegativas() {
        Contexto contexto = crearContexto(1);
        CrearPreguntaRequest solicitud = solicitud(
                contexto,
                "NOMBRE",
                "TEXTO",
                null,
                valores(contexto, "-10.00"));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativas");
    }

    @Test
    void rechazaSumaMenorOMayorA10() {
        Contexto menor = crearContexto(2);
        Contexto mayor = crearContexto(2);

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud(
                menor,
                "NOMBRE MENOR",
                "TEXTO MENOR",
                null,
                valores(menor, "4.99", "5.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactamente 10.00");
        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud(
                mayor,
                "NOMBRE MAYOR",
                "TEXTO MAYOR",
                null,
                valores(mayor, "5.01", "5.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactamente 10.00");
    }

    @Test
    void normalizaEscalasSinPerdidaYRechazaRedondeos() {
        Contexto contexto = crearContexto(1);
        List<String> valoresValidos = List.of("10", "10.0", "10.00", "10.000");

        for (int indice = 0; indice < valoresValidos.size(); indice++) {
            PreguntaCreadaDTO creada = gestorPregunta.crearPregunta(solicitud(
                    contexto,
                    "NOMBRE ESCALA " + indice,
                    "TEXTO ESCALA " + indice,
                    null,
                    valores(contexto, valoresValidos.get(indice))));

            PonderacionItem persistida = ponderacionItemDAO
                    .findByPreguntaCodigoIn(List.of(creada.codigo()))
                    .getFirst();
            assertThat(persistida.getPonderacion())
                    .isEqualByComparingTo("10.00");
            assertThat(persistida.getPonderacion().scale()).isEqualTo(2);
        }

        CrearPreguntaRequest requiereRedondeo = solicitud(
                contexto,
                "NOMBRE REDONDEO",
                "TEXTO REDONDEO",
                null,
                valores(contexto, "10.001"));
        assertThatThrownBy(() -> gestorPregunta.crearPregunta(requiereRedondeo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("redondeo");
    }

    @Test
    void sumaPonderacionesConEscalasDiferentesPorValorNumerico() {
        Contexto contexto = crearContexto(2);
        PreguntaCreadaDTO creada = gestorPregunta.crearPregunta(solicitud(
                contexto,
                "NOMBRE ESCALAS DIFERENTES",
                "TEXTO ESCALAS DIFERENTES",
                null,
                valores(contexto, "4.0", "6.000")));

        assertThat(creada.ponderaciones())
                .extracting(ponderacion -> ponderacion.ponderacion().scale())
                .containsExactly(2, 2);
    }

    @Test
    void detectaDuplicadoActivoPorNombreYPorTexto() {
        Contexto contexto = crearContexto(1);
        crearPreguntaExistente(
                contexto, "NOMBRE EXISTENTE", "TEXTO EXISTENTE", false);

        PreguntaConflictoException porNombre = capturarConflicto(solicitud(
                contexto, "nombre existente", "TEXTO NUEVO", null,
                valores(contexto, "10.00")));
        PreguntaConflictoException porTexto = capturarConflicto(solicitud(
                contexto, "NOMBRE NUEVO", "texto existente", null,
                valores(contexto, "10.00")));

        assertThat(porNombre.getConflicto().codigo())
                .isEqualTo("PREGUNTA_DUPLICADA_ACTIVA");
        assertThat(porNombre.getConflicto().campoCoincidencia())
                .isEqualTo("NOMBRE");
        assertThat(porTexto.getConflicto().campoCoincidencia())
                .isEqualTo("TEXTO");
    }

    @Test
    void informaConflictosActivosPorAmbosCampos() {
        Contexto contexto = crearContexto(1);
        crearPreguntaExistente(
                contexto, "NOMBRE DUPLICADO UNO", "TEXTO DUPLICADO UNO", false);
        crearPreguntaExistente(
                contexto, "NOMBRE DUPLICADO DOS", "TEXTO DUPLICADO DOS", false);

        PreguntaConflictoException conflicto = capturarConflicto(solicitud(
                contexto,
                "NOMBRE DUPLICADO UNO",
                "TEXTO DUPLICADO DOS",
                null,
                valores(contexto, "10.00")));

        assertThat(conflicto.getConflicto().campoCoincidencia())
                .isEqualTo("NOMBRE_Y_TEXTO");
    }

    @Test
    void informaCoincidenciaEliminadaPorNombreConDatosOriginales() {
        Contexto contexto = crearContexto(1);
        Pregunta eliminada = crearPreguntaExistente(
                contexto, "NOMBRE PARA RESTAURAR", "TEXTO ORIGINAL", true);
        CrearPreguntaRequest solicitud = solicitud(
                contexto,
                "nombre para restaurar",
                "TEXTO NUEVO",
                null,
                valores(contexto, "10.00"));

        PreguntaConflictoException conflicto = capturarConflicto(solicitud);

        assertThat(conflicto.getConflicto().codigo())
                .isEqualTo("PREGUNTA_REQUIERE_RESTAURACION");
        assertThat(conflicto.getConflicto().restauracion().candidatos())
                .singleElement()
                .satisfies(candidato -> {
                    assertThat(candidato.codigo()).isEqualTo(eliminada.getCodigo());
                    assertThat(candidato.nombre()).isEqualTo("NOMBRE PARA RESTAURAR");
                    assertThat(candidato.ponderaciones()).hasSize(1);
                    assertThat(candidato.ponderaciones().getFirst().ponderacion())
                            .isEqualByComparingTo("10.00");
                });
        assertThat(preguntaDAO.buscarPreguntasEliminadasPorNombreYFactor(
                "NOMBRE PARA RESTAURAR",
                contexto.codigoFactor()))
                .hasSize(1);
    }

    @Test
    void informaCoincidenciaEliminadaPorTexto() {
        Contexto contexto = crearContexto(1);
        crearPreguntaExistente(
                contexto, "NOMBRE ORIGINAL", "TEXTO PARA RESTAURAR", true);

        PreguntaConflictoException conflicto = capturarConflicto(solicitud(
                contexto,
                "NOMBRE NUEVO",
                "texto para restaurar",
                null,
                valores(contexto, "10.00")));

        assertThat(conflicto.getConflicto().campoCoincidencia())
                .isEqualTo("TEXTO");
        assertThat(conflicto.getConflicto().restauracion().candidatos())
                .hasSize(1);
    }

    @Test
    void devuelveVariosCandidatosEliminadosSinElegirUno() {
        Contexto contexto = crearContexto(1);
        crearPreguntaExistente(
                contexto, "NOMBRE HISTÓRICO", "TEXTO HISTORICO UNO", true);
        crearPreguntaExistente(
                contexto, "  nombre histórico  ", "TEXTO HISTORICO DOS", true);

        PreguntaConflictoException conflicto = capturarConflicto(solicitud(
                contexto,
                "NOMBRE HISTÓRICO",
                "TEXTO NUEVO",
                null,
                valores(contexto, "10.00")));

        assertThat(conflicto.getConflicto().restauracion().candidatos())
                .hasSize(2)
                .extracting(candidato -> candidato.codigo())
                .doesNotHaveDuplicates();
    }

    @Test
    void priorizaConflictoActivoSobreCoincidenciaEliminada() {
        Contexto contexto = crearContexto(1);
        crearPreguntaExistente(
                contexto, "NOMBRE ACTIVO", "TEXTO ACTIVO", false);
        crearPreguntaExistente(
                contexto, "NOMBRE ELIMINADO", "TEXTO ELIMINADO", true);

        PreguntaConflictoException conflicto = capturarConflicto(solicitud(
                contexto,
                "NOMBRE ACTIVO",
                "TEXTO ELIMINADO",
                null,
                valores(contexto, "10.00")));

        assertThat(conflicto.getConflicto().codigo())
                .isEqualTo("PREGUNTA_DUPLICADA_ACTIVA");
        assertThat(conflicto.getConflicto().restauracion()).isNull();
    }

    @Test
    void traduceSoloViolacionesDeLosIndicesUnicosParciales() {
        Contexto contexto = crearContexto(1);
        CrearPreguntaRequest solicitud = solicitud(
                contexto, "NOMBRE CARRERA", "TEXTO CARRERA", null,
                valores(contexto, "10.00"));
        ConstraintViolationException violacionNombre =
                new ConstraintViolationException(
                        "duplicado concurrente",
                        null,
                        "uk_pregunta_activa_factor_nombre");
        doThrow(new DataIntegrityViolationException(
                "duplicado concurrente",
                violacionNombre))
                .when(preguntaDAO)
                .saveAndFlush(any(Pregunta.class));

        PreguntaConflictoException conflicto = capturarConflicto(solicitud);

        assertThat(conflicto.getConflicto().codigo())
                .isEqualTo("PREGUNTA_DUPLICADA_ACTIVA");
        assertThat(conflicto.getConflicto().campoCoincidencia())
                .isEqualTo("NOMBRE");
    }

    @Test
    void carreraDeAltasDuplicadasProduceUnSoloRegistroYConflictoDeNegocio()
            throws Exception {
        Contexto contexto = crearContexto(1);
        CrearPreguntaRequest solicitud = solicitud(
                contexto,
                "PREGUNTA CONCURRENTE " + contexto.codigoFactor(),
                "TEXTO CONCURRENTE " + contexto.codigoFactor(),
                null,
                valores(contexto, "10.00"));
        CountDownLatch ambasConsultasDeNombre = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        doAnswer(invocacion -> {
            ambasConsultasDeNombre.countDown();
            if (!ambasConsultasDeNombre.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Las solicitudes no alcanzaron la comprobación de duplicados");
            }
            return Optional.empty();
        }).when(preguntaDAO)
                .buscarPreguntaActivaPorNombreYFactor(
                        eq(solicitud.nombre()),
                        eq(contexto.codigoFactor()));

        try {
            Future<Object> primera = executor.submit(
                    () -> intentarCrear(solicitud));
            Future<Object> segunda = executor.submit(
                    () -> intentarCrear(solicitud));
            List<Object> resultados = List.of(
                    primera.get(20, TimeUnit.SECONDS),
                    segunda.get(20, TimeUnit.SECONDS));

            assertThat(resultados.stream()
                    .filter(PreguntaCreadaDTO.class::isInstance)
                    .count())
                    .isEqualTo(1);
            assertThat(resultados.stream()
                    .filter(PreguntaConflictoException.class::isInstance)
                    .count())
                    .isEqualTo(1);

            PreguntaConflictoException conflicto = resultados.stream()
                    .filter(PreguntaConflictoException.class::isInstance)
                    .map(PreguntaConflictoException.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertThat(conflicto.getConflicto().codigo())
                    .isEqualTo("PREGUNTA_DUPLICADA_ACTIVA");
        } finally {
            executor.shutdownNow();
        }

        Integer preguntasActivas = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM pregunta
                WHERE codigo_factor = ?
                  AND eliminado = false
                  AND upper(btrim(nombre)) = ?
                """,
                Integer.class,
                contexto.codigoFactor(),
                solicitud.nombre());
        assertThat(preguntasActivas).isEqualTo(1);
    }

    @Test
    void reviertePreguntaYPonderacionesSiFallaElDAODePonderaciones() {
        Contexto contexto = crearContexto(2);
        String nombre = "PREGUNTA ROLLBACK " + contexto.codigoFactor();
        CrearPreguntaRequest solicitud = solicitud(
                contexto,
                nombre,
                "TEXTO ROLLBACK " + contexto.codigoFactor(),
                null,
                valores(contexto, "5.00", "5.00"));
        doAnswer(invocacion -> {
            Iterable<PonderacionItem> ponderaciones =
                    invocacion.getArgument(0);
            for (PonderacionItem ponderacion : ponderaciones) {
                entityManager.persist(ponderacion);
            }
            entityManager.flush();
            throw new DataIntegrityViolationException(
                    "Fallo de persistencia simulado después de insertar ponderaciones");
        }).when(ponderacionItemDAO)
                .saveAllAndFlush(any(Iterable.class));

        assertThatThrownBy(() -> gestorPregunta.crearPregunta(solicitud))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(ponderacionItemDAO).saveAllAndFlush(any(Iterable.class));

        Integer preguntasPersistidas = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM pregunta
                WHERE codigo_factor = ?
                  AND nombre = ?
                """,
                Integer.class,
                contexto.codigoFactor(),
                nombre);
        Integer ponderacionesPersistidas = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM ponderacion_item pi
                JOIN pregunta p ON p.codigo = pi.codigo_pregunta
                WHERE p.codigo_factor = ?
                  AND p.nombre = ?
                """,
                Integer.class,
                contexto.codigoFactor(),
                nombre);

        assertThat(preguntasPersistidas).isZero();
        assertThat(ponderacionesPersistidas).isZero();
    }

    private Contexto crearContexto(int cantidadItems) {
        int codigo = siguienteCodigo();
        Competencia competencia = crearCompetencia(codigo, false);
        Factor factor = crearFactor(competencia, codigo + 1, false);
        OpcionRespuesta opcion = opcionRespuestaDAO.saveAndFlush(
                OpcionRespuesta.builder()
                        .nombre("OPCION " + codigo)
                        .descripcion("PRUEBA CU21")
                        .eliminado(false)
                        .build());
        List<ItemOpcion> items = new ArrayList<>();
        for (int orden = 1; orden <= cantidadItems; orden++) {
            items.add(itemOpcionDAO.saveAndFlush(ItemOpcion.builder()
                    .ordenVisualizacion(orden)
                    .descripcion("ITEM " + codigo + " " + orden)
                    .opcionRespuesta(opcion)
                    .build()));
        }
        return new Contexto(
                competencia.getCodigo(),
                factor.getCodigo(),
                opcion.getIdOpcionRespuesta(),
                items);
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
            boolean eliminado) {
        return factorDAO.saveAndFlush(Factor.builder()
                .codigo(codigo)
                .nombre("FACTOR " + codigo)
                .descripcion("PRUEBA CU21")
                .nroOrden(1)
                .competencia(competencia)
                .eliminado(eliminado)
                .build());
    }

    private CrearPreguntaRequest solicitud(
            Contexto contexto,
            String nombre,
            String texto,
            String descripcion,
            List<PonderacionItemRequest> ponderaciones) {
        return new CrearPreguntaRequest(
                contexto.codigoCompetencia(),
                contexto.codigoFactor(),
                nombre,
                texto,
                descripcion,
                contexto.idOpcion(),
                ponderaciones);
    }

    private List<PonderacionItemRequest> valores(
            Contexto contexto,
            String... valores) {
        List<PonderacionItemRequest> ponderaciones = new ArrayList<>();
        for (int indice = 0; indice < valores.length; indice++) {
            ponderaciones.add(new PonderacionItemRequest(
                    contexto.items().get(indice).getIdItemOpcion(),
                    new BigDecimal(valores[indice])));
        }
        return ponderaciones;
    }

    private Pregunta crearPreguntaExistente(
            Contexto contexto,
            String nombre,
            String texto,
            boolean eliminada) {
        Pregunta pregunta = preguntaDAO.saveAndFlush(Pregunta.builder()
                .nombre(nombre)
                .pregunta(texto)
                .descripcion("DESCRIPCION ORIGINAL")
                .factor(factorDAO.findById(contexto.codigoFactor()).orElseThrow())
                .opcionRespuesta(opcionRespuestaDAO.findById(contexto.idOpcion())
                        .orElseThrow())
                .eliminado(eliminada)
                .build());
        ponderacionItemDAO.saveAllAndFlush(List.of(PonderacionItem.builder()
                .ponderacion(new BigDecimal("10.00"))
                .pregunta(pregunta)
                .itemOpcion(contexto.items().getFirst())
                .build()));
        return pregunta;
    }

    private PreguntaConflictoException capturarConflicto(
            CrearPreguntaRequest solicitud) {
        Throwable conflicto = org.assertj.core.api.Assertions.catchThrowable(
                () -> gestorPregunta.crearPregunta(solicitud));
        assertThat(conflicto).isInstanceOf(PreguntaConflictoException.class);
        return (PreguntaConflictoException) conflicto;
    }

    private Object intentarCrear(CrearPreguntaRequest solicitud) {
        try {
            return gestorPregunta.crearPregunta(solicitud);
        } catch (RuntimeException excepcion) {
            return excepcion;
        }
    }

    private int siguienteCodigo() {
        return CODIGO_TEST.getAndAdd(10);
    }

    private record Contexto(
            Integer codigoCompetencia,
            Integer codigoFactor,
            Long idOpcion,
            List<ItemOpcion> items) {
    }
}
