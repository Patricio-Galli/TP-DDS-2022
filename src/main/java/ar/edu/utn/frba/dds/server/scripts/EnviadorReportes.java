package ar.edu.utn.frba.dds.server.scripts;

import ar.edu.utn.frba.dds.entities.lugares.Organizacion;
import ar.edu.utn.frba.dds.entities.lugares.AreaSectorial;
import ar.edu.utn.frba.dds.entities.lugares.Municipio;
import ar.edu.utn.frba.dds.entities.lugares.Provincia;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.FechaException;
import ar.edu.utn.frba.dds.entities.personas.Miembro;
import ar.edu.utn.frba.dds.interfaces.mappers.OrganizacionMapper;
import ar.edu.utn.frba.dds.interfaces.mappers.TransportesMapper;
import ar.edu.utn.frba.dds.interfaces.mappers.TrayectosMapper;
import ar.edu.utn.frba.dds.interfaces.input.json.OrganizacionConMiembrosJSONDTO;
import ar.edu.utn.frba.dds.interfaces.input.csv.TramoCSVDTO;
import ar.edu.utn.frba.dds.interfaces.input.json.TransporteJSONDTO;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaOrganizacion;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaReportes;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaTrayectos;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.NoExisteMedioException;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserCSV;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserJSON;
import ar.edu.utn.frba.dds.entities.personas.AgenteSectorial;
import ar.edu.utn.frba.dds.entities.personas.ContactoMail;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.servicios.fachadas.NotificadorReportesMail;
import ar.edu.utn.frba.dds.entities.transportes.MedioDeTransporte;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnviadorReportes {
    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newFor("Checksum").build()
                .defaultHelp(true)
                .description("Calculate checksum of given files.");
        parser.addArgument("-o", "--organizaciones").required(true)
                .help("Archivo de organizaciones");
        parser.addArgument("-t", "--trayectos").required(true)
                .help("Archivo de trayectos");
        parser.addArgument("-T", "--transportes").required(true)
                .help("Archivo de transportes");
        parser.addArgument("-p", "--params").required(true)
                .help("Archivo con parámetros de configuración");
        parser.addArgument("-pw", "--password").required(true)
                .help("Archivo con parámetros de configuración");
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        System.out.println("Archivo de parametros: " + ns.get("params"));
        System.out.println("Archivo de organizaciones: " + ns.get("organizaciones"));
        System.out.println("Archivo de trayectos: " + ns.get("trayectos"));
        System.out.println("Archivo de transportes: " + ns.get("transportes"));

        FachadaOrganizacion fachadaOrganizacion = new FachadaOrganizacion();
        Map<String,Float> factoresDeEmision;
        List<Organizacion> organizaciones;

        try {
            factoresDeEmision = cargarFE(ns.getString("params"));
            organizaciones = cargarOrganizaciones(ns.getString("organizaciones"));
            cargarTransportes(ns.getString("transportes"));
            cargarTrayectos(ns.getString("trayectos"));
        } catch (IOException | FechaException | NoExisteMedioException ex) {
            System.out.println(ex.getMessage());
            return;
        }

        fachadaOrganizacion.cargarParametros(factoresDeEmision);

        Integer anio = 2020;//TODO
        Integer mes = 10;

        for(String categoria : factoresDeEmision.keySet()) {
            System.out.println(categoria + " -> " + factoresDeEmision.get(categoria));
        }

        AreaSectorial areaReporte = new Municipio("Ciudad de Buenos Aires", new Provincia("Ciudad de Buenos Aires", "Argentina"));
        organizaciones.get(0).agregarContactoMail(new ContactoMail("cuentaejemplodds2@gmail.com"));
        organizaciones.get(1).agregarContactoMail(new ContactoMail("rumplestilskink@gmail.com"));

//        areaReporte.agregarOrganizacion(organizaciones.get(0));
//        areaReporte.agregarOrganizacion(organizaciones.get(1));

        AgenteSectorial agente = new AgenteSectorial(areaReporte);
        areaReporte.setAgente(agente);
//        agente.setContactoMail(new ContactoMail("cuentaejemplodds2", ""));
        agente.setMail(new ContactoMail("cuentaejemplodds1", ns.getString("password")));
        FachadaReportes fachadaReportes = new FachadaReportes();
        fachadaReportes
                .setNotificador(new NotificadorReportesMail())
                .generarReporteAgente(areaReporte, anio, mes);
    }


    public static Map<String,Float> cargarFE(String archFE) throws IOException {
//        Map<String,Float> factoresDeEmision = new ParserParametrosCSV().generarFE(archFE); //TODO parser-mapper
//        return factoresDeEmision;
        return null;
    }

    public static List<Organizacion> cargarOrganizaciones(String archOrg) throws IOException, ParseException {
        List<Organizacion> organizaciones = new ArrayList<>();

//        List<OrganizacionConMiembrosJSONDTO> organizacionesDTO = ParserOrganizacionesJSON.generarOrganizaciones(archOrg);
        List<OrganizacionConMiembrosJSONDTO> organizacionesDTO = new ParserJSON<>(OrganizacionConMiembrosJSONDTO.class).parseFileToCollection(archOrg);

        Repositorio<Miembro> repoMiembros = FactoryRepositorio.getByParameterType(Miembro.class);

        for(OrganizacionConMiembrosJSONDTO organizacionDTO : organizacionesDTO){
            Organizacion unaOrg = OrganizacionMapper.toEntity(organizacionDTO);
            for(Miembro unMiembro : unaOrg.getMiembros()){
                repoMiembros.agregar(unMiembro);
            }
            organizaciones.add(unaOrg);
        }

        return organizaciones;
    }

    public static void cargarTransportes(String archTrans) throws IOException {
        //List<MedioDeTransporte> mediosDeTransporte = new ArrayList<>();

        List<TransporteJSONDTO> transportesDTO = new ParserJSON<>(TransporteJSONDTO.class).parseFileToCollection(archTrans);
        Repositorio<MedioDeTransporte> repoMedios = FactoryRepositorio.getByParameterType(MedioDeTransporte.class);

        for(TransporteJSONDTO transporteDTO : transportesDTO){
            MedioDeTransporte unTransporte = TransportesMapper.toEntity(transporteDTO);
            //mediosDeTransporte.add(unTransporte);
            repoMedios.agregar(unTransporte);
        }
    }

    public static void cargarTrayectos(String archTray) throws IOException {
        FachadaTrayectos fachadaTrayectos = new FachadaTrayectos();

        List<TramoCSVDTO> trayectosDTO = new ParserCSV<>(TramoCSVDTO.class).parseFileToCollection(archTray);

        trayectosDTO.forEach(tr -> {
            boolean esCompartidoPasivo = tr.getTrayectoId().equals("0");
            if(!esCompartidoPasivo) {
                fachadaTrayectos.cargarTrayectoActivo(TrayectosMapper.toNuevoTrayectoDTO(tr));
            } else {
                fachadaTrayectos.cargarTrayectoPasivo(TrayectosMapper.toTrayectoCompartidoDTO(tr));
            }
        });

        fachadaTrayectos.mostrarTrayectos();
    }
}