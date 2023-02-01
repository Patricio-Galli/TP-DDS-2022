package ar.edu.utn.frba.dds.server.scripts;

import ar.edu.utn.frba.dds.entities.lugares.Organizacion;
import ar.edu.utn.frba.dds.entities.lugares.Sector;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.FechaException;
import ar.edu.utn.frba.dds.entities.medibles.Periodo;
import ar.edu.utn.frba.dds.entities.personas.Miembro;
import ar.edu.utn.frba.dds.entities.transportes.MedioDeTransporte;
import ar.edu.utn.frba.dds.interfaces.mappers.FactorEmisionMapper;
import ar.edu.utn.frba.dds.interfaces.mappers.OrganizacionMapper;
import ar.edu.utn.frba.dds.interfaces.mappers.TransportesMapper;
import ar.edu.utn.frba.dds.interfaces.mappers.TrayectosMapper;
import ar.edu.utn.frba.dds.interfaces.input.csv.FactorEmisionCSVDTO;
import ar.edu.utn.frba.dds.interfaces.input.json.OrganizacionConMiembrosJSONDTO;
import ar.edu.utn.frba.dds.interfaces.input.csv.TramoCSVDTO;
import ar.edu.utn.frba.dds.interfaces.input.json.TransporteJSONDTO;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaOrganizacion;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaTrayectos;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.NoExisteMedioException;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserCSV;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserJSON;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TrayectosHCSectores {

    private static final String SALIDA_2_PATH = "resources/salida2.csv";

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
            System.exit(1);
        }

        System.out.println("Archivo de parametros: " + ns.get("params"));
        System.out.println("Archivo de organizaciones: " + ns.get("organizaciones"));
        System.out.println("Archivo de trayectos: " + ns.get("trayectos"));
        System.out.println("Archivo de transportes: " + ns.get("transportes"));

        Map<String,Float> factoresDeEmision;
        List<Organizacion> organizaciones;
        FachadaOrganizacion fachada = new FachadaOrganizacion();

        try {
            factoresDeEmision = cargarFE(ns.getString("params"));
            organizaciones = cargarOrganizaciones(ns.getString("organizaciones"));
            cargarTransportes(ns.getString("transportes"));
            cargarTrayectos(ns.getString("trayectos"));
        } catch (IOException | FechaException | NoExisteMedioException ex) {
            System.out.println(ex.getMessage());
            return;
        }

        fachada.cargarParametros(factoresDeEmision);

        //SALIDA 2
        PrintWriter writer = new PrintWriter(SALIDA_2_PATH, "UTF-8");
        writer.println("Anio, Mes, Razon Social, Sector, Impacto/Cant Miembros");

        Integer anio = 2020; //TODO
        Integer mes = 10;

        for(Organizacion org : organizaciones){
            String razonSocial = org.getRazonSocial();
            Set<Sector> sectores = org.getSectores();
            Float consumoTotalOrganizacion = fachada.calcularImpactoOrganizacion(org, new Periodo(anio, mes));

            for(Sector sector : sectores){
                String nombreSector = sector.getNombre();
                System.out.println("Sector: " + nombreSector);
                float impacto = fachada.calcularImpactoTrayectos(sector, new Periodo(anio, mes)) / consumoTotalOrganizacion;
                writer.println(anio + ", " + mes + ", " + razonSocial + ", " + nombreSector + ", " + impacto);
                System.out.println(anio + ", " + mes + ", " + razonSocial + ", " + nombreSector + ", " + impacto + '\n');
            }
        }
        writer.close();
    }

    public static Map<String,Float> cargarFE(String archFE) throws IOException {
        List<FactorEmisionCSVDTO> factoresDeEmision = new ParserCSV<>(FactorEmisionCSVDTO.class)
                .parseFileToCollection(archFE);

        Map<String,Float> mapaFactores = factoresDeEmision.stream()
                .map(FactorEmisionMapper::toEntry)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return mapaFactores;
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
