package com.CapitalHumano.trabajointegrador.exception;

public class PuestoSinCompetenciasException extends RuntimeException {

    public PuestoSinCompetenciasException() {
        super("El puesto no tiene competencias configuradas");
    }
}
