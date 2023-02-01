package ar.edu.utn.frba.dds.interfaces.controllers;

import ar.edu.utn.frba.dds.entities.lugares.Organizacion;
import ar.edu.utn.frba.dds.entities.medibles.BatchMediciones;
import ar.edu.utn.frba.dds.entities.medibles.Medicion;
import ar.edu.utn.frba.dds.interfaces.input.json.BatchMedicionesResponse;
import ar.edu.utn.frba.dds.interfaces.input.json.MedicionJSONDTO;
import ar.edu.utn.frba.dds.interfaces.input.json.BatchMedicionJSONDTO;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserJSON;
import ar.edu.utn.frba.dds.interfaces.mappers.BatchMedicionMapper;
import ar.edu.utn.frba.dds.interfaces.mappers.MedicionMapper;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.MiHuellaApiException;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;

public class BatchMedicionController {
    private Repositorio<BatchMediciones> repoBatch;
    private final Repositorio<Organizacion> repoOrganizaciones;

    public BatchMedicionController(){
        this.repoBatch = FactoryRepositorio.getByParameterType(BatchMediciones.class);
        this.repoOrganizaciones = FactoryRepositorio.getByParameterType(Organizacion.class);
    }

    public List<BatchMedicionesResponse> mostrarTodos(Request request, Response response) {
        List<BatchMediciones> batches = this.repoBatch.buscarTodos();
        Organizacion organizacion = this.repoOrganizaciones.buscar(Integer.parseInt(request.params("id"))).get();
        return batches.stream()
                .filter(b -> b.getOrganizacion().getId().equals(organizacion.getId()))
                .map(BatchMedicionMapper::toDTOLazy).collect(Collectors.toList());
    }

    public BatchMedicionesResponse obtener(Request request, Response response) {
        BatchMediciones batchMediciones = this.repoBatch.buscar(Integer.parseInt(request.params("batch")))
                .filter(batch -> batch.getOrganizacion().getId()
                        .equals(Integer.parseInt(request.params("id"))))
                .orElseThrow(() -> new MiHuellaApiException("El batch " + request.params("batch")
                        + " de la organizacion " + request.params("id") + " no existe"));
        return BatchMedicionMapper.toDTO(batchMediciones);
    }

    public Object agregar(Request request, Response response) {
        BatchMedicionJSONDTO requestDTO = new ParserJSON<>(BatchMedicionJSONDTO.class).parseElement(request.body());
        List<Medicion> mediciones = requestDTO.getMediciones().stream()
                .map(MedicionMapper::toEntity).collect(Collectors.toList());

        Organizacion organizacion = this.repoOrganizaciones.buscar(Integer.parseInt(request.params("id"))).get();
        organizacion.agregarMediciones(mediciones);

        BatchMediciones batchMediciones = new BatchMediciones(mediciones);
        batchMediciones.setFecha(LocalDate.now());
        batchMediciones.setOrganizacion(organizacion);

        this.repoBatch.agregar(batchMediciones);

        response.status(HttpStatus.CREATED_201);
        return "BatchMediciones agregado correctamente.";
    }

    public Object eliminar(Request request, Response response) {
        Optional<BatchMediciones> batchMedicion = this.repoBatch.buscar(Integer.parseInt(request.params("id")));
        if (!batchMedicion.isPresent() || !batchMedicion.get().getOrganizacion().getId()
                .equals(Integer.parseInt(request.params("id")))) {
            throw new MiHuellaApiException("El batch " + request.params("batch") + " de la organizacion "
                    + request.params("id") + " no existe");
        }
        this.repoBatch.eliminar(batchMedicion.get());

        return "BatchMediciones de id : " + request.params("id") + " eliminado correctamente.";
    }

}