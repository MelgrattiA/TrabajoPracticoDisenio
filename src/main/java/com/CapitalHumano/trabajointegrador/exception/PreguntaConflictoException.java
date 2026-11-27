package com.CapitalHumano.trabajointegrador.exception;

import java.util.Objects;

import com.CapitalHumano.trabajointegrador.dto.ConflictoPreguntaDTO;

public class PreguntaConflictoException extends RuntimeException {

    private final ConflictoPreguntaDTO conflicto;

    public PreguntaConflictoException(ConflictoPreguntaDTO conflicto) {
        super(Objects.requireNonNull(conflicto).mensaje());
        this.conflicto = conflicto;
    }

    public ConflictoPreguntaDTO getConflicto() {
        return conflicto;
    }
}
