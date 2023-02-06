package ar.edu.utn.frba.dds.framework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class FactoryClassLoaderTest {
//      TODO: es posible probar un framework sin código de cliente?

    @Test
    void seCarganCorrectamenteLasClasesDeLaRuta() throws IOException, ClassNotFoundException {
        Assertions.assertEquals(10, FactoryClassLoader.getClasses("ar.edu.utn.frba.dds.entities.lugares").size());
    }


}
