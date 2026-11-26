package com.CapitalHumano.trabajointegrador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.CapitalHumano.trabajointegrador.config.PostgreSqlTestConfiguration;
import com.CapitalHumano.trabajointegrador.dto.ClasificacionCuestionariosDTO;
import com.CapitalHumano.trabajointegrador.dto.OrdenMeritoDTO;
import com.CapitalHumano.trabajointegrador.dto.OrdenMeritoIncompletoDTO;
import com.CapitalHumano.trabajointegrador.dto.PuestoOrdenMeritoDTO;
import com.CapitalHumano.trabajointegrador.dto.ReporteOrdenMeritoDTO;
import com.CapitalHumano.trabajointegrador.entity.Candidato;
import com.CapitalHumano.trabajointegrador.entity.CaracteristicaPuesto;
import com.CapitalHumano.trabajointegrador.entity.CaracteristicaPuestoId;
import com.CapitalHumano.trabajointegrador.entity.Competencia;
import com.CapitalHumano.trabajointegrador.entity.Cuestionario;
import com.CapitalHumano.trabajointegrador.entity.EstadoCuestionario;
import com.CapitalHumano.trabajointegrador.entity.Puesto;
import com.CapitalHumano.trabajointegrador.entity.ResultadoCompetencia;
import com.CapitalHumano.trabajointegrador.entity.ResultadoCompetenciaId;
import com.CapitalHumano.trabajointegrador.exception.PuestoNoEncontradoException;
import com.CapitalHumano.trabajointegrador.exception.PuestoSinCompetenciasException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgreSqlTestConfiguration.class)
@Transactional
class GestorOrdenMeritoIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private GestorOrdenMerito gestorOrdenMerito;

    @BeforeEach
    void prepararDatos() {
        Puesto puesto = crearPuesto("P-100", "Analista", "Acme", false);
        Puesto otroPuesto = crearPuesto("P-200", "Analista Senior", "Globex", false);
        crearPuesto("P-400", "Diseñador", "Acme", false);
        crearPuesto("P-300", "Analista Eliminado", "Acme", true);

        Candidato candidato = crearCandidato(1, "Gomez");
        Candidato otroCandidato = crearCandidato(2, "Perez");

        crearCuestionario("Q-1", puesto, candidato, 3);
        crearCuestionario("Q-2", puesto, candidato, 3);
        crearCuestionario("Q-3", puesto, otroCandidato, 2);
        crearCuestionario("Q-5", puesto, candidato, 1);
        crearCuestionario("Q-6", puesto, otroCandidato, 4);
        crearCuestionario("Q-7", puesto, otroCandidato, 5);
        crearCuestionario("Q-4", otroPuesto, otroCandidato, 3);

        Competencia competenciaActiva = crearCompetencia(101, "Comunicación", false);
        Competencia competenciaEliminada = crearCompetencia(102, "Análisis", true);
        Competencia competenciaOtroPuesto = crearCompetencia(103, "Liderazgo", false);
        crearCaracteristica(puesto, competenciaActiva, new BigDecimal("4.25"));
        crearCaracteristica(puesto, competenciaEliminada, new BigDecimal("6.75"));
        crearCaracteristica(otroPuesto, competenciaOtroPuesto, new BigDecimal("2.00"));

        crearResultado("Q-1", puesto, competenciaActiva, new BigDecimal("4.12"));
        crearResultado("Q-1", puesto, competenciaEliminada, new BigDecimal("9.50"));
        crearResultado("Q-2", puesto, competenciaActiva, new BigDecimal("3.75"));
        crearResultado("Q-3", puesto, competenciaEliminada, new BigDecimal("7.00"));
        crearResultado("Q-4", otroPuesto, competenciaOtroPuesto, new BigDecimal("0.50"));
    }

    @Test
    void buscaPorCodigoExactoYCalculaCandidatosUnicosYCuestionariosCompletos() {
        List<PuestoOrdenMeritoDTO> resultados =
                gestorOrdenMerito.buscarPuestos(" P-100 ", null, null);

        assertThat(resultados).hasSize(1);
        PuestoOrdenMeritoDTO puesto = resultados.getFirst();
        assertThat(puesto.getCodigo()).isEqualTo("P-100");
        assertThat(puesto.getCandidatos()).isEqualTo(2L);
        assertThat(puesto.getEvaluacionesCompletas()).isEqualTo(2L);
    }

    @Test
    void buscaPorCodigoParcialSinDistinguirMayusculas() {
        assertThat(gestorOrdenMerito.buscarPuestos("p-10", null, null))
                .extracting(PuestoOrdenMeritoDTO::getCodigo)
                .containsExactly("P-100");
    }

    @Test
    void clasificaCadaCuestionarioYOrdenaAprobadosSinDeduplicarCandidatos() {
        Puesto puesto = entityManager.getReference(Puesto.class, "P-100");
        Candidato candidato = entityManager.getReference(Candidato.class, 1);
        Candidato otroCandidato = entityManager.getReference(Candidato.class, 2);
        Competencia comunicacion = entityManager.getReference(Competencia.class, 101);
        Competencia analisis = entityManager.getReference(Competencia.class, 102);

        crearCuestionario("Q-8", puesto, candidato, 3);
        actualizarPuntajeFinal("Q-8", new BigDecimal("8.50"));
        crearResultado("Q-8", puesto, comunicacion, new BigDecimal("4.250"));
        crearResultado("Q-8", puesto, analisis, new BigDecimal("6.75"));

        crearCuestionario("Q-9", puesto, candidato, 3);
        actualizarPuntajeFinal("Q-9", new BigDecimal("9.50"));
        crearResultado("Q-9", puesto, comunicacion, new BigDecimal("5.00"));
        crearResultado("Q-9", puesto, analisis, new BigDecimal("7.00"));

        crearCuestionario("Q-10", puesto, otroCandidato, 3);
        actualizarPuntajeFinal("Q-10", new BigDecimal("7.00"));
        crearResultado("Q-10", puesto, comunicacion, new BigDecimal("3.00"));
        crearResultado("Q-10", puesto, analisis, new BigDecimal("6.00"));

        crearCuestionario("Q-11", puesto, otroCandidato, 3);
        actualizarPuntajeFinal("Q-11", null);
        crearResultado("Q-11", puesto, comunicacion, new BigDecimal("4.25"));
        crearResultado("Q-11", puesto, analisis, new BigDecimal("6.75"));

        ClasificacionCuestionariosDTO clasificacion =
                gestorOrdenMerito.clasificarCuestionariosPorPuesto("P-100");

        assertThat(clasificacion.cuestionariosAprobados())
                .containsExactly("Q-9", "Q-8");
        assertThat(clasificacion.cuestionariosDesaprobados())
                .containsExactlyInAnyOrder("Q-1", "Q-10");
        assertThat(clasificacion.cuestionariosSinCompletarPorEstado())
                .containsEntry("Activo", List.of("Q-5"))
                .containsEntry("En Proceso", List.of("Q-3"))
                .containsEntry("Incompleto", List.of("Q-6"))
                .containsEntry("Sin Contestar", List.of("Q-7"));

        assertThat(clasificacion.cuestionariosInconsistentes())
                .containsKeys("Q-2", "Q-11");
        assertThat(clasificacion.cuestionariosInconsistentes().get("Q-2")
                .codigosCompetenciasFaltantes())
                .containsExactly(102);
        assertThat(clasificacion.cuestionariosInconsistentes().get("Q-2")
                .puntajeFinalAusente())
                .isFalse();
        assertThat(clasificacion.cuestionariosInconsistentes().get("Q-11")
                .codigosCompetenciasFaltantes())
                .isEmpty();
        assertThat(clasificacion.cuestionariosInconsistentes().get("Q-11")
                .puntajeFinalAusente())
                .isTrue();
    }

    @Test
    void rechazaClasificarUnPuestoSinCompetencias() {
        assertThatThrownBy(() ->
                gestorOrdenMerito.clasificarCuestionariosPorPuesto("P-400"))
                .isInstanceOf(PuestoSinCompetenciasException.class)
                .hasMessage("El puesto no tiene competencias configuradas");
    }

    @Test
    void distingueUnPuestoInexistenteDeUnoSinCompetencias() {
        assertThatThrownBy(() ->
                gestorOrdenMerito.generarReporteOrdenMerito("P-999"))
                .isInstanceOf(PuestoNoEncontradoException.class)
                .hasMessage("No se encontró el puesto: P-999");
    }

    @Test
    void desempataAprobadosPorCandidatoFechaYClaveDeCuestionario() {
        Puesto puesto = entityManager.getReference(Puesto.class, "P-100");
        Candidato candidato = entityManager.getReference(Candidato.class, 1);
        Candidato otroCandidato = entityManager.getReference(Candidato.class, 2);
        Competencia comunicacion = entityManager.getReference(Competencia.class, 101);
        Competencia analisis = entityManager.getReference(Competencia.class, 102);

        crearCuestionario("Q-TIE-01", puesto, candidato, 3);
        crearCuestionario("Q-TIE-02", puesto, candidato, 3);
        crearCuestionario("Q-TIE-03", puesto, candidato, 3);
        crearCuestionario("Q-TIE-04", puesto, otroCandidato, 3);
        crearCuestionario("Q-TIE-05", puesto, candidato, 3);

        for (String clave : List.of(
                "Q-TIE-01", "Q-TIE-02", "Q-TIE-03", "Q-TIE-04", "Q-TIE-05")) {
            actualizarPuntajeFinal(clave, new BigDecimal("8.50"));
            crearResultado(clave, puesto, comunicacion, new BigDecimal("4.25"));
            crearResultado(clave, puesto, analisis, new BigDecimal("6.75"));
        }

        actualizarFechaInicio(
                "Q-TIE-03", LocalDateTime.of(2026, 11, 22, 9, 0));
        actualizarFechaInicio(
                "Q-TIE-04", LocalDateTime.of(2026, 11, 21, 9, 0));
        actualizarFechaInicio("Q-TIE-05", null);

        ClasificacionCuestionariosDTO clasificacion =
                gestorOrdenMerito.clasificarCuestionariosPorPuesto("P-100");

        assertThat(clasificacion.cuestionariosAprobados())
                .containsExactly(
                        "Q-TIE-03",
                        "Q-TIE-01",
                        "Q-TIE-02",
                        "Q-TIE-05",
                        "Q-TIE-04");
    }

    @Test
    void generaReporteConGruposYExcluyeCuestionariosInconsistentes() {
        Puesto puesto = entityManager.getReference(Puesto.class, "P-100");
        Candidato candidato = entityManager.getReference(Candidato.class, 1);
        Competencia comunicacion = entityManager.getReference(Competencia.class, 101);
        Competencia analisis = entityManager.getReference(Competencia.class, 102);

        crearCuestionario("Q-8", puesto, candidato, 3);
        actualizarPuntajeFinal("Q-8", new BigDecimal("8.50"));
        crearResultado("Q-8", puesto, comunicacion, new BigDecimal("4.25"));
        crearResultado("Q-8", puesto, analisis, new BigDecimal("6.75"));

        crearCuestionario("Q-9", puesto, candidato, 3);
        actualizarPuntajeFinal("Q-9", new BigDecimal("9.50"));
        crearResultado("Q-9", puesto, comunicacion, new BigDecimal("4.25"));
        crearResultado("Q-9", puesto, analisis, new BigDecimal("6.75"));

        ReporteOrdenMeritoDTO reporte =
                gestorOrdenMerito.generarReporteOrdenMerito("P-100");

        assertThat(reporte.aprobados())
                .extracting(OrdenMeritoDTO::claveCuestionario)
                .containsExactly("Q-9", "Q-8");
        assertThat(reporte.aprobados())
                .extracting(OrdenMeritoDTO::posicion)
                .containsExactly(1, 2);
        assertThat(reporte.aprobados().getFirst().puntajeObtenido())
                .isEqualByComparingTo("9.50");
        assertThat(reporte.aprobados().getFirst().superaMinimo()).isTrue();
        assertThat(reporte.aprobados().getFirst().nombre()).isEqualTo("Candidato 1");
        assertThat(reporte.aprobados().getFirst().fechaFinalizacion())
                .isEqualTo(LocalDateTime.of(2026, 11, 20, 9, 0)
                        .plusDays(3)
                        .plusHours(4));

        assertThat(reporte.desaprobados())
                .extracting(OrdenMeritoDTO::claveCuestionario)
                .containsExactly("Q-1");
        assertThat(reporte.desaprobados().getFirst().posicion()).isNull();
        assertThat(reporte.desaprobados().getFirst().superaMinimo()).isFalse();

        assertThat(reporte.cuestionariosSinCompletarPorEstado().keySet())
                .containsExactly(
                        "Activo", "En Proceso", "Incompleto", "Sin Contestar");
        List<OrdenMeritoIncompletoDTO> activos =
                reporte.cuestionariosSinCompletarPorEstado().get("Activo");
        assertThat(activos)
                .extracting(OrdenMeritoIncompletoDTO::claveCuestionario)
                .containsExactly("Q-5");
        assertThat(activos.getFirst().fechaUltimoIngreso())
                .isEqualTo(LocalDateTime.of(2026, 11, 20, 9, 0)
                        .plusDays(1)
                        .plusHours(2));
        assertThat(activos.getFirst().cantidadAccesos()).isEqualTo(1);

        List<String> clavesDelReporte = new ArrayList<>();
        reporte.aprobados().forEach(dto ->
                clavesDelReporte.add(dto.claveCuestionario()));
        reporte.desaprobados().forEach(dto ->
                clavesDelReporte.add(dto.claveCuestionario()));
        reporte.cuestionariosSinCompletarPorEstado().values().stream()
                .flatMap(List::stream)
                .map(OrdenMeritoIncompletoDTO::claveCuestionario)
                .forEach(clavesDelReporte::add);

        assertThat(clavesDelReporte)
                .doesNotContain("Q-2")
                .doesNotHaveDuplicates();
    }

    @Test
    void obtieneTodosLosCuestionariosConSusRelacionesYDatosSinDuplicarlos() {
        entityManager.flush();
        entityManager.clear();

        List<Cuestionario> cuestionarios =
                gestorOrdenMerito.obtenerCuestionarios("P-100");

        assertThat(cuestionarios)
                .hasSize(6)
                .extracting(Cuestionario::getClave)
                .containsExactlyInAnyOrder(
                        "Q-1", "Q-2", "Q-3", "Q-5", "Q-6", "Q-7");
        assertThat(cuestionarios.stream()
                .map(Cuestionario::getClave)
                .distinct())
                .hasSize(6);
        assertThat(cuestionarios)
                .extracting(cuestionario ->
                        cuestionario.getEstado().getIdEstadoCuestionario())
                .containsExactlyInAnyOrder(1, 2, 3, 3, 4, 5);

        Cuestionario completo = buscarPorClave(cuestionarios, "Q-1");
        assertThat(completo.getCandidato().getApellido()).isEqualTo("Gomez");
        assertThat(completo.getCandidato().getTipoDocumento()).isEqualTo("DNI");
        assertThat(buscarPorClave(cuestionarios, "Q-2")
                .getCandidato()
                .getNroCandidato())
                .isEqualTo(completo.getCandidato().getNroCandidato());
        assertThat(completo.getEstado().getNombre()).isEqualTo("Completo");
        assertThat(completo.getFechaInicio())
                .isEqualTo(LocalDateTime.of(2026, 11, 20, 9, 0).plusDays(3));
        assertThat(completo.getFechaFinalizacion())
                .isEqualTo(LocalDateTime.of(2026, 11, 20, 13, 0).plusDays(3));
        assertThat(completo.getCantidadAccesos()).isEqualTo(3);
        assertThat(completo.getPuntajeFinal()).isEqualByComparingTo("8.50");

        Cuestionario incompleto = buscarPorClave(cuestionarios, "Q-6");
        assertThat(incompleto.getEstado().getNombre()).isEqualTo("Incompleto");
        assertThat(incompleto.getFechaInicio())
                .isEqualTo(LocalDateTime.of(2026, 11, 20, 9, 0).plusDays(4));
        assertThat(incompleto.getFechaUltimoIngreso())
                .isEqualTo(LocalDateTime.of(2026, 11, 20, 11, 0).plusDays(4));
        assertThat(incompleto.getFechaFinalizacion()).isNull();
        assertThat(incompleto.getCantidadAccesos()).isEqualTo(4);
        assertThat(incompleto.getPuntajeFinal()).isNull();
    }

    @Test
    void devuelveVacioParaPuestoSinCuestionarios() {
        assertThat(gestorOrdenMerito.obtenerCuestionarios("P-400")).isEmpty();
    }

    @Test
    void rechazaCodigoDePuestoNuloOVacio() {
        assertThatThrownBy(() -> gestorOrdenMerito.obtenerCuestionarios(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El código del puesto es obligatorio");
        assertThatThrownBy(() -> gestorOrdenMerito.obtenerCuestionarios("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El código del puesto es obligatorio");
    }

    @Test
    void obtieneTodasLasCaracteristicasConMinimosAunqueLaCompetenciaEsteEliminada() {
        entityManager.flush();
        entityManager.clear();

        var caracteristicas = gestorOrdenMerito.obtenerCaracteristicasPuesto("P-100");

        assertThat(caracteristicas).hasSize(2);
        assertThat(caracteristicas)
                .extracting(caracteristica ->
                        caracteristica.getCompetencia().getCodigo())
                .containsExactly(101, 102);
        assertThat(caracteristicas)
                .extracting(CaracteristicaPuesto::getPonderacionMinima)
                .containsExactly(
                        new BigDecimal("4.25"),
                        new BigDecimal("6.75"));
        assertThat(caracteristicas.get(1).getCompetencia().getEliminado()).isTrue();
    }

    @Test
    void devuelveVacioSiElPuestoNoTieneCompetencias() {
        assertThat(gestorOrdenMerito.obtenerCaracteristicasPuesto("P-400"))
                .isEmpty();
    }

    @Test
    void obtieneResultadosDeVariosCuestionariosEnLotePorPuestoYCompetencia() {
        entityManager.flush();
        entityManager.clear();

        List<ResultadoCompetencia> resultados =
                gestorOrdenMerito.obtenerResultadosCompetencia(
                        "P-100",
                        List.of("Q-1", "Q-2", "Q-3", "Q-4", "Q-5", "Q-1"));

        assertThat(resultados).hasSize(4);
        assertThat(resultados)
                .extracting(resultado -> resultado.getId().getClaveCuestionario()
                        + ":" + resultado.getId().getCodigoCompetencia())
                .containsExactly(
                        "Q-1:101",
                        "Q-1:102",
                        "Q-2:101",
                        "Q-3:102");
        assertThat(resultados)
                .extracting(ResultadoCompetencia::getPuntajeObtenido)
                .containsExactly(
                        new BigDecimal("4.12"),
                        new BigDecimal("9.50"),
                        new BigDecimal("3.75"),
                        new BigDecimal("7.00"));
        assertThat(resultados)
                .allSatisfy(resultado ->
                        assertThat(resultado.getId().getCodigoPuesto())
                                .isEqualTo("P-100"));
    }

    @Test
    void devuelveVacioParaCuestionariosSinResultadosOVacioDeClaves() {
        assertThat(gestorOrdenMerito.obtenerResultadosCompetencia(
                "P-100", List.of("Q-5"))).isEmpty();
        assertThat(gestorOrdenMerito.obtenerResultadosCompetencia(
                "P-100", List.of())).isEmpty();
    }

    @Test
    void buscaPorNombre() {
        assertThat(gestorOrdenMerito.buscarPuestos(null, "ANALI", null))
                .extracting(PuestoOrdenMeritoDTO::getCodigo)
                .containsExactly("P-100", "P-200");
    }

    @Test
    void buscaPorEmpresa() {
        assertThat(gestorOrdenMerito.buscarPuestos(null, null, "ACME"))
                .extracting(PuestoOrdenMeritoDTO::getCodigo)
                .containsExactly("P-100", "P-400");
    }

    @Test
    void devuelveVacioSiLaEmpresaCoincidePeroElNombreNo() {
        assertThat(gestorOrdenMerito.buscarPuestos(
                null, "Puesto inexistente", "ACME"))
                .isEmpty();
    }

    @Test
    void combinaNombreYEmpresaSinDistinguirMayusculas() {
        List<PuestoOrdenMeritoDTO> resultados =
                gestorOrdenMerito.buscarPuestos(null, "ANALI", "cme");

        assertThat(resultados)
                .extracting(PuestoOrdenMeritoDTO::getCodigo)
                .containsExactly("P-100");
    }

    @Test
    void devuelveVacioSiNoHayCoincidencias() {
        assertThat(gestorOrdenMerito.buscarPuestos(null, "Inexistente", null))
                .isEmpty();
    }

    @Test
    void devuelveConteosEnCeroParaPuestosSinCuestionarios() {
        List<PuestoOrdenMeritoDTO> resultados =
                gestorOrdenMerito.buscarPuestos("P-400", null, null);

        assertThat(resultados).hasSize(1);
        assertThat(resultados.getFirst().getCandidatos()).isZero();
        assertThat(resultados.getFirst().getEvaluacionesCompletas()).isZero();
    }

    @Test
    void rechazaLaBusquedaSinCriterios() {
        assertThatThrownBy(() -> gestorOrdenMerito.buscarPuestos(" ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Debe ingresar al menos un criterio de búsqueda");
    }

    private Puesto crearPuesto(
            String codigo,
            String nombre,
            String empresa,
            boolean eliminado) {
        Puesto puesto = Puesto.builder()
                .codigoPuesto(codigo)
                .nombrePuesto(nombre)
                .empresa(empresa)
                .eliminado(eliminado)
                .build();
        entityManager.persist(puesto);
        return puesto;
    }

    private Candidato crearCandidato(Integer numero, String apellido) {
        Candidato candidato = Candidato.builder()
                .nroCandidato(numero)
                .apellido(apellido)
                .nombre("Candidato " + numero)
                .tipoDocumento("DNI")
                .nroDocumento(numero * 1000)
                .eliminado(false)
                .build();
        entityManager.persist(candidato);
        return candidato;
    }

    private void crearCuestionario(
            String clave,
            Puesto puesto,
            Candidato candidato,
            Integer idEstado) {
        LocalDateTime fechaInicio =
                LocalDateTime.of(2026, 11, 20, 9, 0).plusDays(idEstado);
        Cuestionario cuestionario = Cuestionario.builder()
                .clave(clave)
                .puesto(puesto)
                .candidato(candidato)
                .estado(entityManager.getReference(EstadoCuestionario.class, idEstado))
                .fechaInicio(fechaInicio)
                .fechaUltimoIngreso(fechaInicio.plusHours(2))
                .fechaFinalizacion(
                        idEstado == 3 ? fechaInicio.plusHours(4) : null)
                .cantidadAccesos(idEstado)
                .puntajeFinal(idEstado == 3 ? new BigDecimal("8.50") : null)
                .build();
        entityManager.persist(cuestionario);
    }

    private void actualizarPuntajeFinal(String clave, BigDecimal puntajeFinal) {
        entityManager.find(Cuestionario.class, clave)
                .setPuntajeFinal(puntajeFinal);
    }

    private void actualizarFechaInicio(String clave, LocalDateTime fechaInicio) {
        entityManager.find(Cuestionario.class, clave)
                .setFechaInicio(fechaInicio);
    }

    private Competencia crearCompetencia(
            Integer codigo,
            String nombre,
            boolean eliminado) {
        Competencia competencia = Competencia.builder()
                .codigo(codigo)
                .nombre(nombre)
                .descripcion("Competencia de prueba")
                .eliminado(eliminado)
                .build();
        entityManager.persist(competencia);
        return competencia;
    }

    private void crearCaracteristica(
            Puesto puesto,
            Competencia competencia,
            BigDecimal ponderacionMinima) {
        CaracteristicaPuestoId id = CaracteristicaPuestoId.builder()
                .codigoPuesto(puesto.getCodigoPuesto())
                .codigoCompetencia(competencia.getCodigo())
                .build();
        entityManager.persist(CaracteristicaPuesto.builder()
                .id(id)
                .puesto(puesto)
                .competencia(competencia)
                .ponderacionMinima(ponderacionMinima)
                .build());
    }

    private void crearResultado(
            String claveCuestionario,
            Puesto puesto,
            Competencia competencia,
            BigDecimal puntajeObtenido) {
        Cuestionario cuestionario =
                entityManager.getReference(Cuestionario.class, claveCuestionario);
        CaracteristicaPuestoId caracteristicaId = CaracteristicaPuestoId.builder()
                .codigoPuesto(puesto.getCodigoPuesto())
                .codigoCompetencia(competencia.getCodigo())
                .build();
        CaracteristicaPuesto caracteristica =
                entityManager.getReference(CaracteristicaPuesto.class, caracteristicaId);
        ResultadoCompetenciaId id = ResultadoCompetenciaId.builder()
                .claveCuestionario(claveCuestionario)
                .codigoPuesto(puesto.getCodigoPuesto())
                .codigoCompetencia(competencia.getCodigo())
                .build();
        entityManager.persist(ResultadoCompetencia.builder()
                .id(id)
                .puntajeObtenido(puntajeObtenido)
                .cuestionario(cuestionario)
                .caracteristicaPuesto(caracteristica)
                .build());
    }

    private Cuestionario buscarPorClave(List<Cuestionario> cuestionarios, String clave) {
        return cuestionarios.stream()
                .filter(cuestionario -> cuestionario.getClave().equals(clave))
                .findFirst()
                .orElseThrow();
    }
}
