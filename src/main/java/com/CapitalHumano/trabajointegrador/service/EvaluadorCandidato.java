package com.CapitalHumano.trabajointegrador.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.CapitalHumano.trabajointegrador.dto.CandidatoDTO;
import com.CapitalHumano.trabajointegrador.dto.CompetenciaPonderadaDTO;
import com.CapitalHumano.trabajointegrador.dto.PuestoEvaluadoDTO;
import com.CapitalHumano.trabajointegrador.entity.Candidato;
import com.CapitalHumano.trabajointegrador.entity.CaracteristicaPuesto;
import com.CapitalHumano.trabajointegrador.entity.Competencia;
import com.CapitalHumano.trabajointegrador.entity.Puesto;
import com.CapitalHumano.trabajointegrador.repository.CandidatoDAO;
import com.CapitalHumano.trabajointegrador.repository.CaracteristicaPuestoDAO;
import com.CapitalHumano.trabajointegrador.repository.FactorDAO;
import com.CapitalHumano.trabajointegrador.repository.PreguntaDAO;
import com.CapitalHumano.trabajointegrador.repository.PuestoDAO;
import com.CapitalHumano.trabajointegrador.entity.Factor;

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
        List<Puesto> puestosLista = puestoDAO.findByEliminadoFalse();
        ArrayList<PuestoEvaluadoDTO> puestoEvalDTO = new ArrayList<PuestoEvaluadoDTO>();

        for(Puesto p: puestosLista){
            puestoEvalDTO.add(generarPuestoEvaluadoDTO(p));
        }

        return puestoEvalDTO;
    }

    private PuestoEvaluadoDTO generarPuestoEvaluadoDTO (Puesto puesto){
        String nombre = puesto.getNombrePuesto();
        String empresa = puesto.getEmpresa();
        String codigo = puesto.getCodigoPuesto();

        return new PuestoEvaluadoDTO(nombre, empresa, codigo);
    }

    public List<CompetenciaPonderadaDTO> buscarCompetenciasEvaluarValidas (String codPuesto){
        //Buscamos las competencias y ponderaciones del puesto
        List<CaracteristicaPuesto> caracteristicas = caracteristicaPuestoDAO.buscarCaracteristicaPuesto(codPuesto);

        if(caracteristicas.isEmpty()){
            throw new IllegalArgumentException("El puesto seleccionado no tiene competencias asociadas");
        }

        List<String> noEvaluables = new ArrayList<String>();
        List<CompetenciaPonderadaDTO> compeDTOLista = new ArrayList<CompetenciaPonderadaDTO>();
        //Por cada competencia buscamos que tenga un factor evaluable
        for(CaracteristicaPuesto cp: caracteristicas){
            Competencia competencia = cp.getCompetencia();

            if(!tieneFactorEvaluable(competencia)){
                noEvaluables.add(competencia.getNombre());
            }
            else{
                compeDTOLista.add(generarCompetenciaPonderadaDTO(cp));
            }
            
        }

        if(!noEvaluables.isEmpty()){
            throw new IllegalStateException("Las siguientes competencias no pueden ser evaluadas: "
                + String.join(", ", noEvaluables)
            );
        }

        return compeDTOLista;

    }
    
    private boolean tieneFactorEvaluable (Competencia c){
        List<Factor> factoresCompetencia = factorDAO.findByCompetenciaCodigoAndEliminadoFalseOrderByNroOrdenAsc(c.getCodigo());

        for(Factor f: factoresCompetencia){
            if(factorEvaluable(f)){
                return true;
            }
        }

        return false;
    }

    private boolean factorEvaluable (Factor f){
        if(preguntaDAO.findByFactorCodigoAndEliminadoFalse(f.getCodigo()).size()>=2){
            return true;
        }

        return false;
    }

     private CompetenciaPonderadaDTO generarCompetenciaPonderadaDTO (CaracteristicaPuesto cp){
        String compNombre = cp.getCompetencia().getNombre();
        Integer ponderacion = cp.getPonderacionMinima();

        return new CompetenciaPonderadaDTO(compNombre, ponderacion);
    }

}
