package com.CapitalHumano.trabajointegrador.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.CapitalHumano.trabajointegrador.dto.CandidatoDTO;
import com.CapitalHumano.trabajointegrador.dto.PuestoEvaluadoDTO;
import com.CapitalHumano.trabajointegrador.entity.Candidato;
import com.CapitalHumano.trabajointegrador.entity.Puesto;
import com.CapitalHumano.trabajointegrador.repository.CandidatoDAO;
import com.CapitalHumano.trabajointegrador.repository.PuestoDAO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor  
public class EvaluadorCandidato {
    private final CandidatoDAO candidatoDAO;
    private final GestorCuestionario gestorCuestionario;
    private final PuestoDAO puestoDAO;

    public List<CandidatoDTO> buscarCandidato(Integer nroCandidato, String nombre, String apellido){
        boolean sinFiltros = ((nombre == null || nombre.isBlank()) && 
                              (apellido == null || apellido.isBlank()) &&
                              (nroCandidato == null));
        if(sinFiltros){
            throw new IllegalArgumentException("Debe ingresar al menos un argumento");
        }
        
        List<Candidato> candidatosLista = candidatoDAO.buscarCandidato(nroCandidato,nombre,apellido);

        ArrayList<CandidatoDTO> candidatoDTOLista = new ArrayList<CandidatoDTO>();

        for(Candidato c: candidatosLista){
            candidatoDTOLista.add(generarCandidatoDTO(c));
        }

        return candidatoDTOLista;

   }

    private CandidatoDTO generarCandidatoDTO(Candidato candidato){
        int nro = candidato.getNroCandidato();
        String nom = candidato.getNombre();
        String apel = candidato.getApellido();

        return new CandidatoDTO(nro, nom, apel); 
    }

    public List<Integer> validarCandidatos(List<Integer> listaIdCandidatos){
        return gestorCuestionario.buscarActivoOEnProcesoLista(listaIdCandidatos);
    }

    public List<PuestoEvaluadoDTO> obtenerPuestos(){
        List<Puesto> puestosLista = puestoDAO.findAll();

        ArrayList<PuestoEvaluadoDTO> puestoEvalDTO = new ArrayList<PuestoEvaluadoDTO>();

        for(Puesto p: puestosLista){
            puestoEvalDTO.add(generarPuestoEvaluadoDTO(p));
        }

        return puestoEvalDTO;
    }

    private PuestoEvaluadoDTO generarPuestoEvaluadoDTO (Puesto puesto){
        String nombre = puesto.getNombrePuesto();
        String empresa = puesto.getEmpresa();

        return new PuestoEvaluadoDTO(nombre, empresa);
    }

}
