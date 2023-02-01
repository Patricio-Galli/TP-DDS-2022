package ar.edu.utn.frba.dds.server.scripts;

import ar.edu.utn.frba.dds.entities.medibles.BatchMediciones;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.FechaException;
import ar.edu.utn.frba.dds.entities.medibles.Medicion;
import ar.edu.utn.frba.dds.interfaces.mappers.FactorEmisionMapper;
import ar.edu.utn.frba.dds.interfaces.mappers.MedicionMapper;
import ar.edu.utn.frba.dds.interfaces.input.csv.FactorEmisionCSVDTO;
import ar.edu.utn.frba.dds.interfaces.input.csv.MedicionCSVDTO;
import ar.edu.utn.frba.dds.servicios.fachadas.FachadaOrganizacion;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.MedicionSinFactorEmisionException;
import ar.edu.utn.frba.dds.interfaces.input.parsers.ParserCSV;
import ar.edu.utn.frba.dds.entities.medibles.Medible;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CalculadorHU {

    public static void main(String[] args) {

        ArgumentParser parser = ArgumentParsers.newFor("Checksum").build()
                .defaultHelp(true)
                .description("Calculate checksum of given files.");
        parser.addArgument("-m", "--mediciones").required(true)
                .help("Archivo de mediciones");
        parser.addArgument("-p", "--params").required(true)
                .help("Archivo con parámetros de configuración");
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        System.out.println("Archivo de mediciones: " + ns.get("mediciones"));
        System.out.println("Archivo de parametros: " + ns.get("params"));

        Map<String,Float> factoresDeEmision;
        Repositorio<Medicion> repoMediciones = FactoryRepositorio.getByParameterType(Medicion.class);
        List<Medicion> mediciones;
        try {
            factoresDeEmision = cargarFE(ns.getString("params"));
            mediciones = cargarMediciones(ns.getString("mediciones"));
        } catch (IOException | FechaException ex) {
            System.out.println(ex.getMessage());
            return;
        }

        mediciones.forEach(repoMediciones::agregar);

        FachadaOrganizacion calculadora = new FachadaOrganizacion();
        calculadora.cargarParametros(factoresDeEmision);

        Repositorio<BatchMediciones> repoBatch = FactoryRepositorio.getByParameterType(BatchMediciones.class);
        repoBatch.agregar(new BatchMediciones(mediciones, LocalDate.now()));

        Float hcOrg;
        List<Medible> medibles = new ArrayList<>(mediciones);
        try {
            hcOrg = calculadora.obtenerHU(medibles);
        } catch(MedicionSinFactorEmisionException ex) {
            System.out.println("No hay factor de emision definido para la categoria '" + ex.getCategoria() + "'");
            return;
        }

        System.out.println("La huella de carbono correspondiente a las mediciones ingresadas es: " + hcOrg);
        System.exit(0);
    }

    public static Map<String,Float> cargarFE(String archFE) throws IOException {
        List<FactorEmisionCSVDTO> factoresDeEmision = new ParserCSV<>(FactorEmisionCSVDTO.class)
                .parseFileToCollection(archFE);

        Map<String,Float> mapaFactores = factoresDeEmision.stream()
                .map(FactorEmisionMapper::toEntry)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return mapaFactores;
    }

    public static List<Medicion> cargarMediciones(String archMed) throws IOException {
        List<MedicionCSVDTO> medicionesDTO = new ParserCSV<>(MedicionCSVDTO.class).parseFileToCollection(archMed);
        return medicionesDTO.stream().map(MedicionMapper::toEntity).collect(Collectors.toList());
    }
}