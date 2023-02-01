package ar.edu.utn.frba.dds.interfaces.controllers;

import ar.edu.utn.frba.dds.entities.personas.AgenteSectorial;
import ar.edu.utn.frba.dds.interfaces.gui.GuiUtils;
import ar.edu.utn.frba.dds.interfaces.gui.dto.AgenteHBS;
import ar.edu.utn.frba.dds.interfaces.gui.mappers.AgenteMapperHBS;
import ar.edu.utn.frba.dds.interfaces.gui.mappers.OrganizacionMapperHBS;
import ar.edu.utn.frba.dds.interfaces.gui.mappers.ReporteMapperHBS;
import ar.edu.utn.frba.dds.entities.lugares.Organizacion;
import ar.edu.utn.frba.dds.entities.lugares.Sector;
import ar.edu.utn.frba.dds.entities.lugares.AreaSectorial;
import ar.edu.utn.frba.dds.entities.medibles.Categoria;
import ar.edu.utn.frba.dds.entities.medibles.Periodo;
import ar.edu.utn.frba.dds.entities.medibles.ReporteOrganizacion;
import ar.edu.utn.frba.dds.entities.personas.Miembro;
import ar.edu.utn.frba.dds.server.SystemProperties;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaReportes;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportesController {
    private final FachadaReportes fachadaReportes;
    private final Repositorio<AreaSectorial> repoAreas;
    private final Repositorio<Organizacion> repoOrganizaciones;
    private final Repositorio<AgenteSectorial> repoAgentes;

    public ReportesController() {
        this.fachadaReportes = new FachadaReportes();
        this.repoAreas = FactoryRepositorio.getByParameterType(AreaSectorial.class);
        this.repoOrganizaciones = FactoryRepositorio.getByParameterType(Organizacion.class);
        this.repoAgentes = FactoryRepositorio.getByParameterType(AgenteSectorial.class);
    }

    public Object generarReporteAgente(Request request, Response response) {
        int idArea = Integer.parseInt(request.params(("id")));
        AgenteSectorial areaSectorial = repoAgentes.buscar(idArea).get();

        LocalDate fecha = LocalDate.now();
        return fachadaReportes
                .generarReporteAgente(areaSectorial.getArea(), fecha.getYear(), fecha.getMonthValue());
    }

    public Object generarReporteOrganizacion(Request request, Response response) {
        int idOrganizacion = Integer.parseInt(request.params(("id")));
        Organizacion organizacion = repoOrganizaciones.buscar(idOrganizacion).get();

        LocalDate fecha = LocalDate.now();
        return fachadaReportes
                .generarReporteOrganizacion(organizacion, new Periodo(fecha.getYear(), fecha.getMonthValue()));
    }

    public ModelAndView darAltaYMostrarOrg(Request request, Response response) {
        return darAltaYMostrar(request, "organizacion");
    }

    public ModelAndView darAltaYMostrarAgente(Request request, Response response) {
        return darAltaYMostrar(request, "agente");
    }

    public ModelAndView darAltaYMostrar(Request request, String rol) {
        Map<String, Object> parametros = GuiUtils.dtoHeader(request);

        Organizacion org;
        int id;
        if(rol.equals("organizacion")) {
            id = Integer.parseInt(request.params("id"));
            org = repoOrganizaciones.buscar(id).get(); //deberia coincidir con los permisos del usuario
//            parametros.put("rol", "ORGANIZACION");
//            parametros.put("user", org.getRazonSocial());
            parametros.put("organizacion", OrganizacionMapperHBS.toDTO(org));
        }

        AgenteSectorial agente;
        if(rol.equals("agente")) {
            id = Integer.parseInt(request.params("id"));
            agente = this.repoAgentes.buscar(id).get();

//            parametros.put("rol", "AGENTE"); //todo ver si poner como el menu
//            parametros.put("user", agente.getMail().getDireccion()); //todo agregar nombre en agente?
            AgenteHBS agenteDTO = AgenteMapperHBS.toDTO(agente);
//            List<Organizacion> orgs = repoOrganizaciones.buscarTodos().stream().filter(o -> agente.getArea().getUbicaciones().contains(o.getUbicacion())).collect(Collectors.toList());
            Set<Organizacion> orgs = agente.getArea().getOrganizaciones();
            agenteDTO.setOrganizaciones(orgs.stream().map(OrganizacionMapperHBS::toDTO).collect(Collectors.toList()));
            parametros.put("agente", agenteDTO);
            //todo quizas agregar reporte de agente (todas las organizaciones en uno)
            if(request.queryParams("org") != null)
                parametros.put("organizacion", OrganizacionMapperHBS.toDTO(repoOrganizaciones.buscar(Integer.parseInt(request.queryParams("org"))).get()));
        }

        ReporteOrganizacion reporte = fachadaReportes.getReporteOrganizacion();
        if(reporte != null) {
            parametros.put("reporte", ReporteMapperHBS.toDTO(reporte));

            DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyyMMdd");
            String arch = reporte.getOrganizacion().getRazonSocial().toLowerCase().replaceAll("\\s","") + reporte.getFechaCreacion().format(formato);
            String ruta = "/docs/" + arch + ".txt";
//            File file = new File(System.getProperty("user.dir") + SystemProperties.getStaticBasePath() + SystemProperties.getStaticRelativePath() + ruta);
            File file = new File(SystemProperties.getStaticAbsolutePath() + ruta);
            if(file.canRead()) {
                System.out.println("Existe el archivo para descargar: " + file.getAbsolutePath());
                parametros.put("file", ruta);
            } else {
                System.out.println("No Existe el archivo para descargar: " + file.getAbsolutePath());
//                parametros.put("file", ruta);
            }
            fachadaReportes.quitarReporteOrganizacion();
        }

        return new ModelAndView(parametros, "reporte.hbs");
    }

    public Response generarOrg(Request request, Response response) {
        return generar(request, response, "organizacion");
    }

    public Response generarAgente(Request request, Response response) {
        return generar(request, response, "agente");
    }

    public Response generar(Request request, Response response, String rol) {
        /*String fechaActual = LocalDate.now().getMonthValue()+"/"+LocalDate.now().getYear();
        String[] fecha = request.queryParamOrDefault("f-fecha", fechaActual).split("/"); //todo validar fecha
        Periodo periodo;
        if(fecha.length > 1)
            periodo = new Periodo(Integer.parseInt(fecha[1]), Integer.parseInt(fecha[0]));
        else
            periodo = new Periodo(Integer.parseInt(fecha[0]));*/
        Periodo periodo = fachadaReportes.parsearPeriodo(request.queryParams("f-fecha"));
        Organizacion organizacion = null;
        String ruta = "";
        int id = Integer.parseInt(request.params("id"));
        if(rol.equals("organizacion")) {
            organizacion = repoOrganizaciones.buscar(id).get();
            ruta = "/organizacion/"+organizacion.getId()+"/reporte#reporte";
        }

        if(rol.equals("agente")) {
            AgenteSectorial agente = this.repoAgentes.buscar(id).get();
            int idOrg = Integer.parseInt(request.queryParams("f-organizacion"));

            organizacion = repoOrganizaciones.buscar(idOrg).get();
            ruta = "/agente/"+agente.getId()+"/reporte?org="+idOrg+"#reporte";
        }
        repoOrganizaciones.sync(organizacion);
        fachadaReportes.generarReporteOrganizacion(organizacion, periodo);
        documentarReporte(fachadaReportes.getReporteOrganizacion(), organizacion);
        response.redirect(ruta);

        return response;
    }

    private String documentarReporte(ReporteOrganizacion reporte, Organizacion organizacion) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyyMMdd");
        String arch = organizacion.getRazonSocial().toLowerCase().replaceAll("\\s","") + reporte.getFechaCreacion().format(formato) + ".txt";
//        String ruta = "resources/public/docs/" + arch;
//        String ruta = "src/main/resources/public/docs/" + arch;
        String ruta = SystemProperties.getStaticAbsolutePath() + "/docs/" + arch;
        try {
            PrintWriter writer = new PrintWriter(ruta, "UTF-8");
            writer.println("Fecha de Creacion: " + reporte.getFechaCreacion());
            String periodo = reporte.getPeriodoReferencia().getAnio().toString();
            if(reporte.getPeriodoReferencia().getPeriodicidad() == 'M')
                periodo += "-" + reporte.getPeriodoReferencia().getMes().toString();
            writer.println("Periodo de Referencia: " + periodo);
            writer.println("Consumo Total: " + reporte.getConsumoTotal());
            writer.println("Consumo Mediciones: " + reporte.getConsumoMediciones());
            writer.println("Consumo Trayectos: " + (reporte.getConsumoTotal() - reporte.getConsumoMediciones()));
            writer.println("\n");

            for (Map.Entry<Miembro, Float> miembro : reporte.getConsumoPorMiembro().entrySet()) {
                String info = miembro.getKey().getNombreCompleto() + " - " + miembro.getKey().getDocumento();
                writer.println("Miembro: " + info + " -> " + miembro.getValue());
            }

            for (Map.Entry<Sector, Float> sector : reporte.getConsumoPorSector().entrySet()) {
                writer.println("Sector: " + sector.getKey().getNombre() + " -> " + sector.getValue());
            }

            for(Map.Entry<Categoria, Float> categoria : reporte.getConsumoPorCategoria().entrySet()) {
                writer.println("Categoria: " + categoria.getKey().toString() + " -> " + categoria.getValue());
            }

            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            System.out.println("No se pudo crear el archivo " + ruta);
            e.printStackTrace();
            //throw new ReporteException("No se pudo crear el archivo del reporte.");
        }

        return arch;
    }
}
