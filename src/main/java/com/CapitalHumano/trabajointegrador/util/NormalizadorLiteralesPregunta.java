package com.CapitalHumano.trabajointegrador.util;

import java.util.Locale;

public final class NormalizadorLiteralesPregunta {

    private static final Locale LOCALE_ES_AR =
            Locale.forLanguageTag("es-AR");

    private NormalizadorLiteralesPregunta() {
    }

    public static String normalizarObligatorio(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim().toUpperCase(LOCALE_ES_AR);
    }

    public static String normalizarDescripcion(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim().toUpperCase(LOCALE_ES_AR);
        return normalizado.isEmpty() ? null : normalizado;
    }
}
