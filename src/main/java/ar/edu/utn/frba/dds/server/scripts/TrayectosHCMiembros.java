package ar.edu.utn.frba.dds.server.scripts;

import ar.edu.utn.frba.dds.entities.lugares.Organizacion;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.FechaException;
import ar.edu.utn.frba.dds.entities.medibles.Periodo;
import ar.edu.utn.frba.dds.entities.transportes.MedioDeTransporte;
import ar.edu.utn.frba.dds.entities.medibles.Tramo;
import ar.edu.utn.frba.dds.entities.medibles.Trayecto;
import ar.edu.utn.frba.dds.interfaces.mappers.FactorEmisionMapper;
import ar.edu.utn.frba.dds.interfaces.mappers.OrganizacionMapper;
import ar.edu.utn.frba.dds.interfaces.mappers.TransportesMapper;
import ar.edu.utn.frba.dds.interfaces.mappers.TrayectosMapper;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaOrganizacion;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaTrayectos;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.NoExisteMedioException;
import ar.edu.utn.frba.dds.interfaces.input.csv.FactorEmisionCSVDTO;
import ar.edu.utn.frba.dds.interfaces.input.csv.TramoCSVDTO;
import ar.edu.utn.frba.dds.interfaces.input.json.OrganizacionConMiembrosJSONDTO;
import ar.edu.utn.frba.dds.interfaces.input.json.TransporteJSONDTO;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserCSV;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserJSON;
import ar.edu.utn.frba.dds.entities.personas.Miembro;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class TrayectosHCMiembros {

    private static final String SALIDA_1_PATH = "resources/salida1.csv"/*"src/main/resources/salida1.csv"*/;

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
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            exit(1);
        }

        System.out.println("Archivo de parametros: " + ns.get("params"));
        System.out.println("Archivo de organizaciones: " + ns.get("organizaciones"));
        System.out.println("Archivo de trayectos: " + ns.get("trayectos"));
        System.out.println("Archivo de transportes: " + ns.get("transportes"));

        FactoryRepositorio.getByParameterType(Trayecto.class).buscarTodos().forEach(System.out::println);
        FactoryRepositorio.getByParameterType(Tramo.class).buscarTodos().forEach(System.out::println);

        try {
            cargarFE(ns.getString("params"));
            cargarOrganizaciones(ns.getString("organizaciones"));
            cargarTransportes(ns.getString("transportes"));
            cargarTrayectos(ns.getString("trayectos"));
        } catch (IOException | FechaException | NoExisteMedioException ex) {
            System.out.println(ex.getMessage());
            exit(0);
        }

        //SALIDA 1
        PrintWriter writer = new PrintWriter(SALIDA_1_PATH, "UTF-8");
        writer.println("Anio, Mes, Razon Social, DNI, Impacto");

        Integer anio = 2020; //TODO
        int mes = 10;

        FachadaOrganizacion fachada = new FachadaOrganizacion();
        fachada.mostrarParametros();

        List<Organizacion> organizaciones = FactoryRepositorio.getByParameterType(Organizacion.class).buscarTodos();

        for(Organizacion org : organizaciones) {
            String razonSocial = org.getRazonSocial();
            Set<Miembro> miembros = org.getMiembros();

            Float consumoTotalOrganizacion = fachada.calcularImpactoOrganizacion(org, new Periodo(anio, mes));
            for (Miembro miembro : miembros) {
                Integer documento = miembro.getNroDocumento();
                System.out.println("MiembroID: " + documento);
                float impacto = 100 * fachada.calcularImpactoTrayectos(miembro, new Periodo(anio, mes)) / consumoTotalOrganizacion;
                writer.println(anio + ", " + mes + ", " + razonSocial + ", " + documento + ", " + impacto);
                System.out.println(anio + ", " + mes + ", " + razonSocial + ", " + documento + ", " + impacto + '\n');
            }
        }
        writer.close();
        exit(0);
    }

    public static void cargarFE(String archFE) throws IOException {
        FachadaOrganizacion fachada = new FachadaOrganizacion();
        List<FactorEmisionCSVDTO> factoresDeEmision = new ParserCSV<>(FactorEmisionCSVDTO.class)
                .parseFileToCollection(archFE);

        fachada.cargarParametros(
                factoresDeEmision.stream()
                .map(FactorEmisionMapper::toEntry)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    public static void cargarOrganizaciones(String archOrg) throws IOException {
        Repositorio<Organizacion> repoOrganizacion = FactoryRepositorio.getByParameterType(Organizacion.class);

        List<OrganizacionConMiembrosJSONDTO> organizacionesDTO =
                new ParserJSON<>(OrganizacionConMiembrosJSONDTO.class).parseFileToCollection(archOrg);

        organizacionesDTO.forEach(organizacionDTO ->
            repoOrganizacion.agregar(OrganizacionMapper.toEntity(organizacionDTO)));
    }

//    public static List<Organizacion> cargarOrganizaciones(String archOrg) throws IOException {
//        RepoMiembros repoMiembros = (RepoMiembros) FactoryRepositorio.get(Miembro.class);
//
//        List<OrganizacionJSONDTO> organizacionesDTO = new ParserJSON<>(OrganizacionJSONDTO.class).parseFileToCollection(archOrg);
//
//        return organizacionesDTO.stream().map(organizacionDTO -> {
//            Organizacion unaOrg = OrganizacionMapper.toEntity(organizacionDTO);
//
//            unaOrg.getSectores().forEach(sector -> {
//                sector.getListaDeMiembros().forEach(unMiembro -> {
//                    Optional<Miembro> miembro = repoMiembros
//                            .findByDocumento(unMiembro.getTipoDeDocumento(), unMiembro.getNroDocumento());
//                    if(miembro.isPresent()) {
//                        sector.agregarMiembro(miembro.get());
//                    } else {
//                        throw new MiembroException();
//                    }
//                });
//            });
//            return unaOrg;
//        }).collect(Collectors.toList());
//    }

    public static void cargarTransportes(String archTrans) throws IOException {
        List<TransporteJSONDTO> transportesDTO = new ParserJSON<>(TransporteJSONDTO.class).parseFileToCollection(archTrans);
        Repositorio<MedioDeTransporte> repoMedios = FactoryRepositorio.getByParameterType(MedioDeTransporte.class);

        for(TransporteJSONDTO transporteDTO : transportesDTO){
            MedioDeTransporte unTransporte = TransportesMapper.toEntity(transporteDTO);
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