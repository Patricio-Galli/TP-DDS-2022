package ar.edu.utn.frba.dds.server;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SystemProperties {
    private static final Boolean jpa;
    private static final Float delta;
    private static final Float coeficienteGradoKm;
    private static final Boolean calculadoraDistanciasMockEnabled;
    private static final Boolean calculadoraDistanciasCacheEnabled;
    private static final String calculadoraDistanciasUrl;
    private static final String calculadoraDistanciasToken;
    private static final Boolean localhost;
    private static final String staticRelativePath;
    private static final String staticBasePath;
    private static final String staticDomainPath;
    private static final String staticAbsolutePath;
    private static final String apiUrl;
    private static final String repositoryPath;

    static {
        Map<String, String> varEntorno = System.getenv();
        Properties propArchivo = cargarArchivoConfigurable();
        System.out.println("aplication (entorno): " + varEntorno);
        jpa = varEntorno.getOrDefault("jpa", propArchivo.getProperty("jpa", "true")).equals("true");
        delta = Float.parseFloat(
                varEntorno.getOrDefault("coordenadas_precision_delta",
                        propArchivo.getProperty("coordenadas_precision_delta", "0.00001")));
        coeficienteGradoKm = Float.parseFloat(
                varEntorno.getOrDefault("coordenadas_precision_equivalencia",
                        propArchivo.getProperty("coordenadas_precision_equivalencia", "111.10")));
        calculadoraDistanciasMockEnabled = varEntorno.getOrDefault("client_calculadora_distancias_mock_enabled",
                propArchivo.getProperty("client_calculadora_distancias_mock_enabled", "true")).equals("true");
        calculadoraDistanciasCacheEnabled = varEntorno.getOrDefault("client_calculadora_distancias_cache_enabled",
                propArchivo.getProperty("client_calculadora_distancias_cache_enabled", "false")).equals("true");
        calculadoraDistanciasUrl = varEntorno.getOrDefault("client_calculadora_distancias_api_url",
                propArchivo.getProperty("client_calculadora_distancias_api_url", "https://ddstpa.com.ar/api/"));
        calculadoraDistanciasToken = varEntorno.getOrDefault("client_calculadora_distancias_api_token",
                propArchivo.getProperty("client_calculadora_distancias_api_token", ""));
        localhost = varEntorno.getOrDefault("localhost", propArchivo.getProperty("localhost", "true")).equals("true");
        staticRelativePath = varEntorno.getOrDefault("static_path_relative", propArchivo.getProperty("static_path_relative", "/public"));
        staticBasePath = varEntorno.getOrDefault("static_path_base", propArchivo.getProperty("static_path_base", "/src/main/resources"));
        staticDomainPath = varEntorno.getOrDefault("static_path_domain", propArchivo.getProperty("static_path_domain", System.getProperty("user.dir")));
        staticAbsolutePath = varEntorno.getOrDefault("static_path_absolute", propArchivo.getProperty("static_path_absolute", staticDomainPath + staticBasePath + staticRelativePath));
        apiUrl = varEntorno.getOrDefault("api_url", propArchivo.getProperty("api_url", "https://app.swaggerhub.com/apis-docs/SebasCafiero/dds-mano-g06/2.0"));
        repositoryPath = varEntorno.getOrDefault("mihuella_framework_repository_path", propArchivo.getProperty("mihuella_framework_repository_path", "ar.edu.utn.frba.dds.repositories.impl"));
    }

    public static Boolean isJpa() {
        return jpa;
    }

    public static Float getDelta() {
        return delta;
    }

    public static Float getCoeficienteGradoKm() {
        return coeficienteGradoKm;
    }

    public static Boolean isCalculadoraDistanciasMockEnabled() {
        return calculadoraDistanciasMockEnabled;
    }

    public static Boolean isCalculadoraDistanciasCacheEnabled() {
        return calculadoraDistanciasCacheEnabled;
    }

    public static String getCalculadoraDistanciasUrl() {
        return calculadoraDistanciasUrl;
    }

    public static String getCalculadoraDistanciasToken() {
        return calculadoraDistanciasToken;
    }

    public static Boolean isLocalhost() {
        return localhost;
    }

    public static String getStaticRelativePath() {
        return staticRelativePath;
    }

    public static String getStaticAbsolutePath() {
        return staticAbsolutePath;
    }

    public static String getStaticBasePath() {
        return staticBasePath;
    }

    public static String getStaticDomainPath() {
        return staticDomainPath;
    }

    public static String getRepositoryPath() {
        return repositoryPath;
    }

    public static String getApiUrl() {
        return apiUrl;
    }

    private static Properties cargarArchivoConfigurable() {
        Properties propiedades = new Properties();
        String path = "resources/aplication.properties";
//        File archivo = null;
        try {
//            archivo = new File(path);
            FileReader file = new FileReader(path);
            propiedades.load(file);

            System.out.println("aplication.properties: " + propiedades);

            file.close();
        } catch (FileNotFoundException e) {
            System.out.println("No existe el archivo " + path);
//            System.out.println(archivo.getAbsolutePath());
//            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("IO Excepcion!");
            throw new RuntimeException(e);
        }
        return propiedades;
    }
}
