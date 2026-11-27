package com.CapitalHumano.trabajointegrador.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizadorLiteralesPreguntaTest {

    @Test
    void normalizaMayusculasMinusculasYEspacios() {
        assertThat(NormalizadorLiteralesPregunta.normalizarObligatorio(
                "  pregunta De Prueba  "))
                .isEqualTo("PREGUNTA DE PRUEBA");
    }

    @Test
    void normalizaCaracteresEspañoles() {
        assertThat(NormalizadorLiteralesPregunta.normalizarObligatorio(
                "ñ á é í ó ú ü"))
                .isEqualTo("Ñ Á É Í Ó Ú Ü");
    }

    @Test
    void manejaMayusculasTextoVacioYNulo() {
        assertThat(NormalizadorLiteralesPregunta.normalizarObligatorio(
                "YA MAYUSCULAS"))
                .isEqualTo("YA MAYUSCULAS");
        assertThat(NormalizadorLiteralesPregunta.normalizarObligatorio(""))
                .isEmpty();
        assertThat(NormalizadorLiteralesPregunta.normalizarObligatorio(null))
                .isEmpty();
    }

    @Test
    void normalizaDescripcionNulaOEnBlanco() {
        assertThat(NormalizadorLiteralesPregunta.normalizarDescripcion(null))
                .isNull();
        assertThat(NormalizadorLiteralesPregunta.normalizarDescripcion("   "))
                .isNull();
        assertThat(NormalizadorLiteralesPregunta.normalizarDescripcion(
                "  descripción áéñ  "))
                .isEqualTo("DESCRIPCIÓN ÁÉÑ");
    }

    @Test
    void normalizacionEsIdempotente() {
        String primero =
                NormalizadorLiteralesPregunta.normalizarObligatorio(
                        "  nombre Mixto ÁÉÑ  ");
        String segundo =
                NormalizadorLiteralesPregunta.normalizarObligatorio(primero);
        assertThat(segundo).isEqualTo(primero);
    }
}
