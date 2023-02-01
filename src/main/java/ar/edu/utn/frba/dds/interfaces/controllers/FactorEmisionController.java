package ar.edu.utn.frba.dds.interfaces.controllers;

import ar.edu.utn.frba.dds.entities.medibles.Categoria;
import ar.edu.utn.frba.dds.entities.medibles.FactorEmision;
import ar.edu.utn.frba.dds.interfaces.input.json.FactorEmisionJSONDTO;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserJSON;
import ar.edu.utn.frba.dds.repositories.RepoFactores;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;

import spark.Request;
import spark.Response;

import java.util.List;

public class FactorEmisionController {
    private final RepoFactores repoFactores;

    public FactorEmisionController(){
        this.repoFactores = FactoryRepositorio.getByOutputType(RepoFactores.class);
    }

    public FactorEmision modificar(Request request, Response response) {
        FactorEmisionJSONDTO dto = new ParserJSON<>(FactorEmisionJSONDTO.class).parseElement(request.body());
        Categoria categoria = new Categoria(dto.getCategoria().getActividad(), dto.getCategoria().getTipoConsumo());

        FactorEmision factorEmision = this.repoFactores.findByCategoria(categoria)
                        .stream().findFirst()
                .orElse(new FactorEmision(categoria, dto.getUnidad(), dto.getValor()));

        factorEmision.setUnidad(dto.getUnidad());
        factorEmision.setValor(dto.getValor());

        return this.repoFactores.modificar(factorEmision);
    }

    public String mostrarTodos(Request request, Response response) {
        List<FactorEmision> factores = this.repoFactores.buscarTodos();
        return factores.toString();
    }

}
