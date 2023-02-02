package ar.edu.utn.frba.dds.framework;

import ar.edu.utn.frba.dds.server.SystemProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class FactoryClassLoaderTest {

    @Test
    void seCargarCorrectamenteLasClasesDeLaRuta() throws IOException, ClassNotFoundException {
        Assertions.assertEquals(FactoryClassLoader.getClasses("resources/framework").size(), 2);
    }
}
