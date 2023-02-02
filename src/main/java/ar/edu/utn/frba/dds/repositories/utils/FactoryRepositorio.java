package ar.edu.utn.frba.dds.repositories.utils;

import ar.edu.utn.frba.dds.framework.FactoryClassLoader;
import ar.edu.utn.frba.dds.repositories.MiHuellaFrameworkException;
import ar.edu.utn.frba.dds.repositories.daos.DAOHibernate;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.repositories.impl.memory.RepositorioMemoria;
import ar.edu.utn.frba.dds.repositories.impl.jpa.RepositorioPersistente;
import ar.edu.utn.frba.dds.repositories.daos.DAOMemoria;
import ar.edu.utn.frba.dds.server.SystemProperties;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class FactoryRepositorio {
    private static HashMap<Class<?>, Repositorio> repos;

    static {
        repos = new HashMap<>();
    }

    public static <T> Repositorio<T> getByParameterType(Class<T> type) {
        return (Repositorio<T>) Optional.ofNullable(repos.get(type))
                .orElseGet(() -> SystemProperties.isJpa()
                                ? new RepositorioPersistente<>(new DAOHibernate<>(type))
                                : new RepositorioMemoria<>(new DAOMemoria<>(type))
                );
    }

    public static <T, P> P get(Class<T> type, Class<P> outputType) {
        try {
            return outputType.cast(Objects.requireNonNull(repos.get(type)));
        } catch (NullPointerException e) {
            throw new MiHuellaFrameworkException("The class " + outputType.getName() + " could not be get from context.");
        }
    }

    public static <P> P getByOutputType(Class<P> outputType) {
        return repos.values().stream()
                .filter(repositorio -> repositorio.getClass().equals(outputType) ||
                        Arrays.asList(repositorio.getClass().getInterfaces()).contains(outputType))
                .map(outputType::cast)
                .findFirst()
                .orElseThrow(() -> new MiHuellaFrameworkException("The class " + outputType.getName()
                        + " could not be get from context."));
    }

    public static void cargarRepositoriosEnMemoria(String basePath) {
        FactoryClassLoader.getClasses(basePath, RepositorioMemoria.class)
                .forEach(inter -> {
                    String className = inter.getGenericSuperclass().getTypeName().split("<")[1];
                    try {
                        Class<?> key = Class.forName(className.substring(0, className.length() - 1));
                        repos.put(key, inter.getConstructor(DAOMemoria.class).newInstance(new DAOMemoria<>(key)));
                    } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                             IllegalAccessException | NoSuchMethodException e) {
                        throw new MiHuellaFrameworkException(e.getMessage());
                    }
                });
    }

    public static void cargarRepositoriosPersistentes(String basePath) {
        FactoryClassLoader.getClasses(basePath, RepositorioPersistente.class)
                .forEach(inter -> {
                    String className = inter.getGenericSuperclass().getTypeName().split("<")[1];
                    try {
                        Class<?> key = Class.forName(className.substring(0, className.length() - 1));
                        repos.put(key, inter.getConstructor(DAOHibernate.class).newInstance(new DAOHibernate<>(key)));
                    } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                             IllegalAccessException | NoSuchMethodException e) {
                        throw new MiHuellaFrameworkException(e.getMessage());
                    }
                });
    }
}
