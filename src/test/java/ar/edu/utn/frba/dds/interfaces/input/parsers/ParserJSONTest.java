package ar.edu.utn.frba.dds.interfaces.input.parsers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class ParserJSONTest {

    @Test
    void laLibreriaPuedeTransformarJsonEnInstancia() {
        String json = "{ \"foo\": \"hola\", \"bar\": 123 }";
        Prueba prueba = new ParserJSON<>(Prueba.class).parseElement(json);

        Assertions.assertEquals("hola", prueba.getFoo());
        Assertions.assertEquals(123, prueba.getBar());
    }

    @Test
    void laLibreriaPuedeTransformarJsonEnInstanciaDeLista() {
        String json = "[" +
                "{ \"foo\": \"hola\", \"bar\": 123 }," +
                "{ \"foo\": \"chau\", \"bar\": 456 }" +
                "]";
        List<Prueba> pruebas = new ParserJSON<>(Prueba.class).parseCollection(json);

        Assertions.assertEquals(2, pruebas.size());
        Assertions.assertEquals("hola", pruebas.get(0).getFoo());
        Assertions.assertEquals(456, pruebas.get(1).getBar());
    }

    @Test
    void laLibreriaPuedeTransformarUnArchivoJsonEnInstancia() throws IOException {
        Prueba prueba = new ParserJSON<>(Prueba.class).parseFileToElement("src/test/resources/ParserJson.json");

        Assertions.assertEquals("hola", prueba.getFoo());
        Assertions.assertEquals(123, prueba.getBar());
    }

    @Test
    void laLibreriaPuedeTransformarUnArchivoJsonEnInstanciaDeLista() throws IOException {
        List<Prueba> pruebas = new ParserJSON<>(Prueba.class).parseFileToCollection("src/test/resources/ParserJsonCollection.json");

        Assertions.assertEquals(2, pruebas.size());
        Assertions.assertEquals("hola", pruebas.get(0).getFoo());
        Assertions.assertEquals(456, pruebas.get(1).getBar());
    }

    @Test
    void laLibreriaPuedeTransformarJsonEnInstanciaDeTipoGenerico() {
        String json = "{ \"foo\": {" +
                "\"foo\": \"chau\", \"bar\": 456" +
                "} }";
        Caja<Prueba> prueba =
                new ParserJSON<>(Caja.class, Prueba.class)
                        .parseBounded(json);

        Assertions.assertEquals("chau", prueba.getFoo().getFoo());
        Assertions.assertEquals(456, prueba.getFoo().getBar());
    }

    private class Prueba {
        private String foo;
        private Integer bar;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public Integer getBar() {
            return bar;
        }

        public void setBar(Integer bar) {
            this.bar = bar;
        }
    }

    private static class Caja<E> {
        private E foo;

        public E getFoo() {
            return foo;
        }

        public void setFoo(E foo) {
            this.foo = foo;
        }
    }
}
