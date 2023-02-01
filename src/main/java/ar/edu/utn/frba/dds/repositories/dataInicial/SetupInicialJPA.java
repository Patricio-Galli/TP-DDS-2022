package ar.edu.utn.frba.dds.repositories.dataInicial;

import ar.edu.utn.frba.dds.entities.lugares.*;
import ar.edu.utn.frba.dds.repositories.cache.CacheLocalidad;
import ar.edu.utn.frba.dds.entities.medibles.*;
import ar.edu.utn.frba.dds.entities.personas.*;
import ar.edu.utn.frba.dds.entities.transportes.*;
import ar.edu.utn.frba.dds.entities.medibles.Tramo;
import ar.edu.utn.frba.dds.entities.medibles.Trayecto;
import ar.edu.utn.frba.dds.server.login.User;
import ar.edu.utn.frba.dds.repositories.RepoFactores;
import ar.edu.utn.frba.dds.repositories.RepoOrganizaciones;
import ar.edu.utn.frba.dds.repositories.daos.Cache;
import ar.edu.utn.frba.dds.repositories.utils.FactoryCache;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.servicios.clients.calculadoraDistancias.ddstpa.AdaptadorServicioDDSTPA;
import ar.edu.utn.frba.dds.servicios.clients.calculadoraDistancias.ddstpa.dto.LocalidadGson;
import ar.edu.utn.frba.dds.servicios.clients.calculadoraDistancias.ddstpa.dto.MunicipioGson;
import ar.edu.utn.frba.dds.servicios.clients.calculadoraDistancias.ddstpa.dto.ProvinciaGson;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SetupInicialJPA {
    private final Repositorio<MedioDeTransporte> repoMedios;
    private final Repositorio<UbicacionGeografica> repoUbicaciones;
    private final RepoOrganizaciones repoOrganizaciones;
    private final Repositorio<Trayecto> repoTrayectos;
    private final Repositorio<AreaSectorial> repoAreas;
    private final Repositorio<AgenteSectorial> repoAgentes;
    private final Repositorio<Miembro> repoMiembros;
    private final Repositorio<Contacto> repoContactos;
    private final Repositorio<BatchMediciones> repoBatchMediciones;
    private final Repositorio<User> repoUsuarios;
    private final Cache<CacheLocalidad> cacheLocalidades;

    public SetupInicialJPA() {
        this.repoMedios = FactoryRepositorio.getByParameterType(MedioDeTransporte.class);
        this.repoUbicaciones = FactoryRepositorio.getByParameterType(UbicacionGeografica.class);
        this.repoOrganizaciones = FactoryRepositorio.getByOutputType(RepoOrganizaciones.class);
        this.repoTrayectos = FactoryRepositorio.getByParameterType(Trayecto.class);
        this.repoAreas = FactoryRepositorio.getByParameterType(AreaSectorial.class);
        this.repoAgentes = FactoryRepositorio.getByParameterType(AgenteSectorial.class);
        this.repoMiembros = FactoryRepositorio.getByParameterType(Miembro.class);
        this.repoUsuarios = FactoryRepositorio.getByParameterType(User.class);
        this.repoContactos = FactoryRepositorio.getByParameterType(Contacto.class);
        this.repoBatchMediciones = FactoryRepositorio.getByParameterType(BatchMediciones.class);
        this.cacheLocalidades = FactoryCache.get(CacheLocalidad.class);
    }

    public void doSetup() {
        // Factores de emision

        this.inicializarFactores();

        //  Ubicaciones, Agenstes Sectoriales y medios de transporte

        AdaptadorServicioDDSTPA adapter = new AdaptadorServicioDDSTPA();

        Provincia cabaProvincia = new Provincia("CIUDAD DE BUENOS AIRES", "ARGENTINA");
        Municipio cabaMunicipio = new Municipio("CIUDAD DE BUENOS AIRES", cabaProvincia);

        List<ProvinciaGson> provinciasGson = adapter.obtenerPaises().stream()
                .filter(pais -> pais.getNombre().equals("ARGENTINA"))
                .flatMap(pais -> adapter.obtenerProvincias(pais.getId()).stream())
                .collect(Collectors.toList());

        for(ProvinciaGson prov : provinciasGson) {
            Provincia provincia = new Provincia(prov.getNombre(), prov.getPais().getNombre());
            provincia.setIdApiDistancias(prov.getId());

            if(provincia.getNombre().equals(cabaProvincia.getNombre()))
                cabaProvincia = provincia;

            for(MunicipioGson muni : adapter.obtenerMunicipios(prov.getId())) {
                Municipio municipio = new Municipio(muni.getNombre(), provincia);
                municipio.setIdApiDistancias(muni.getId());
                System.out.println("Cargo municipio en cache: " + municipio.getNombre());

                if(municipio.getNombre().equals(cabaMunicipio.getNombre())) {
                    cabaMunicipio = municipio;

                    List<LocalidadGson> lista = adapter.obtenerLocalidades(muni.getId());
                    lista.forEach(loca -> {
                        System.out.println(loca.getNombre());
                        cacheLocalidades.put(loca.getNombre(),
                                new CacheLocalidad(muni.getProvincia().getId(), muni.getId(), loca.getId()));
                    });
                }
            }

            this.repoAreas.agregar(provincia);
        }

        ContactoMail con1 = new ContactoMail("uncontacto@gmail.com", "123");
        ContactoTelefono con2 = new ContactoTelefono("1155443322");
        ContactoMail con3 = new ContactoMail("otrocontacto@gmail.com", "321");
        ContactoTelefono con4 = new ContactoTelefono("1122334455");

        String almagro = "ALMAGRO";
        String mataderos = "MATADEROS";
        String lugano = "VILLA LUGANO";
        String caballito = "CABALLITO";
        String sanCristobal = "SAN CRISTOBAL";
        String palermo = "PALERMO";
        String flores = "FLORES";
        String villaCrespo = "VILLA CRESPO";
        String constitucion = "CONSTITUCION";

        Optional<String> estado = Stream.of(almagro, mataderos, lugano, constitucion,
                caballito, sanCristobal, palermo, flores, villaCrespo)
                .filter(loca -> !cacheLocalidades.containsKey(almagro))
                .findFirst();

        if(estado.isPresent()) {
            throw new RuntimeException(estado.get());
        }

        UbicacionGeografica ubicacionUtnCampus = new UbicacionGeografica(
                new Direccion(cabaMunicipio, lugano, "Mozart", 2300),
                new Coordenada(-34.659932F, -58.468397F));
        UbicacionGeografica ubicacionUtnMedrano = new UbicacionGeografica(
                new Direccion(cabaMunicipio, almagro, "Av. Medrano", 951),
                new Coordenada(-34.598412F, -58.420196F));
        UbicacionGeografica ubicacionMcObelisco = new UbicacionGeografica(
                new Direccion(cabaMunicipio, sanCristobal, "Av. Corrientes", 992),
                new Coordenada(-34.603825f, -58.380681f));

        UbicacionGeografica mirallaAlberdi = new UbicacionGeografica(
                new Direccion(cabaMunicipio, mataderos, "Miralla", 2200),
                new Coordenada(-34.649292F, -58.499945F));
        UbicacionGeografica sanPedrito = new UbicacionGeografica(
                new Direccion(cabaMunicipio, caballito, "San Pedrito", 2500),
                new Coordenada(-34.630861F, -58.470063F));
        UbicacionGeografica castroBarros = new UbicacionGeografica(
                new Direccion(cabaMunicipio, almagro, "Castro Barros", 256),
                new Coordenada(-34.611624F, -58.421263F));
        UbicacionGeografica cordobaY9deJulio = new UbicacionGeografica(
                new Direccion(cabaMunicipio, palermo, "Av. Cordoba", 1100),
                new Coordenada(-34.599001f, -58.380794f));
        UbicacionGeografica cordobaYEcuador = new UbicacionGeografica(
                new Direccion(cabaMunicipio, villaCrespo, "Av. Cordoba", 2700),
                new Coordenada(-34.597991f, -58.405410f));
        UbicacionGeografica cordobaYMedrano = new UbicacionGeografica(
                new Direccion(cabaMunicipio, villaCrespo, "Av. Cordoba", 3800),
                new Coordenada(-34.597810f, -58.420118f));

        UbicacionGeografica casaDePapuGomez = new UbicacionGeografica(
                new Direccion(cabaMunicipio, lugano, "Av. Escalada", 300),
                new Coordenada(-34.675744f,-58.455509f));//Av Escalada y Alberto Zorrilla
        UbicacionGeografica casaDeManu = new UbicacionGeografica(
                new Direccion(cabaMunicipio, sanCristobal, "Av. Jujuy", 800),
                new Coordenada(-34.618784f,-58.403749f));//Av Jujuy y Av Independencia
        UbicacionGeografica estacionamientoDeWalter = new UbicacionGeografica(
                new Direccion(cabaMunicipio, mataderos, "Virasoro", 2367),
                new Coordenada(-34.584197f,-58.420833f)); //Virasoro 2367
        UbicacionGeografica casaDeWalter = new UbicacionGeografica(
                new Direccion(cabaMunicipio, flores, "Guatemala",1425),
                new Coordenada(-34.587201f,-58.423048f));//Guatemala 1425

        AgenteSectorial carlos = new AgenteSectorial(cabaProvincia, con1, con2);
        AgenteSectorial esteban = new AgenteSectorial(cabaMunicipio, con3, con4);
        this.repoAgentes.agregar(carlos, esteban);

        this.repoUbicaciones.agregar(ubicacionUtnCampus, mirallaAlberdi, sanPedrito, castroBarros, ubicacionUtnMedrano,
                cordobaY9deJulio, cordobaYEcuador, cordobaYMedrano, casaDePapuGomez, casaDeManu, casaDeWalter,
                estacionamientoDeWalter);

//        this.repoContactos.agregar(con1, con2, con3, con4);

        MedioDeTransporte fitito = new ServicioContratado(new TipoServicio("Taxi"));
        MedioDeTransporte autoDeManu = new VehiculoParticular(TipoVehiculo.AUTOMOVIL, TipoCombustible.GASOIL);
        MedioDeTransporte autoDeWalter = new VehiculoParticular(TipoVehiculo.AUTOMOVIL, TipoCombustible.NAFTA);
        MedioDeTransporte caminata = new TransporteEcologico(TipoTransporteEcologico.PIE);
        MedioDeTransporte bicicleta = new TransporteEcologico(TipoTransporteEcologico.BICICLETA);

        TransportePublico colectivo109 = new TransportePublico(TipoTransportePublico.COLECTIVO, "109");
        colectivo109.agregarParadas(
                new Parada(cordobaY9deJulio, 0f, 0.45f),
                new Parada(new Direccion(cabaMunicipio, palermo, "Av. Córdoba", 1231),
                        new Coordenada(-34.599298f, -58.387400f), 0.45f, 0.4f), //av cordoba y uruguay
                new Parada(new Direccion(cabaMunicipio, palermo, "Av. Córdoba", 1531),
                        new Coordenada(-34.599554f, -58.394166f), 0.4f, 1.2f), //av cordoba y riobamba
                new Parada(cordobaYEcuador, 1.2f, 0.5f), //av cordoba y ecuador
                new Parada(new Direccion(cabaMunicipio, palermo, "Av. Córdoba", 2100),
                        new Coordenada(-34.597935f, -58.413103f), 0.5f, 0.6f), //av cordoba y bustamante
                new Parada(cordobaYMedrano, 0.6f, 0f) //av cordoba y av medrano
        );

        TransportePublico colectivo63 = new TransportePublico(TipoTransportePublico.COLECTIVO, "63");
        colectivo63.agregarParadas(
                new Parada(new Direccion(cabaMunicipio, mataderos, "Av. Alberdi", 6200),
                        new Coordenada(-34.655186F, -58.506615F), 0F, 0.6F), // pilar
                new Parada(new Direccion(cabaMunicipio, mataderos, "Av. Alberdi", 5900),
                        new Coordenada(-34.651554F, -58.502202F), 0.6F, 0.350F), // larrazabal
                new Parada(mirallaAlberdi, 0.350F, 0.6F), // miralla
                new Parada(new Direccion(cabaMunicipio, mataderos, "Av. Alberdi", 5000),
                        new Coordenada(-34.645056F, -58.495307F), 0.6F, 1.3F), // escalada
                new Parada(new Direccion(cabaMunicipio, mataderos, "Av. Alberdi", 4500),
                        new Coordenada(-34.636489F, -58.492135F), 1.5F, 0.950F), // mozart
                new Parada(new Direccion(cabaMunicipio, flores, "Av. Alberdi", 3500),
                        new Coordenada(-34.634149F, -58.482348F), 0.950F, 1.1F), // mariano acosta
                new Parada(sanPedrito, 1.1F, 0.8F), // san pedrito
                new Parada(new Direccion(cabaMunicipio, caballito, "Av. Nazca", 4321),
                        new Coordenada(-34.626601F, -58.471626F), 0.6F, 0.7F), // avellaneda
                new Parada(new Direccion(cabaMunicipio, caballito, "Av. Nazca", 1234),
                        new Coordenada(-34.620776F, -58.474034F), 0.7F, 0F) // gaona
        );

        TransportePublico subteA = new TransportePublico(TipoTransportePublico.SUBTE, "A");
        subteA.agregarParadas(
                new Parada(sanPedrito, 0F, 1.3F), // san pedrito
                new Parada(new Direccion(cabaMunicipio, caballito, "Av. Rivadavia", 7000),
                        new Coordenada(-34.626615F, -58.456582F), 1.3F, 1.5F), // carabobo
                new Parada(new Direccion(cabaMunicipio, caballito, "Av. Rivadavia", 6500),
                        new Coordenada(-34.620974F, -58.442162F), 1.5F, 0.6F), // primera junta
                new Parada(new Direccion(cabaMunicipio, caballito, "Av. Rivadavia", 5800),
                        new Coordenada(-34.617957F, -58.435755F), 0.6F, 0.5F), // acoyte
                new Parada(castroBarros, 0.5F, 0.7F), // castro barros
                new Parada(new Direccion(cabaMunicipio, almagro, "Av. Rivadavia", 6500),
                        new Coordenada(-34.610828F, -58.415497F), 0.7F, 0.7F), // loria
                new Parada(new Direccion(cabaMunicipio, constitucion, "Av. Rivadavia", 4500),
                        new Coordenada(-34.610144F, -58.406930F), 0.7F, 0F) // plaza miserere
        );

        this.repoMedios.agregar(fitito, colectivo63, colectivo109, subteA, caminata,
                bicicleta, autoDeManu, autoDeWalter);

        // Organizaciones, sectores y miembros

        ClasificacionOrganizacion universidad = new ClasificacionOrganizacion("Universidad");

        Organizacion orgUtnCampus = new Organizacion("UTN - Campus", TipoDeOrganizacionEnum.INSTITUCION,
                universidad, ubicacionUtnCampus);

        orgUtnCampus.agregarContactoMail(new ContactoMail("cuentafalsa123@hotmail.com"));
        orgUtnCampus.agregarContactoMail(new ContactoMail("cuentafalsa456@hotmail.com"));

        Sector sistemasUtnCampus = new Sector("Sistemas", orgUtnCampus);
        Miembro juanPerez = new Miembro("Juan", "Perez", TipoDeDocumento.DNI, 14432234);
        Miembro guillermoCoppola = new Miembro("Guillermo", "Cóppola", TipoDeDocumento.PASAPORTE, 11332223);
        Miembro manuGinobili = new Miembro("Emanuel", "Ginobili", TipoDeDocumento.DNI, 32123456);
        sistemasUtnCampus.agregarMiembro(juanPerez);//llega con auto,bondi,sube y a pata
        sistemasUtnCampus.agregarMiembro(guillermoCoppola);
        sistemasUtnCampus.agregarMiembro(manuGinobili);///llega en auto particular

        Sector rrhhUtnCampus = new Sector("Capital Humano", orgUtnCampus);
        Miembro sebaSosa = new Miembro("Sebastián", "Sosa", TipoDeDocumento.DNI, 11233223);
        Miembro martinTagliafico = new Miembro("Martin", "Tagliafico", TipoDeDocumento.DNI, 30324522);
        Miembro papuGomez = new Miembro("Alejandro", "Gomez", TipoDeDocumento.DNI, 35789065);
        rrhhUtnCampus.agregarMiembro(sebaSosa);
        rrhhUtnCampus.agregarMiembro(martinTagliafico);
        rrhhUtnCampus.agregarMiembro(papuGomez);//llega en bici

        Sector adminUtnCampus = new Sector("Administracion", orgUtnCampus);
        Miembro elDoctor = new Miembro("Carlos", "Bilardo", TipoDeDocumento.DNI,27145367);
        Miembro laPulga = new Miembro("Lionel", "Messi", TipoDeDocumento.DNI,34674393);
        Miembro elDiego = new Miembro("Diego", "Maradona", TipoDeDocumento.DNI, 20345654);
        adminUtnCampus.agregarMiembro(elDoctor);
        adminUtnCampus.agregarMiembro(laPulga);
        adminUtnCampus.agregarMiembro(elDiego);

        Organizacion orgUtnMedrano = new Organizacion("UTN - Medrano", TipoDeOrganizacionEnum.INSTITUCION,
                universidad, ubicacionUtnMedrano);

        Sector tesoreriaUtnMedrano = new Sector("Tesoreria", orgUtnMedrano);
        Miembro laSaeta = new Miembro("Alfredo", "Di Stéfano", TipoDeDocumento.DNI, 2232122);
        Miembro fitoPaez = new Miembro("Rodolfo", "Páez", TipoDeDocumento.DNI, 34432237);
        Miembro charlyGarcia = new Miembro("Carlos", "Garcia", TipoDeDocumento.DNI, 34432233);
        tesoreriaUtnMedrano.agregarMiembro(laSaeta);
        tesoreriaUtnMedrano.agregarMiembro(fitoPaez);
        tesoreriaUtnMedrano.agregarMiembro(charlyGarcia);//en bondi y caminando

        Sector administracionUtnMedrano = new Sector("Administracion", orgUtnMedrano);
        Miembro elVirrey = new Miembro("Carlos", "Bianchi", TipoDeDocumento.DNI, 22222222);
        Miembro elTitan = new Miembro("Martin", "Palermo", TipoDeDocumento.DNI, 30366666);
        administracionUtnMedrano.agregarMiembro(elVirrey);
        administracionUtnMedrano.agregarMiembro(elTitan);
        administracionUtnMedrano.agregarMiembro(elDiego);//tambien trabaja en sector : adminUtnCampus

        Sector mantenimientoUtnMedrano = new Sector("Mantenimiento", orgUtnMedrano);
        Miembro albertEinstein = new Miembro("Albert", "Einstein", TipoDeDocumento.DNI, 10101010);
        Miembro isaacNewton = new Miembro("Isaac", "Newton", TipoDeDocumento.DNI, 20202020);
        Miembro nikolaTesla = new Miembro("Nikola", "Tesla", TipoDeDocumento.DNI, 30303030);
        mantenimientoUtnMedrano.agregarMiembro(albertEinstein);
        mantenimientoUtnMedrano.agregarMiembro(isaacNewton);
        mantenimientoUtnMedrano.agregarMiembro(nikolaTesla);

        Organizacion orgMcObelisco = new Organizacion("McDonald", TipoDeOrganizacionEnum.EMPRESA,
                new ClasificacionOrganizacion("Restaurante"), ubicacionMcObelisco);

        Sector cocinaMc = new Sector("Tesoreria", orgMcObelisco);
        Miembro gordonRamsey = new Miembro("Gordon", "Ramsey", TipoDeDocumento.DNI, 30000129);
        Miembro jackGusto = new Miembro("Jack", "Gusto", TipoDeDocumento.DNI, 26345554);
        Miembro marioBatali = new Miembro("Mario", "Batali", TipoDeDocumento.DNI, 31666666);
        cocinaMc.agregarMiembro(gordonRamsey);
        cocinaMc.agregarMiembro(jackGusto);
        cocinaMc.agregarMiembro(marioBatali);

        Sector mantenimientoMc = new Sector("Administracion", orgMcObelisco);
        Miembro elonMusk = new Miembro("Elon", "Musk", TipoDeDocumento.DNI, 27345900);
        Miembro billGates = new Miembro("Bill", "Gates", TipoDeDocumento.DNI, 23100698);
        mantenimientoMc.agregarMiembro(elonMusk);
        mantenimientoMc.agregarMiembro(billGates);
        mantenimientoMc.agregarMiembro(albertEinstein);//tambien trabaja en mantenimiento de medrano

        Sector limpiezaMc = new Sector("Mantenimiento", orgMcObelisco);
        Miembro walterWhite = new Miembro("Walter", "White", TipoDeDocumento.DNI, 26456890);
        Miembro jessePinkman = new Miembro("Jesse", "Pinkman", TipoDeDocumento.DNI, 40398652);
        Miembro saulGoodman = new Miembro("Saul", "Goodman", TipoDeDocumento.DNI, 31567908);
        limpiezaMc.agregarMiembro(walterWhite);
        limpiezaMc.agregarMiembro(jessePinkman);
        limpiezaMc.agregarMiembro(saulGoodman);

        this.repoOrganizaciones.agregar(orgUtnCampus, orgUtnMedrano, orgMcObelisco);

        // Tramos y trayectos

        Trayecto trayectoPapu = new Trayecto(new Periodo(2022),
                new Tramo(bicicleta, casaDePapuGomez, ubicacionUtnCampus));
        papuGomez.agregarTrayecto(trayectoPapu);
        trayectoPapu.agregarMiembro(papuGomez);

        Trayecto trayectoManu = new Trayecto(new Periodo(2022),
                new Tramo(autoDeManu, casaDeManu, ubicacionUtnCampus));
        manuGinobili.agregarTrayecto(trayectoManu);
        trayectoManu.agregarMiembro(manuGinobili);

        Trayecto trayectoCharly = new Trayecto(new Periodo(2021),
                new Tramo(colectivo109, cordobaY9deJulio, cordobaYMedrano),
                new Tramo(caminata, cordobaYMedrano, ubicacionUtnMedrano));
        charlyGarcia.agregarTrayecto(trayectoCharly);
        trayectoCharly.agregarMiembro(charlyGarcia);

        Trayecto trayectoJuan = new Trayecto(new Periodo(2022),
                new Tramo(fitito, ubicacionUtnCampus, mirallaAlberdi),
                new Tramo(colectivo63, mirallaAlberdi, sanPedrito),
                new Tramo(subteA, sanPedrito, castroBarros),
                new Tramo(caminata, castroBarros, ubicacionUtnMedrano));
        juanPerez.agregarTrayecto(trayectoJuan);
        trayectoJuan.agregarMiembro(juanPerez);

        Trayecto trayectoEcuadorAMedrano = new Trayecto(new Periodo(2022),
                new Tramo(colectivo109, cordobaYEcuador, cordobaYMedrano),
                new Tramo(caminata, cordobaYMedrano, ubicacionUtnMedrano));
        elVirrey.agregarTrayecto(trayectoEcuadorAMedrano);
        elTitan.agregarTrayecto(trayectoEcuadorAMedrano);
        trayectoEcuadorAMedrano.agregarMiembro(elVirrey);
        trayectoEcuadorAMedrano.agregarMiembro(elTitan);

        Trayecto trayectoDeA3 = new Trayecto(new Periodo(2022),
                new Tramo(caminata, casaDeWalter, estacionamientoDeWalter),
                new Tramo(autoDeWalter, estacionamientoDeWalter, ubicacionMcObelisco));
        walterWhite.agregarTrayecto(trayectoDeA3);
        jessePinkman.agregarTrayecto(trayectoDeA3);
        saulGoodman.agregarTrayecto(trayectoDeA3);
        trayectoDeA3.agregarMiembro(walterWhite);
        trayectoDeA3.agregarMiembro(jessePinkman);
        trayectoDeA3.agregarMiembro(saulGoodman);

        Trayecto[] trayectos = {trayectoManu, trayectoPapu, trayectoCharly, trayectoJuan, trayectoEcuadorAMedrano, trayectoDeA3};

        Arrays.stream(trayectos).flatMap(trayecto -> trayecto.getTramos().stream()).forEach(Tramo::setValor);

        this.repoTrayectos.agregar(trayectos);

        // Mediciones

        Medicion[] medicionesIniciales = {
                new Medicion(new Categoria("Combustion Fija", "Gas Natural"), "m3", 3f, new Periodo(2022, 9)),
                new Medicion(new Categoria("Combustion Fija", "Gas Natural"), "m3", 5f, new Periodo(2022, 8)),
                new Medicion(new Categoria("Electricidad", "Electricidad"), "kw", 4.7f, new Periodo(2022, 8)),
                new Medicion(new Categoria("Electricidad", "Electricidad"), "kw", 10f, new Periodo(2022, 7)),
                new Medicion(new Categoria("Combustion Fija", "Diesel"), "lt", 4f, new Periodo(2022, 7)),
                new Medicion(new Categoria("Electricidad", "Electricidad"), "kw", 22f, new Periodo(2022, 6)),
                new Medicion(new Categoria("Combustion Movil", "Gasoil"), "lt", 2.2f, new Periodo(2022, 6)),
                new Medicion(new Categoria("Combustion Fija", "Gas Natural"), "m3", 4.6f, new Periodo(2022, 6)),
                new Medicion(new Categoria("Combustion Fija", "Gas Natural"), "m3", 2f, new Periodo(2022, 5)),
                new Medicion(new Categoria("Electricidad", "Electricidad"), "kw", 4f, new Periodo(2022, 5)),
                new Medicion(new Categoria("Electricidad", "Electricidad"), "kw", 14f, new Periodo(2022, 4)),
                new Medicion(new Categoria("Combustion Fija", "Diesel"), "lt", 4.7f, new Periodo(2022, 3)),
                new Medicion(new Categoria("Electricidad", "Electricidad"), "kw", 23.6f, new Periodo(2022, 3)),
                new Medicion(new Categoria("Combustion Movil", "Gasoil"), "lt", 24f, new Periodo(2022, 2)),
                new Medicion(new Categoria("Combustion Fija", "Gas Natural"), "m3", 3f, new Periodo(2021, 12)),
                new Medicion(new Categoria("Combustion Fija", "Gas Natural"), "m3", 5f, new Periodo(2021, 10)),
                new Medicion(new Categoria("Electricidad", "Electricidad"), "kw", 4.7f, new Periodo(2021, 8)),
                new Medicion(new Categoria("Electricidad", "Electricidad"), "kw", 10f, new Periodo(2021, 7)),
                new Medicion(new Categoria("Combustion Fija", "Diesel"), "lt", 6.8f, new Periodo(2021, 7)),
                new Medicion(new Categoria("Electricidad", "Electricidad"), "kw", 7f, new Periodo(2021, 6)),
                new Medicion(new Categoria("Combustion Movil", "Gasoil"), "lt", 5.3f, new Periodo(2021, 6))
                //la huella de carbono de todas estas mediciones tendria que dar 548.1
        };

        BatchMediciones batchMediciones = new BatchMediciones(Arrays.asList(medicionesIniciales), LocalDate.now());
        batchMediciones.setOrganizacion(orgUtnCampus);
        orgUtnCampus.agregarMediciones(medicionesIniciales);

        this.repoBatchMediciones.agregar(batchMediciones);

        User usuarioOrganizacionCampus = new User("campus", "org", orgUtnCampus);
        User usuarioOrganizacionMedrano = new User("medrano", "org", orgUtnMedrano);
        User usuarioOrganizacionMc = new User("mac", "org", orgMcObelisco);

        User usuarioMiembroCharly = new User("fito", "miembro", fitoPaez);
        User usuarioMiembroManu = new User("manu", "miembro", manuGinobili);
        User usuarioMiembroDiego = new User("diego", "miembro", elDiego);
        User usuarioMiembroPapu = new User("papu", "miembro", papuGomez);

        User usuarioAgenteProv = new User("provcaba", "agente", carlos);
        User usuarioAgenteMuni = new User("municaba", "agente", esteban);

        this.repoUsuarios.agregar(usuarioOrganizacionCampus, usuarioOrganizacionMedrano, usuarioOrganizacionMc,
                usuarioMiembroCharly, usuarioMiembroManu, usuarioMiembroDiego,
                usuarioAgenteProv, usuarioAgenteMuni, usuarioMiembroPapu);
    }

    public void undoSetup() {
        RepoFactores repoFactores = FactoryRepositorio.getByOutputType(RepoFactores.class);
        this.repoUsuarios.buscarTodos().forEach(this.repoUsuarios::eliminar);
        this.repoTrayectos.buscarTodos().forEach(this.repoTrayectos::eliminar);
        this.repoMedios.buscarTodos().forEach(this.repoMedios::eliminar);
        this.repoBatchMediciones.buscarTodos().forEach(this.repoBatchMediciones::eliminar);
        this.repoOrganizaciones.buscarTodos().forEach(this.repoOrganizaciones::eliminar);
        this.repoUbicaciones.buscarTodos().forEach(this.repoUbicaciones::eliminar);
        this.repoAreas.buscarTodos().forEach(this.repoAreas::eliminar);
        this.repoAgentes.buscarTodos().forEach(this.repoAgentes::eliminar);
        this.repoMiembros.buscarTodos().forEach(this.repoMiembros::eliminar);
        this.repoContactos.buscarTodos().forEach(this.repoContactos::eliminar);
        repoFactores.buscarTodos().forEach(repoFactores::eliminar);
    }

    private void inicializarFactores() {
        RepoFactores repoFactores = FactoryRepositorio.getByOutputType(RepoFactores.class);
        repoFactores.agregar(new FactorEmision(new Categoria("Combustion Fija", "Gas Natural"), "m3", 1F));
        repoFactores.agregar(new FactorEmision(new Categoria("Combustion Fija", "Diesel"), "lt", 2F));
        repoFactores.agregar(new FactorEmision(new Categoria("Combustion Movil", "Gasoil"), "lt", 3F));
        repoFactores.agregar(new FactorEmision(new Categoria("Electricidad", "Electricidad"), "kw", 4F));

        repoFactores.agregar(new FactorEmision(new Categoria("Traslado de Miembros", "Particular - GNC"), "km", 100F));
        repoFactores.agregar(new FactorEmision(new Categoria("Traslado de Miembros", "Particular - GASOIL"), "km", 80F));
        repoFactores.agregar(new FactorEmision(new Categoria("Traslado de Miembros", "Particular - NAFTA"), "km", 150F));
        repoFactores.agregar(new FactorEmision(new Categoria("Traslado de Miembros", "Particular - ELECTRICO"), "km", 15F));

        repoFactores.agregar(new FactorEmision(new Categoria("Traslado de Miembros", "Publico - TREN"), "km", 250F));
        repoFactores.agregar(new FactorEmision(new Categoria("Traslado de Miembros", "Publico - SUBTE"), "km", 125F));
        repoFactores.agregar(new FactorEmision(new Categoria("Traslado de Miembros", "Publico - COLECTIVO"), "km", 200F));

        repoFactores.agregar(new FactorEmision(new Categoria("Traslado de Miembros", "Ecologico"), "km", 0F));

        repoFactores.agregar(new FactorEmision(new Categoria("Traslado de Miembros", "Contratado"), "km", 130F));
    }

}