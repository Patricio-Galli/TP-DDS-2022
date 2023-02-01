package ar.edu.utn.frba.dds.interfaces.controllers;

import ar.edu.utn.frba.dds.entities.lugares.Organizacion;
import ar.edu.utn.frba.dds.entities.lugares.Sector;
import ar.edu.utn.frba.dds.entities.personas.Miembro;
import ar.edu.utn.frba.dds.entities.personas.TipoDeDocumento;
import ar.edu.utn.frba.dds.interfaces.RequestInvalidoApiException;
import ar.edu.utn.frba.dds.interfaces.input.ErrorDTO;
import ar.edu.utn.frba.dds.interfaces.input.json.*;
import ar.edu.utn.frba.dds.interfaces.mappers.OrganizacionMapper;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserJSON;
import ar.edu.utn.frba.dds.repositories.RepoMiembros;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.server.login.User;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaOrganizacionApi;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaUsuarios;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.MiHuellaApiException;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import javax.persistence.EntityNotFoundException;
import java.util.List;

public class OrganizacionController {
    private final Repositorio<Organizacion> repoOrganizaciones;
    private final RepoMiembros repoMiembros;
    private final FachadaOrganizacionApi fachadaOrganizacion;
    private final FachadaUsuarios fachadaUsuarios;

    public OrganizacionController() {
        this.repoOrganizaciones = FactoryRepositorio.getByParameterType(Organizacion.class);
        this.repoMiembros = FactoryRepositorio.getByOutputType(RepoMiembros.class);
        this.fachadaOrganizacion = new FachadaOrganizacionApi();
        this.fachadaUsuarios = new FachadaUsuarios();
    }

    public OrganizacionResponse obtener(Request request, Response response) {
        Organizacion organizacion = this.repoOrganizaciones.buscar(Integer.parseInt(request.params("id"))).get();
        return OrganizacionMapper.toResponse(organizacion);
    }

    public Object eliminar(Request request, Response response) {
        Organizacion organizacion = this.repoOrganizaciones.buscar(Integer.parseInt(request.params("id"))).get();

        this.repoOrganizaciones.eliminar(organizacion);
        return "Organizacion borrada correctamente";
    }

    public Object modificar(Request request, Response response) {
        response.status(400);
        return "Modificacion de organizaciones no está implementada";
    }

    public NuevaEntidadResponse agregar(Request request, Response response) {
        NuevaEntidadResponse res = new NuevaEntidadResponse();

        NuevaEntidadGenericaRequest<OrganizacionJSONDTO> orgRequest =
                new ParserJSON<>(NuevaEntidadGenericaRequest.class, OrganizacionJSONDTO.class)
                        .parseBounded(request.body());

        try {
            fachadaUsuarios.validarRequest(orgRequest.getUsuario());
        } catch (MiHuellaApiException e) {
            res.setEstado("ERROR");
            res.setError(e.getError());
            return res;
        }

        Organizacion organizacion;
        try {
            organizacion = this.fachadaOrganizacion.agregar(orgRequest.getEntidad());
        } catch (EntityNotFoundException | MiHuellaApiException e) {
            response.status(HttpStatus.CONFLICT_409);
            res.setEstado("ERROR");
            res.setError(new ErrorDTO("ERROR_DE_DOMINIO", e.getMessage()));
            return res;
        }

        User usuario = this.fachadaUsuarios.agregar(new User(orgRequest.getUsuario().getUsername(),
                orgRequest.getUsuario().getPassword(), organizacion));

        res.setEstado("OK");
        res.setEntidad(organizacion.getId());
        res.setUsuario(usuario.getId());
        return res;
    }

    public String mostrarTodos(Request request, Response response){
        List<Organizacion> organizaciones = this.repoOrganizaciones.buscarTodos();
        return organizaciones.toString();
    }

    public Response agregarMiembro(Request request, Response response) {
        Organizacion organizacion = this.repoOrganizaciones.buscar(Integer.parseInt(request.params("id"))).get();

        Sector sector = organizacion.getSectores().stream()
                .filter(sec -> sec.getId().equals(Integer.parseInt(request.params("sector"))))
                .findFirst().orElseThrow(() -> new RequestInvalidoApiException("No existe el sector"));

        MiembroIdentityRequest miembroRequest = new ParserJSON<>(MiembroIdentityRequest.class)
                .parseElement(request.body());

        Miembro miembro = this.repoMiembros.findByDocumento( //TODO validar body
                TipoDeDocumento.valueOf(miembroRequest.getTipoDocumento()), miembroRequest.getDocumento())
//                .orElseThrow(MiembroException::new);
                .orElseThrow(() -> new MiHuellaApiException("No existe el miembro de tipo de documento " +
                        miembroRequest.getTipoDocumento() + " y nro de documento "+ miembroRequest.getDocumento()));

        if (organizacion.getMiembros().stream().anyMatch(mi -> mi.getId().equals(miembro.getId()))) {
            throw new MiHuellaApiException("El miembro ya trabaja en la organización");
        }

        sector.agregarMiembro(miembro);
        this.repoOrganizaciones.agregar(organizacion);
        response.status(HttpStatus.CREATED_201);
        return response;
    }
}
