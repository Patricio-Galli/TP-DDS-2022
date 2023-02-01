package ar.edu.utn.frba.dds.interfaces.controllers;

import ar.edu.utn.frba.dds.entities.lugares.Organizacion;
import ar.edu.utn.frba.dds.entities.medibles.Medicion;
import ar.edu.utn.frba.dds.interfaces.input.json.MedicionJSONDTO;
import ar.edu.utn.frba.dds.interfaces.mappers.MedicionMapper;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MedicionController {
    private Repositorio<Medicion> repoMediciones;
    private Repositorio<Organizacion> repoOrganizaciones;

    public MedicionController(){
        this.repoMediciones = FactoryRepositorio.getByParameterType(Medicion.class);
        this.repoOrganizaciones = FactoryRepositorio.getByParameterType(Organizacion.class);
    }

    public String obtener(Request request, Response response) {
        Medicion medicion = this.repoMediciones.buscar(Integer.parseInt(request.params("id"))).get();
        return medicion.toString();
    }

    public List<MedicionJSONDTO> mostrarTodos(Request request, Response response) {
        Organizacion organizacion = this.repoOrganizaciones.buscar(Integer.parseInt(request.params("id"))).get();
        return organizacion.getMediciones().stream()
                .map(MedicionMapper::toDTO).collect(Collectors.toList());
    }

    public String filtrarUnidad(Request request, Response response) {
        List<Medicion> mediciones = this.repoMediciones.buscarTodos();
        return  mediciones
                .stream()
                .filter(m -> Objects.equals(m.getUnidad(), String.valueOf(request.params("unidad"))))
                .collect(Collectors.toList())
                .toString();
    }

    public String filtrarValor(Request request, Response response) {
        List<Medicion> mediciones = this.repoMediciones.buscarTodos();
        return  mediciones
                .stream()
                .filter(m -> Objects.equals(m.getValor(), Float.valueOf(request.params("valor"))))
                .collect(Collectors.toList())
                .toString();
    }
}

