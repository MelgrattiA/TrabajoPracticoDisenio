package com.CapitalHumano.trabajointegrador.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.CapitalHumano.trabajointegrador.dto.PuestoOrdenMeritoDTO;
import com.CapitalHumano.trabajointegrador.dto.ReporteOrdenMeritoDTO;
import com.CapitalHumano.trabajointegrador.service.GestorOrdenMerito;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orden-merito")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class OrdenMeritoController {

    private final GestorOrdenMerito gestorOrdenMerito;

    @GetMapping("/puestos")
    public List<PuestoOrdenMeritoDTO> buscarPuestos(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String empresa) {
        return gestorOrdenMerito.buscarPuestos(codigo, nombre, empresa);
    }

    @GetMapping("/puestos/{codigoPuesto}/reporte")
    public ReporteOrdenMeritoDTO generarReporte(
            @PathVariable String codigoPuesto) {
        return gestorOrdenMerito.generarReporteOrdenMerito(codigoPuesto);
    }
}
