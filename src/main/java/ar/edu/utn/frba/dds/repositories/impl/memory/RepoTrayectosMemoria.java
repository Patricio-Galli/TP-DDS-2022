package ar.edu.utn.frba.dds.repositories.impl.memory;

import ar.edu.utn.frba.dds.entities.personas.Miembro;
import ar.edu.utn.frba.dds.entities.transportes.MedioDeTransporte;
import ar.edu.utn.frba.dds.entities.transportes.Parada;
import ar.edu.utn.frba.dds.entities.transportes.TransportePublico;
import ar.edu.utn.frba.dds.entities.medibles.Tramo;
import ar.edu.utn.frba.dds.entities.medibles.Trayecto;
import ar.edu.utn.frba.dds.repositories.daos.DAOMemoria;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.repositories.Repositorio;

import java.util.List;

public class RepoTrayectosMemoria<T> extends RepositorioMemoria<Trayecto> {

    private Repositorio<Tramo> repoTramos = FactoryRepositorio.getByParameterType(Tramo.class);

    public RepoTrayectosMemoria(DAOMemoria<Trayecto> dao) {
        super(dao);
        this.buscarTodos().forEach(this::sincronizarRepos);
    }

    private void sincronizarRepos(Trayecto trayecto) {
        Repositorio<Miembro> repoMiembros = FactoryRepositorio.getByParameterType(Miembro.class);
        trayecto.getMiembros().forEach(m -> {
            if(!repoMiembros.buscarTodos().contains(m))
                repoMiembros.agregar(m);
        });

        Repositorio<MedioDeTransporte> repoMedios = FactoryRepositorio.getByParameterType(MedioDeTransporte.class);
        Repositorio<Parada> repoParadas = FactoryRepositorio.getByParameterType(Parada.class);
        List<Tramo> tramos = trayecto.getTramos();
        tramos.stream().map(Tramo::getMedioDeTransporte).forEach(mt -> {
            if(!repoMedios.buscarTodos().contains(mt))
                repoMedios.agregar(mt);
            if(mt instanceof TransportePublico){
                List<Parada> paradas = ((TransportePublico) mt).getParadas();
                paradas.forEach(p -> {
                    if(!repoParadas.buscarTodos().contains(p))
                        repoParadas.agregar(p); //todo quizas sacar de acá y que las ids se establezcan por cada transporte!
                });
            }
        });
        tramos.forEach(repoTramos::agregar);
    }

    @Override
    public Trayecto agregar(Trayecto unObjeto) {
        unObjeto.getTramos().forEach(repoTramos::agregar);
        return super.agregar(unObjeto);
    }

    @Override
    public Trayecto modificar(Trayecto unObjeto) {
        //o son los mismos tramos, o hay uno nuevo, pero no se pueden eliminar tramos
        unObjeto.getTramos().forEach(tr -> {
            if(repoTramos.buscar(tr.getId()).isPresent())
                repoTramos.modificar(tr);
            else
                repoTramos.agregar(tr);
        });
        return super.modificar(unObjeto);
    }

    @Override
    public void modificar(Integer id, Trayecto unObjeto) {
        //o son los mismos tramos, o hay uno nuevo, pero no se pueden eliminar tramos
        unObjeto.getTramos().forEach(tr -> {
            if(tr.getId() != null && repoTramos.buscar(tr.getId()).isPresent())
                repoTramos.modificar(tr.getId(),tr);
            else
                repoTramos.agregar(tr);
        });
        super.modificar(id, unObjeto);
    }

    @Override
    public void eliminar(Trayecto unObjeto) {
        unObjeto.getTramos().forEach(repoTramos::eliminar);
        super.eliminar(unObjeto);
    }
}
