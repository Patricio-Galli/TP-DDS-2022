package ar.edu.utn.frba.dds.interfaces.controllers;

import ar.edu.utn.frba.dds.entities.lugares.AreaSectorial;
import ar.edu.utn.frba.dds.entities.lugares.Organizacion;
import ar.edu.utn.frba.dds.entities.personas.AgenteSectorial;
import ar.edu.utn.frba.dds.entities.personas.ContactoMail;
import ar.edu.utn.frba.dds.entities.personas.ContactoTelefono;
import ar.edu.utn.frba.dds.interfaces.gui.mappers.OrganizacionMapperHBS;
import ar.edu.utn.frba.dds.interfaces.input.ErrorDTO;
import ar.edu.utn.frba.dds.interfaces.input.json.AgenteSectorialJSONDTO;
import ar.edu.utn.frba.dds.interfaces.input.json.NuevaEntidadGenericaRequest;
import ar.edu.utn.frba.dds.interfaces.input.json.NuevaEntidadResponse;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserJSON;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.server.login.User;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaUsuarios;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.MiHuellaApiException;
import org.eclipse.jetty.http.HttpStatus;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import javax.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AgenteSectorialController {
    private final Repositorio<AgenteSectorial> repoAgentes;
    private final Repositorio<AreaSectorial> repoAreas;
    private final Repositorio<Organizacion> repoOrganizaciones;
    private final FachadaUsuarios fachadaUsuarios;

    public AgenteSectorialController() {
        this.repoAgentes = FactoryRepositorio.getByParameterType(AgenteSectorial.class);
        this.repoAreas = FactoryRepositorio.getByParameterType(AreaSectorial.class);
        this.repoOrganizaciones = FactoryRepositorio.getByParameterType(Organizacion.class);
        this.fachadaUsuarios = new FachadaUsuarios();
    }

    public String mostrarTodos(Request request, Response response) {
        /*if (loginController.chequearValidezAcceso(request, response, true) != null){
            return loginController.chequearValidezAcceso(request, response, true);
        }    Todo esto agregar una vez que tengamos la vista*/
        List<AgenteSectorial> agentes = this.repoAgentes.buscarTodos();
        return agentes.toString();
    }

    public AgenteSectorial obtener(Request request, Response response) {
        return this.repoAgentes.buscar(Integer.parseInt(request.params("id")))
                .orElseThrow(EntityNotFoundException::new);
    }

    public Object agregar(Request request, Response response) {
        NuevaEntidadResponse res = new NuevaEntidadResponse();

        NuevaEntidadGenericaRequest<AgenteSectorialJSONDTO> agenteDTO =
                new ParserJSON<>(NuevaEntidadGenericaRequest.class, AgenteSectorialJSONDTO.class)
                        .parseBounded(request.body());

        try {
            fachadaUsuarios.validarRequest(agenteDTO.getUsuario());
        } catch (MiHuellaApiException e) {
            res.setEstado("ERROR");
            res.setError(e.getError());
            return res;
        }

        AreaSectorial area;
        try {
            area = this.repoAreas.buscar(agenteDTO.getEntidad().getArea())
                    .orElseThrow(EntityNotFoundException::new);
        } catch (EntityNotFoundException e) {
            response.status(HttpStatus.CONFLICT_409);
            res.setEstado("ERROR");
            res.setError(new ErrorDTO("ERROR_DE_DOMINIO", "Ne existe el área sectorial mencionada"));
            return res;
        }

        AgenteSectorial agenteSectorial = new AgenteSectorial(area,
                new ContactoMail(agenteDTO.getEntidad().getContactoMail().getDireccionesEMail(),
                        agenteDTO.getEntidad().getContactoMail().getPassword()),
                new ContactoTelefono(agenteDTO.getEntidad().getTelefono()));

        this.repoAgentes.agregar(agenteSectorial);

        User usuario = this.fachadaUsuarios.agregar(new User(agenteDTO.getUsuario().getUsername(),
                agenteDTO.getUsuario().getPassword(), agenteSectorial));

        res.setEstado("OK");
        res.setEntidad(agenteSectorial.getId());
        res.setUsuario(usuario.getId());
        return res;
    }

    public Object modificar(Request request, Response response){
        /*if (loginController.chequearValidezAcceso(request, response, true) != null){
            return loginController.chequearValidezAcceso(request, response, true);
        }    Todo esto agregar una vez que tengamos la vista*/
        AgenteSectorialJSONDTO agenteDTO = new ParserJSON<>(AgenteSectorialJSONDTO.class).parseElement(request.body());

        AgenteSectorial agenteSectorial = this.repoAgentes.buscar(Integer.parseInt(request.params("id")))
                .orElseThrow(EntityNotFoundException::new);
        agenteSectorial.setMail(new ContactoMail(agenteDTO.getContactoMail().getDireccionesEMail(), agenteDTO.getContactoMail().getPassword()));
        agenteSectorial.setTelefono(new ContactoTelefono(agenteDTO.getTelefono()));

        this.repoAgentes.modificar(Integer.parseInt(request.params("id")),agenteSectorial);
        return "Agente Sectorial modificado correctamente";
    }

    public Object eliminar(Request request, Response response){
        /*if (loginController.chequearValidezAcceso(request, response, true) != null){
            return loginController.chequearValidezAcceso(request, response, true);
        }    Todo esto agregar una vez que tengamos la vista*/
        AgenteSectorial agenteSectorial = this.repoAgentes.buscar(Integer.parseInt(request.params("id")))
                .orElseThrow(EntityNotFoundException::new);
        this.repoAgentes.eliminar(agenteSectorial);
        return "Agente Sectorial eliminado correctamente";
    }

    public ModelAndView mostrarOrganizaciones(Request request, Response response) {
        Map<String, Object> parametros = new HashMap<>();
        Integer idAgente = Integer.parseInt(request.params("id"));
        AgenteSectorial agente = this.repoAgentes.buscar(idAgente)
                .orElseThrow(EntityNotFoundException::new);

        parametros.put("rol", "AGENTE"); //todo ver si poner como el menu
        parametros.put("user", agente.getMail().getDireccion()); //todo agregar nombre en agente?
        parametros.put("agenteID", idAgente);

//        List<Organizacion> orgs = repoOrganizaciones.buscarTodos().stream().filter(o -> agente.getArea().getUbicaciones().contains(o.getUbicacion())).collect(Collectors.toList());
//        List<Organizacion> orgs = repoOrganizaciones.buscarTodos();
        Set<Organizacion> orgs = agente.getArea().getOrganizaciones();
        parametros.put("organizaciones", orgs.stream().map(OrganizacionMapperHBS::toDTOUbicacion).collect(Collectors.toList()));
//        parametros.put("organizaciones", agente.getArea().getOrganizaciones().stream().map(OrganizacionMapperHBS::toDTOUbicacion).collect(Collectors.toList()));
        return new ModelAndView(parametros, "organizaciones.hbs");
    }
}
