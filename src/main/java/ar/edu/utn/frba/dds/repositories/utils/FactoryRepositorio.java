package ar.edu.utn.frba.dds.repositories.utils;

import ar.edu.utn.frba.dds.repositories.MiHuellaFrameworkException;
import ar.edu.utn.frba.dds.repositories.daos.DAOHibernate;
import ar.edu.utn.frba.dds.repositories.Repositorio;
import ar.edu.utn.frba.dds.repositories.impl.memory.RepositorioMemoria;
import ar.edu.utn.frba.dds.repositories.impl.jpa.RepositorioPersistente;
import ar.edu.utn.frba.dds.repositories.daos.DAOMemoria;
import ar.edu.utn.frba.dds.server.SystemProperties;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
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
                .findFirst().orElseThrow(() -> new MiHuellaFrameworkException("The class " + outputType.getName()
                        + " could not be get from context."));
    }

    public static <T> List<Class<? extends T>> getClasses(String packageName, Class<T> inheritedType) {
        List<Class<? extends T>> result = new ArrayList<>();
        ArrayList<Class<?>> classes = new ArrayList<>();
        try {
            classes = getClasses(packageName);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        for (Class<?> next : classes) {
            Class<?> superClass = next.getSuperclass();
            while (!superClass.equals(Object.class)) {
                if (superClass.equals(inheritedType))
                    result.add((Class<? extends T>) next);
                superClass = superClass.getSuperclass();
            }
        }
        return result;
    }

    public static ArrayList<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }

        return classes;
    }

    public static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class
                        .forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    public static void cargarRepositoriosEnMemoria() {
        getClasses("ar.edu.utn.frba.dds.repositories.impl.memory", RepositorioMemoria.class)
                .forEach(inter -> {
                    String className = inter.getGenericSuperclass().getTypeName().split("<")[1];
                    try {
                        Class<?> key = Class.forName(className.substring(0, className.length() - 1));
                        repos.put(key, inter.getConstructor(DAOMemoria.class).newInstance(new DAOMemoria<>(key)));
                    } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                             IllegalAccessException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static void cargarRepositoriosPersistentes() {
        getClasses("ar.edu.utn.frba.dds.repositories.impl.jpa", RepositorioPersistente.class)
                .forEach(inter -> {
                    String className = inter.getGenericSuperclass().getTypeName().split("<")[1];
                    try {
                        Class<?> key = Class.forName(className.substring(0, className.length() - 1));
                        repos.put(key, inter.getConstructor(DAOHibernate.class).newInstance(new DAOHibernate<>(key)));
                    } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                             IllegalAccessException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
