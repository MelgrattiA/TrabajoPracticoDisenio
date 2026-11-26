package com.CapitalHumano.trabajointegrador.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.CapitalHumano.trabajointegrador.dto.CandidatoClaveDTO;
import com.CapitalHumano.trabajointegrador.dto.CandidatoDTO;
import com.CapitalHumano.trabajointegrador.dto.CompetenciaPonderadaDTO;
import com.CapitalHumano.trabajointegrador.dto.PuestoEvaluadoDTO;
import com.CapitalHumano.trabajointegrador.entity.Candidato;
import com.CapitalHumano.trabajointegrador.entity.CaracteristicaPuesto;
import com.CapitalHumano.trabajointegrador.entity.Competencia;
import com.CapitalHumano.trabajointegrador.entity.Cuestionario;
import com.CapitalHumano.trabajointegrador.entity.Puesto;
import com.CapitalHumano.trabajointegrador.repository.CandidatoDAO;
import com.CapitalHumano.trabajointegrador.repository.CaracteristicaPuestoDAO;
import com.CapitalHumano.trabajointegrador.repository.CuestionarioDAO;
import com.CapitalHumano.trabajointegrador.repository.EstadoCuestionarioDAO;
import com.CapitalHumano.trabajointegrador.repository.FactorDAO;
import com.CapitalHumano.trabajointegrador.repository.PreguntaDAO;
import com.CapitalHumano.trabajointegrador.repository.PuestoDAO;

import jakarta.transaction.Transactional;

