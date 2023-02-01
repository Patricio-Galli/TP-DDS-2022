package ar.edu.utn.frba.dds.interfaces.controllers;

import ar.edu.utn.frba.dds.entities.personas.Miembro;
import ar.edu.utn.frba.dds.interfaces.RequestInvalidoApiException;
import ar.edu.utn.frba.dds.interfaces.gui.dto.ErrorResponse;
import ar.edu.utn.frba.dds.interfaces.input.ErrorDTO;
import ar.edu.utn.frba.dds.interfaces.input.json.NuevaEntidadGenericaRequest;
import ar.edu.utn.frba.dds.interfaces.input.json.NuevaEntidadResponse;
import ar.edu.utn.frba.dds.interfaces.input.json.NuevoMiembroRequest;
import ar.edu.utn.frba.dds.interfaces.input.json.OrganizacionJSONDTO;
import ar.edu.utn.frba.dds.interfaces.mappers.MiembrosMapper;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserJSON;
import ar.edu.utn.frba.dds.repositories.RepoMiembros;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.server.login.User;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaUsuarios;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.MiHuellaApiException;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import javax.persistence.PersistenceException;
import java.util.List;

public class MiembroController {
    private final RepoMiembros repoMiembros;
    private final FachadaUsuarios fachadaUsuarios;

    public MiembroController() {
        this.repoMiembros = FactoryRepositorio.getByOutputType(RepoMiembros.class);
        this.fachadaUsuarios = new FachadaUsuarios();
    }

    public Object eliminar(Request request, Response response) {
        Miembro miembro = this.repoMiembros.buscar(Integer.parseInt(request.params("id"))).get();

        if(miembro == null) {
            response.status(400);
            return new ErrorResponse("El miembro de id " + request.params("id") + " no existe");
        }

        this.repoMiembros.eliminar(miembro);
        return "Organizacion borrada correctamente";
    }

    public Object modificar(Request request, Response response) {
        return "Modificacion de miembros no implementada";
    }

    public NuevaEntidadResponse agregar(Request request, Response response) {
        NuevaEntidadResponse res = new NuevaEntidadResponse();

        NuevaEntidadGenericaRequest<NuevoMiembroRequest> miembroRequest =
                new ParserJSON<>(NuevaEntidadGenericaRequest.class, NuevoMiembroRequest.class)
                        .parseBounded(request.body());

        try {
            fachadaUsuarios.validarRequest(miembroRequest.getUsuario());
        } catch (MiHuellaApiException e) {
            res.setEstado("ERROR");
            res.setError(e.getError());
            return res;
        }

        Miembro miembro = MiembrosMapper.toEntity(miembroRequest.getEntidad());

        try {
            miembro = this.repoMiembros.agregar(miembro);
        } catch (PersistenceException e) {
            response.status(HttpStatus.CONFLICT_409);
            res.setEstado("ERROR");
            res.setError(new ErrorDTO("ERROR_DE_DOMINIO", "Ya existe un miembro con el mismo par tipo " +
                    "documento + numero documento"));
            return res;
        }

        User usuario = this.fachadaUsuarios.agregar(new User(miembroRequest.getUsuario().getUsername(),
                miembroRequest.getUsuario().getPassword(), miembro));

        res.setEstado("OK");
        res.setEntidad(miembro.getId());
        res.setUsuario(usuario.getId());
        return res;
    }

    public String mostrarTodos(Request request, Response response){
        List<Miembro> organizaciones = this.repoMiembros.buscarTodos();
        return organizaciones.toString();
    }
}
