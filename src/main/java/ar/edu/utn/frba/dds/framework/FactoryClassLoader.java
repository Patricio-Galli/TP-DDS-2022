package ar.edu.utn.frba.dds.framework;

import ar.edu.utn.frba.dds.repositories.MiHuellaFrameworkException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FactoryClassLoader {
    public static <T> List<Class<? extends T>> getClasses(String packageName, Class<T> inheritedType) {
        try {
            return getClasses(packageName).stream()
                    .flatMap(clazz -> {
                        Class<?> superClass = clazz.getSuperclass();
                        while (!superClass.equals(Object.class)) {
                            if (superClass.equals(inheritedType))
                                return Stream.of((Class<? extends T>) clazz);
                            superClass = superClass.getSuperclass();
                        }
                        return Stream.empty();
                    }).collect(Collectors.toList());
        } catch (ClassNotFoundException | IOException e) {
            throw new MiHuellaFrameworkException("Could not load classes of the directory: " + packageName);
        }
    }

    public static List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
//        String helper = new File(path).getAbsolutePath();
//        String path = packageName;
//        File currentDirFile = new File(packageName);
//        String helper = currentDirFile.getAbsolutePath();
//        String currentDir = helper.substring(0, helper.length() - currentDirFile.getCanonicalPath().length());//this line may need a try-catch block

        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }

        return dirs.stream()
                .filter(directory -> !directory.getAbsolutePath().contains("test-classes"))
                .flatMap(directory -> findClasses(directory, packageName).stream())
                .collect(Collectors.toList());
    }

    public static List<Class<?>> findClasses(File directory, String packageName) {
        return !directory.exists() ? new ArrayList<>() : Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                .flatMap(file -> {
                    if (file.isDirectory()) {
                        assert !file.getName().contains(".");
                        return findClasses(file, packageName + "." + file.getName()).stream();
                    } else if (file.getName().endsWith(".class")) {
                        String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                        try {
                            return Stream.of(Class.forName(className));
                        } catch (ClassNotFoundException e) {
                            throw new MiHuellaFrameworkException("Could not load class by the path " + className);
                        }
                    }
                    return Stream.empty();
                }).collect(Collectors.toList());
    }
}