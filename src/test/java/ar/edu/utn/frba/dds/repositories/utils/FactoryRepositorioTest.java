package ar.edu.utn.frba.dds.repositories.utils;

import ar.edu.utn.frba.dds.repositories.daos.DAOHibernate;
import ar.edu.utn.frba.dds.repositories.impl.jpa.RepositorioPersistente;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class FactoryRepositorioTest {

    public static <T> List<Class<? extends T>> getClasses(String packageName, Class<T> inheritedType) {
        List<Class<? extends T>> result = new ArrayList<>();
        ArrayList<Class<?>> classes = new ArrayList<>();
        try {
            classes = getClasses(packageName);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
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

    @Test
    void getClasses2() {
        String packageName = "ar.edu.utn.frba.dds.repositories.impl.jpa";
        ArrayList<Class<?>> classes = new ArrayList<>();
        try {
            classes = getClasses(packageName);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println(classes);
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

    @Test
    void logroGenerarUnMapaDecente() {
        Map<Class<?>, RepositorioPersistente> mapita = new HashMap<>();

        getClasses("ar.edu.utn.frba.dds.repositories.impl.jpa", RepositorioPersistente.class)
                .forEach(inter -> {
                    String className = inter.getGenericSuperclass().getTypeName().split("<")[1];
                    try {
                        Class<?> key = Class.forName(className.substring(0, className.length() - 1));
                        mapita.put(key, inter.getConstructor(DAOHibernate.class).newInstance(new DAOHibernate<>(key)));
                    } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                             IllegalAccessException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
            }
        });
    }

}
