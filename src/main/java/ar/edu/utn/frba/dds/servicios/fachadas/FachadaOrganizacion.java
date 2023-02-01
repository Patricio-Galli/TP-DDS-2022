package ar.edu.utn.frba.dds.servicios.fachadas;

import ar.edu.utn.frba.dds.entities.exceptions.TrayectoSinMiembrosException;
import ar.edu.utn.frba.dds.entities.lugares.Organizacion;
import ar.edu.utn.frba.dds.entities.lugares.Sector;
import ar.edu.utn.frba.dds.entities.medibles.Categoria;
import ar.edu.utn.frba.dds.entities.medibles.FactorEmision;
import ar.edu.utn.frba.dds.entities.medibles.Medible;
import ar.edu.utn.frba.dds.entities.medibles.Periodo;
import ar.edu.utn.frba.dds.entities.personas.Miembro;
import ar.edu.utn.frba.dds.entities.medibles.Trayecto;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.repositories.utils.EntityManagerHelper;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.MedicionSinFactorEmisionException;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.MiembroSinOrganizacionesException;
import ar.edu.utn.frba.dds.repositories.RepoFactores;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;

import java.util.*;

public class FachadaOrganizacion implements FachadaOrg {
    private final RepoFactores repoFactores;

    public FachadaOrganizacion() {
        repoFactores = FactoryRepositorio.getByOutputType(RepoFactores.class);
    }

    public FachadaOrganizacion(RepoFactores repoFactores) {
        this.repoFactores = repoFactores;
    }

    @Override
    public void cargarParametros(Map<String, Float> parametrosSistema) {
        parametrosSistema.forEach(this::cargarParametro);
    }

    @Override
    public Float obtenerHU(Collection<Medible> mediciones) {
        return (float) mediciones.stream().mapToDouble(medible -> {
            String[] subCategoria = medible.getCategoria().split("-", 2);
            Categoria categoria = new Categoria(subCategoria[0].trim(), subCategoria[1].trim());

            return getValorAndUpdateRow(medible) * repoFactores.findByCategoria(categoria).stream().findFirst()
                    .orElseThrow(() -> new MedicionSinFactorEmisionException(medible.getCategoria()))
                    .getValor();
        }).reduce(0, Double::sum);
    }

    public void cargarParametro(String nombreFactor, Float valor) {
        String[] categoriaString = nombreFactor.split(":");
        String[] subCategoria = categoriaString[0].split("->");
        String unidad = categoriaString[1].trim();

        Categoria categoria = new Categoria(subCategoria[0].trim(), subCategoria[1].trim());
        Optional<FactorEmision> optFactor = repoFactores.findByCategoria(categoria).stream().findFirst();

        if(optFactor.isPresent()) {
            optFactor.get().setValor(valor);
        } else {
            this.repoFactores.agregar(new FactorEmision(categoria, unidad, valor));
        }
    }

    public void mostrarParametros() {
        repoFactores.buscarTodos().forEach(System.out::println);
    }

    public Float calcularImpactoOrganizacion(Organizacion organizacion, Periodo periodo) {
        Float mediciones = calcularImpactoMediciones(organizacion, periodo);
        Float trayectos = calcularImpactoTrayectos(organizacion, periodo);

        return mediciones + trayectos;
    }

    private Float calcularImpactoMediciones(Organizacion organizacion, Periodo periodo) {
        return organizacion.getMediciones().stream()
                .map(me -> obtenerHU(Collections.singletonList(me)) *
                        factorEquivalenciaPeriodos(periodo, me.getPeriodo()))
                .reduce(Float::sum).orElse(0F);
    }

    public Float calcularImpactoTrayectos(Miembro miembro, Periodo periodo) {
        return miembro.getTrayectos().stream()
                .map(trayecto -> {
                    Float trayectos = obtenerHU(new ArrayList<>(trayecto.getTramos())) *
                                    factorProporcionalTrayecto(trayecto, miembro, periodo);
                    return trayectos;
                })
                .reduce(Float::sum).orElse(0F);
    }

    public Float calcularImpactoTrayectos(Sector sector, Periodo periodo) {
        return sector.getMiembros().stream()
                .map(miembro -> {
                    Float trayectos = calcularImpactoTrayectos(miembro, periodo);
                    return trayectos;
                })
                .reduce(Float::sum).orElse(0F);
    }

    private Float calcularImpactoTrayectos(Organizacion org, Periodo periodo) {
        return org.getSectores().stream()
                .map(sector -> {
                    Float trayectos = calcularImpactoTrayectos(sector, periodo);
                    return  trayectos;
                })
                .reduce(Float::sum).orElse(0F);
    }

    public float factorProporcionalTrayecto(Trayecto trayecto, Miembro miembro, Periodo periodo) {
        return factorEquivalenciaPeriodos(periodo, trayecto.getPeriodo()) *
                factorProporcionalCompartido(trayecto) *
                factorProporcionalOrganizaciones(miembro);
    }

    public float factorEquivalenciaPeriodos(Periodo periodoDeseado, Periodo periodoMedible) {
        if(!periodoDeseado.getAnio().equals(periodoMedible.getAnio()))
            return 0;

        if(periodoDeseado.getPeriodicidad().equals('A'))
            return 1;

        if(periodoMedible.getPeriodicidad().equals('A'))
            return 1/12F;

        if(!periodoDeseado.getMes().equals(periodoMedible.getMes()))
            return 0;

        return 1;
    }

    private float factorProporcionalCompartido(Trayecto trayecto) {
        if(trayecto.cantidadDeMiembros().equals(0))
            throw new TrayectoSinMiembrosException(trayecto);

        return 1F / trayecto.cantidadDeMiembros();
    }

    private float factorProporcionalOrganizaciones(Miembro miembro) {
        if(miembro.cantidadDeOrganizacionesDondeTrabaja().equals(0))
            throw new MiembroSinOrganizacionesException(miembro);

        return 1F / miembro.cantidadDeOrganizacionesDondeTrabaja();
    }

    private float getValorAndUpdateRow(Medible medible) {
        Float datoActividad = medible.getValor();

        EntityManagerHelper.getEntityManager().getTransaction().begin();
        EntityManagerHelper.getEntityManager().persist(medible);
        EntityManagerHelper.getEntityManager().getTransaction().commit();

        return datoActividad;
    }
}