import com.CapitalHumano.trabajointegrador.entity.Factor;
import com.CapitalHumano.trabajointegrador.entity.EstadoCuestionario;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EvaluadorCandidato {
    private final CandidatoDAO candidatoDAO;
    private final GestorCuestionario gestorCuestionario;
    private final PuestoDAO puestoDAO;
    private final CaracteristicaPuestoDAO caracteristicaPuestoDAO;
    private final FactorDAO factorDAO;
    private final PreguntaDAO preguntaDAO;
    private final CuestionarioDAO cuestionarioDAO;
    private final EstadoCuestionarioDAO estadoCuestionarioDAO;

    // Para generar claves
    private static final String CARACTERES_CLAVE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();


    public List<CandidatoDTO> buscarCandidato(Integer nroCandidato, String nombre, String apellido){
        String nombreFiltro = (nombre == null || nombre.isBlank()) ? null : nombre.trim().toLowerCase();
        String apellidoFiltro = (apellido == null || apellido.isBlank()) ? null : apellido.trim().toLowerCase();

        if (nroCandidato == null && nombreFiltro == null && apellidoFiltro == null) {
            throw new IllegalArgumentException("Debe ingresar al menos un argumento");
        }

        List<Candidato> candidatosLista = candidatoDAO.buscarCandidato(nroCandidato, nombreFiltro, apellidoFiltro);

        ArrayList<CandidatoDTO> candidatoDTOLista = new ArrayList<CandidatoDTO>();

        for (Candidato c : candidatosLista) {
            candidatoDTOLista.add(generarCandidatoDTO(c));
        }

        return candidatoDTOLista;

    }

    private CandidatoDTO generarCandidatoDTO(Candidato candidato) {
        int nro = candidato.getNroCandidato();
        String nom = candidato.getNombre();
        String apel = candidato.getApellido();

        return new CandidatoDTO(nro, nom, apel);
    }

    public List<Integer> validarCandidatos(List<Integer> listaIdCandidatos) {
        if (listaIdCandidatos == null || listaIdCandidatos.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un candidato para evaluar.");
        }
        return gestorCuestionario.buscarActivoOEnProcesoLista(listaIdCandidatos);
    }

    public List<PuestoEvaluadoDTO> obtenerPuestos() {
        List<Puesto> puestosLista = puestoDAO.findByEliminadoFalse();
        ArrayList<PuestoEvaluadoDTO> puestoEvalDTO = new ArrayList<PuestoEvaluadoDTO>();

        for (Puesto p : puestosLista) {
            puestoEvalDTO.add(generarPuestoEvaluadoDTO(p));
        }

        return puestoEvalDTO;
    }

    private PuestoEvaluadoDTO generarPuestoEvaluadoDTO(Puesto puesto) {
        String nombre = puesto.getNombrePuesto();
        String empresa = puesto.getEmpresa();
        String codigo = puesto.getCodigoPuesto();

        return new PuestoEvaluadoDTO(nombre, empresa, codigo);
    }

    public List<CompetenciaPonderadaDTO> buscarCompetenciasEvaluarValidas(String codPuesto) {
        // Buscamos las competencias y ponderaciones del puesto
        List<CaracteristicaPuesto> caracteristicas = caracteristicaPuestoDAO.buscarCaracteristicaPuesto(codPuesto);

        if (caracteristicas.isEmpty()) {
            throw new IllegalArgumentException("El puesto seleccionado no tiene competencias asociadas");
        }

        List<String> noEvaluables = new ArrayList<String>();
        List<CompetenciaPonderadaDTO> compeDTOLista = new ArrayList<CompetenciaPonderadaDTO>();
        // Por cada competencia buscamos que tenga un factor evaluable
        for (CaracteristicaPuesto cp : caracteristicas) {
            Competencia competencia = cp.getCompetencia();

            if (!tieneFactorEvaluable(competencia)) {
                noEvaluables.add(competencia.getNombre());
            } else {
                compeDTOLista.add(generarCompetenciaPonderadaDTO(cp));
            }

        }

        if (!noEvaluables.isEmpty()) {
            throw new IllegalStateException("Las siguientes competencias no pueden ser evaluadas: "
                    + String.join(", ", noEvaluables));
        }

        return compeDTOLista;

    }

    private boolean tieneFactorEvaluable(Competencia c) {
        List<Factor> factoresCompetencia = factorDAO
                .findByCompetenciaCodigoAndEliminadoFalseOrderByNroOrdenAsc(c.getCodigo());

        for (Factor f : factoresCompetencia) {
            if (factorEvaluable(f)) {
                return true;
            }
        }

        return false;
    }

    private boolean factorEvaluable(Factor f) {
        if (preguntaDAO.findByFactorCodigoAndEliminadoFalse(f.getCodigo()).size() >= 2) {
            return true;
        }

        return false;
    }

    private CompetenciaPonderadaDTO generarCompetenciaPonderadaDTO(CaracteristicaPuesto cp) {
        String compNombre = cp.getCompetencia().getNombre();
        BigDecimal ponderacion = cp.getPonderacionMinima();

        return new CompetenciaPonderadaDTO(compNombre, ponderacion);
    }

    public List<CandidatoClaveDTO> buscarCandidatosYGenerarClaves(List<Integer> idCandidatos) {
        List<Candidato> candidatosClave = candidatoDAO.findByNroCandidatoInAndEliminadoFalse(idCandidatos);
        List<CandidatoClaveDTO> candidatoClaveDTOs = new ArrayList<CandidatoClaveDTO>();

        for (Candidato c : candidatosClave) {
            candidatoClaveDTOs.add(generarCandidatoClaveDTO(c, generarClaveCandidato()));
        }

        return candidatoClaveDTOs;
    }

    private String generarClaveCandidato() {
        String clave;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                int indice = RANDOM.nextInt(CARACTERES_CLAVE.length());
                sb.append(CARACTERES_CLAVE.charAt(indice));
            }
            clave = sb.toString();
        } while (cuestionarioDAO.existsById(clave));

        return clave;
    }

    private CandidatoClaveDTO generarCandidatoClaveDTO(Candidato c, String clave) {
        Integer nro_candidato = c.getNroCandidato();
        String nombre = c.getNombre();
        String apellido = c.getApellido();
        String tipoDocumento = c.getTipoDocumento();
        Integer nroDocumento = c.getNroDocumento();

        return new CandidatoClaveDTO(nro_candidato, nombre, apellido, tipoDocumento, nroDocumento, clave);
    }

    @Transactional
    public void guardarInformacion(List<CandidatoClaveDTO> candidatosLista, String codigoPuesto) {
        List<Cuestionario> evaluaciones = new ArrayList<Cuestionario>();
        Puesto puesto = puestoDAO.getReferenceById(codigoPuesto);
        LocalDateTime now = LocalDateTime.now();
        EstadoCuestionario estadoActivo = estadoCuestionarioDAO.getReferenceById(1);

        for (CandidatoClaveDTO c : candidatosLista) {
            Candidato candidato = candidatoDAO.getReferenceById(c.getNro_candidato());

            Cuestionario cuestionario = Cuestionario.builder()
                    .clave(c.getClaveGenerada())
                    .fechaCreacion(now)
                    .cantidadAccesos(0)
                    .candidato(candidato)
                    .puesto(puesto)
                    .estado(estadoActivo)
                    .build();

            evaluaciones.add(cuestionario);
        }

        cuestionarioDAO.saveAll(evaluaciones);
    }
}
