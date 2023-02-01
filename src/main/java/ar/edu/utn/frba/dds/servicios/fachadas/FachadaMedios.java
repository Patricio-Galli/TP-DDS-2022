package ar.edu.utn.frba.dds.servicios.fachadas;

import ar.edu.utn.frba.dds.entities.transportes.*;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.NoExisteMedioException;
import ar.edu.utn.frba.dds.repositories.*;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.repositories.Repositorio;

import java.util.List;
import java.util.Optional;

public class FachadaMedios {
    private final Repositorio<MedioDeTransporte> repoMedios;
    private final RepoEcologicos repoEcologicos;
    private final RepoPublicos repoPublicos;
    private final RepoContratados repoContratados;
    private final RepoParticulares repoParticulares;
    private final RepoTiposServicio repoTiposServicio;

    public FachadaMedios() {
        this.repoMedios = FactoryRepositorio.getByParameterType(MedioDeTransporte.class);
        this.repoEcologicos = FactoryRepositorio.getByOutputType(RepoEcologicos.class);
        this.repoPublicos = FactoryRepositorio.getByOutputType(RepoPublicos.class);
        this.repoContratados = FactoryRepositorio.getByOutputType(RepoContratados.class);
        this.repoTiposServicio = FactoryRepositorio.getByOutputType(RepoTiposServicio.class);
        this.repoParticulares = FactoryRepositorio.getByOutputType(RepoParticulares.class);
    }

    public Optional<MedioDeTransporte> findById(Integer id) {
        return repoMedios.buscar(id);
    }

    public List<MedioDeTransporte> findAll() {
        return repoMedios.buscarTodos();
    }

    public Optional<? extends MedioDeTransporte> find(String tipo, String discriminante1, String discriminante2) {
        if(tipo.equals("contratado")) {
            return obtenerContratado(discriminante1);
        } else if(tipo.equals("publico")) {
            return obtenerPublico(discriminante1, discriminante2);
        } else if(tipo.equals("ecologico")) {
            return obtenerEcologico(discriminante1);
        } else if(tipo.equals("particular")) {
            return obtenerParticular(discriminante1, discriminante2);
        }

        throw new NoExisteMedioException(tipo);
    }

    private Optional<VehiculoParticular> obtenerParticular(String discriminante1, String discriminante2) {
        if(TipoVehiculo.hasValue(discriminante1) && TipoCombustible.hasValue(discriminante2)) {
            return this.repoParticulares.findByEquality(TipoVehiculo.valueOf(discriminante1), TipoCombustible.valueOf(discriminante2));
        }
        return Optional.empty();
    }

    private Optional<TransporteEcologico> obtenerEcologico(String discriminante1) {
        if(TipoTransporteEcologico.hasValue(discriminante1)) {
            return this.repoEcologicos.findByEquality(TipoTransporteEcologico.valueOf(discriminante1));
        }
        return Optional.empty();
    }

    private Optional<TransportePublico> obtenerPublico(String discriminante1, String discriminante2) {
        if(TipoTransportePublico.hasValue(discriminante1)) {
            return this.repoPublicos.findByEquality(TipoTransportePublico.valueOf(discriminante1), discriminante2);
        }
        return Optional.empty();
    }

    private Optional<ServicioContratado> obtenerContratado(String discriminante1) {
        return repoTiposServicio.findByEquality(discriminante1)
                .flatMap(repoContratados::findByEquality);
    }

}
