package ar.edu.utn.frba.dds.servicios.fachadas;

import ar.edu.utn.frba.dds.entities.lugares.*;
import ar.edu.utn.frba.dds.entities.transportes.Parada;
import ar.edu.utn.frba.dds.entities.transportes.TransportePublico;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;
import java.util.function.BiFunction;

public class FachadaUbicaciones {
    private final Repositorio<AreaSectorial> repoAreas;
    private final Repositorio<Municipio> repoMunicipios;
    private final Repositorio<Provincia> repoProvincias;
    private final Repositorio<UbicacionGeografica> repoUbicaciones;

    public FachadaUbicaciones() {
        this.repoAreas = FactoryRepositorio.getByParameterType(AreaSectorial.class);
        this.repoMunicipios = FactoryRepositorio.getByParameterType(Municipio.class);
        this.repoProvincias = FactoryRepositorio.getByParameterType(Provincia.class);
        this.repoUbicaciones = FactoryRepositorio.getByParameterType(UbicacionGeografica.class);
    }

    public Optional<Municipio> getMunicipio(String municipio, String provincia) {
        return this.repoMunicipios.buscarTodos().stream()
                .filter(muni -> muni.getNombre().equals(municipio) &&
                        provincia.equals(muni.getProvincia().getNombre())).findFirst();
    }

    public Optional<Parada> getParada(TransportePublico transporte, Integer id) {
        return transporte.getParadas().stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public UbicacionGeografica getUbicacion(String pais, String provincia, String municipio, String localidad, String calle, Integer numero, Float latitud, Float longitud) {
        Municipio municipioReal = getMunicipio(municipio, provincia)
                //otra es en el orElse crear nuevo Municipio
                .orElseThrow(() -> new EntityNotFoundException("No existe la ubicación de municipio " + municipio +
                        " y provincia " + provincia));
        Direccion direccion = new Direccion(municipioReal, localidad, calle, numero);
        Coordenada coordenadas = new Coordenada(latitud, longitud);
        return this.repoUbicaciones.buscarTodos()
                .stream()
                .filter(u -> u.getDireccion().esIgualAOtraDireccion(direccion) && u.getCoordenada().esIgualAOtraCoordenada(coordenadas)) //todo check la igualacion de direcciones
                .findFirst()
                .orElseGet(() -> new UbicacionGeografica(direccion, coordenadas));
    }


}
