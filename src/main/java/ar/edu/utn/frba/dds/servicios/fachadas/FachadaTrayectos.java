package ar.edu.utn.frba.dds.servicios.fachadas;

import ar.edu.utn.frba.dds.entities.exceptions.TrayectoConMiembroRepetidoException;
import ar.edu.utn.frba.dds.entities.lugares.*;
import ar.edu.utn.frba.dds.entities.medibles.Periodo;
import ar.edu.utn.frba.dds.entities.personas.Miembro;
import ar.edu.utn.frba.dds.entities.exceptions.MiembroException;
import ar.edu.utn.frba.dds.entities.personas.TipoDeDocumento;
import ar.edu.utn.frba.dds.entities.transportes.MedioDeTransporte;
import ar.edu.utn.frba.dds.entities.medibles.Tramo;
import ar.edu.utn.frba.dds.entities.medibles.Trayecto;
import ar.edu.utn.frba.dds.entities.transportes.Parada;
import ar.edu.utn.frba.dds.entities.transportes.TransportePublico;
import ar.edu.utn.frba.dds.interfaces.controllers.TramoSinDistanciaException;
import ar.edu.utn.frba.dds.interfaces.controllers.TrayectoConMiembrosDeDistintaOrganizacionException;
import ar.edu.utn.frba.dds.repositories.RepoFactores;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.NoExisteMedioException;
import ar.edu.utn.frba.dds.interfaces.input.NuevoTrayectoDTO;
import ar.edu.utn.frba.dds.interfaces.input.TrayectoCompartidoDTO;
import ar.edu.utn.frba.dds.repositories.RepoMiembros;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.NoExisteUbicacionException;
import spark.QueryParamsMap;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FachadaTrayectos {
    private final RepoMiembros repoMiembros;
    private final Repositorio<Trayecto> repoTrayectos;
    private final FachadaMedios fachadaMedios;
    private final FachadaUbicaciones fachadaUbicaciones;

    public FachadaTrayectos() {
        this.repoMiembros = FactoryRepositorio.getByOutputType(RepoMiembros.class);
        this.repoTrayectos = FactoryRepositorio.getByParameterType(Trayecto.class);
        this.fachadaMedios = new FachadaMedios();
        this.fachadaUbicaciones = new FachadaUbicaciones();
    }

    public FachadaTrayectos(RepoMiembros repoMiembros, Repositorio<Trayecto> repoTrayectos, Repositorio<MedioDeTransporte> repoMedios) {
        this.repoMiembros = repoMiembros;
        this.repoTrayectos = repoTrayectos;
        this.fachadaMedios = new FachadaMedios();
        this.fachadaUbicaciones = new FachadaUbicaciones();
    }

    public Trayecto cargarTrayecto(Trayecto unTrayecto) {
        return this.repoTrayectos.agregar(unTrayecto);
    }

    public Trayecto updateTrayecto(Trayecto unTrayecto) {
        return this.repoTrayectos.modificar(unTrayecto);
    }

    public Trayecto obtenerTrayecto(Integer id) {
        Trayecto trayecto = this.repoTrayectos.buscar(id)
                .orElseThrow(EntityNotFoundException::new);
        this.repoTrayectos.sync(trayecto); //Sincronizo porque el trayecto pudo ya haber sido cargado y modificado por los miembros
        return trayecto;
    }

    public void eliminarTrayecto(Trayecto trayecto) {
        repoTrayectos.eliminar(trayecto);
    }

    public Miembro obtenerMiembro(Integer id) {
        Miembro miembro = this.repoMiembros.buscar(id)
                .orElseThrow(EntityNotFoundException::new);
        this.repoMiembros.sync(miembro); //Sincronizo porque el miembro pudo ya haber sido cargado y modificado por los trayectos
        return miembro;
    }

    public List<MedioDeTransporte> obtenerTransportes() {
        return this.fachadaMedios.findAll();
    }

    public Optional<MedioDeTransporte> obtenerTransporte(Integer id) {
        return this.fachadaMedios.findById(id);
    }

    public void mostrarTrayectos() {
        List<Trayecto> trayectos = this.repoTrayectos.buscarTodos();
        trayectos.forEach(t -> {
            this.repoTrayectos.sync(t);
            System.out.println(t);
        });
    }

    public Trayecto cargarTrayectoActivo(Miembro miembro, QueryParamsMap queryMap) {
        Periodo periodo = parsearPeriodo(queryMap.value("f-fecha"));
        Trayecto trayecto = new Trayecto(periodo);
        trayecto.setTramos(asignarTramos(queryMap));

        if(queryMap.value("f-transporte-nuevo") != null) {
            trayecto.agregarTramo(asignarTramoNuevo(queryMap));
        }

        miembro.agregarTrayecto(trayecto);
        trayecto.agregarMiembro(miembro);
        return cargarTrayecto(trayecto);
    }

    public Trayecto cargarTrayectoPasivo(Miembro miembro, Integer idTrayecto) {
        Trayecto trayecto = obtenerTrayecto(idTrayecto);

        if(trayecto.getMiembros().stream().map(Miembro::getId).anyMatch(mi -> mi.equals(miembro.getId()))) {
            throw new TrayectoConMiembroRepetidoException();
        }

        trayecto.getMiembros().stream().limit(1) // Hago el limit para que busque solo en el primer miembro
                .flatMap(mi -> mi.getOrganizaciones().stream())
                .filter(org -> miembro.getOrganizaciones().stream().anyMatch(o -> o.equals(org)))
                .findAny().orElseThrow(TrayectoConMiembrosDeDistintaOrganizacionException::new);

        miembro.agregarTrayecto(trayecto);
        trayecto.agregarMiembro(miembro);

        return updateTrayecto(trayecto);
    }

    public void cargarTrayectoActivo(NuevoTrayectoDTO trayectoDTO) { //todo quedo viejo
        Miembro unMiembro = repoMiembros.findByDocumento(TipoDeDocumento.valueOf(trayectoDTO.getTipoDocumento()),
                        trayectoDTO.getMiembroDNI())
                .orElseThrow(MiembroException::new);

        Trayecto trayecto = repoTrayectos.buscarTodos().stream()//todo falta el sync
                .filter(tr -> tr.getId().equals(trayectoDTO.getTrayectoId()))
                .findFirst().orElseGet(() -> {
                    Periodo periodo = trayectoDTO.getPeriodicidad().equals('A')
                            ? new Periodo(trayectoDTO.getAnio())
                            : new Periodo(trayectoDTO.getAnio(), trayectoDTO.getMes());

                    Trayecto nuevoTrayecto = new Trayecto(periodo);
                    unMiembro.agregarTrayecto(nuevoTrayecto);
                    repoTrayectos.agregar(nuevoTrayecto);

                    return nuevoTrayecto;
                });
        trayecto.agregarMiembro(unMiembro);

        MedioDeTransporte medio = fachadaMedios.find(trayectoDTO.getTipoMedio(),
                        trayectoDTO.getAtributo1(), trayectoDTO.getAtributo2())
                .orElseThrow(() -> new NoExisteMedioException(trayectoDTO.getTipoMedio(),
                        trayectoDTO.getAtributo1(), trayectoDTO.getAtributo2()));

        Coordenada coordenadaInicial = new Coordenada(trayectoDTO.getLatitudInicial(), trayectoDTO.getLongitudInicial());
        Coordenada coordenadaFinal = new Coordenada(trayectoDTO.getLatitudFinal(), trayectoDTO.getLongitudFinal());
        UbicacionGeografica ubicacionInicial = new UbicacionGeografica(new Direccion(), coordenadaInicial);
        UbicacionGeografica ubicacionFinal = new UbicacionGeografica(new Direccion(), coordenadaFinal);

        Tramo tramo = new Tramo(medio, ubicacionInicial, ubicacionFinal);
        tramo.setValor();
        tramo.setTrayecto(trayecto);

        trayecto.agregarTramo(tramo);
        repoTrayectos.modificar(trayecto);
    }

    public void cargarTrayectoPasivo(TrayectoCompartidoDTO trayectoCompartidoDTO) {
        Miembro miembro = repoMiembros.findByDocumento(TipoDeDocumento.DNI, trayectoCompartidoDTO.getMiembroDNI())
                .orElseThrow(() -> new EntityNotFoundException("El miembro con DNI: " +
                        trayectoCompartidoDTO.getMiembroDNI() + "no existe en el sistema"));

        cargarTrayectoPasivo(miembro, trayectoCompartidoDTO.getTrayectoReferencia());
    }

    private UbicacionGeografica obtenerUbicacion(QueryParamsMap map, MedioDeTransporte transporte, Boolean esInicial, Integer posicion) {
        Function<String, Integer> paramToInt = param -> Integer.parseInt(map.value(param));
        Function<String, Float> paramToFloat = param -> Float.parseFloat(map.value(param));

        String pos = posicion == null ? "nueva" : posicion.toString();
        String lugar = esInicial ? "inicial" : "final";

        if(transporte instanceof TransportePublico) {
            Integer paradaId = paramToInt.apply("f-transporte-parada-" + lugar + "-" + pos);
            return fachadaUbicaciones.getParada((TransportePublico) transporte, paradaId)
                    .orElseThrow(() -> new NoExisteUbicacionException("No existe la parada indicada"))
                    .getUbicacion();
        } else {
            String pais = "ARGENTINA";
            String provincia = map.value("f-provincia-" + lugar + "-" + pos);
            String municipio = map.value("f-municipio-" + lugar + "-" + pos);
            String localidad = map.value("f-localidad-" + lugar + "-" + pos);
            String calle = map.value("f-calle-" + lugar + "-" + pos);
            Integer numero = paramToInt.apply("f-numero-" + lugar + "-" + pos);
            Float latitud = paramToFloat.apply("f-lat-" + lugar + "-" + pos);
            Float longitud = paramToFloat.apply("f-lon-" + lugar + "-" + pos);
            return fachadaUbicaciones.getUbicacion(pais, provincia, municipio, localidad, calle, numero, latitud, longitud);
        }
    }

    private List<Tramo> asignarTramos(QueryParamsMap map) {
        Integer limit = Stream.iterate(0, i -> i + 1)
                .filter(i -> map.value("f-transporte-"+i) == null)
                .findFirst().orElse(0);

        return Stream.iterate(0, i -> i + 1)
                .limit(limit)
                .map(i -> {
//                    if((req.queryParams("f-transporte-parada-inicial-" + i) == null) && (req.queryParams("f-lat-inicial-"+i) == null)) {
//                     //Cuando se deja sin modificar el tramo //TODO VER SI ENTRA ALGUNA VEZ, CREO QUE QUEDO VIEJO CUANDO ESTABA EL DISABLED
//                        return trayecto.getTramos().get(i); //TODO orden de la lista (indice es cant?), o buscar id?
//                    }

                    MedioDeTransporte transporte = obtenerTransporte(Integer.parseInt(map.value("f-transporte-" + i))).get();

                    Tramo tramo = new Tramo(transporte, obtenerUbicacion(map, transporte, true, i), obtenerUbicacion(map, transporte, false, i));

                    if(tramo.getUbicacionInicial().getCoordenada().esIgualAOtraCoordenada(tramo.getUbicacionFinal().getCoordenada())) {
                        throw new TramoSinDistanciaException();
                    }

                    tramo.setId(Integer.parseInt(map.value("f-tramo-id-"+i)));
                    return tramo;
                })
                .collect(Collectors.toList());
    }

    private Tramo asignarTramoNuevo(QueryParamsMap map) {
        MedioDeTransporte transporte = obtenerTransporte(Integer.parseInt(map.value("f-transporte-nuevo"))).get();
        return new Tramo(transporte, obtenerUbicacion(map, transporte, true, null), obtenerUbicacion(map, transporte, false, null));
    }

    private Periodo parsearPeriodo(String input) {
        Periodo periodo;
        if(input.matches("\\d+")) {
            periodo = new Periodo(Integer.parseInt(input));
        } else if(input.matches("\\d+/\\d+")) {
            String[] fecha = input.split("/");
            periodo = new Periodo(Integer.parseInt(fecha[1]), Integer.parseInt(fecha[0]));
        } else {
            LocalDate fecha = LocalDate.now();
            periodo = new Periodo(fecha.getYear(), fecha.getMonthValue());
        }
        return periodo;
    }

    public void modificarTrayecto(Miembro miembro, int idTrayecto, QueryParamsMap queryMap) {
        Trayecto trayecto = obtenerTrayecto(idTrayecto);

        if(trayecto.getMiembros().stream().noneMatch(m -> m.getId().equals(miembro.getId()))) {
            throw new MiembroException();
        }

        trayecto.setTramos(asignarTramos(queryMap));

        if(queryMap.value("f-transporte-nuevo") != null) {
            trayecto.agregarTramo(asignarTramoNuevo(queryMap));
        }

        this.updateTrayecto(trayecto);
    }

    public void quitarTrayectoDeMiembro(Integer idMiembro, Integer idTrayecto) {
        Miembro miembro = obtenerMiembro(idMiembro);
        Trayecto trayecto = obtenerTrayecto(idTrayecto);
        miembro.quitarTrayecto(trayecto);
        trayecto.quitarMiembro(miembro);

        this.repoMiembros.modificar(miembro);
        this.repoTrayectos.modificar(trayecto);
    }
}
