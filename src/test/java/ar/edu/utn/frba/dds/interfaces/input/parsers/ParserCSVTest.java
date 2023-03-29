package ar.edu.utn.frba.dds.interfaces.input.parsers;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class ParserCSVTest {

    @Test
    void laLibreriaPuedeTransformarCSVEnInstancia() throws IOException {
        List<PruebaConNombres> pruebas = new ParserCSV<>(PruebaConNombres.class)
                .parseFileToCollection("src/test/resources/ParserCsv.csv");

        Assertions.assertEquals("hola", pruebas.get(0).getFoo());
        Assertions.assertEquals(456, pruebas.get(1).getBar());
    }

    @Test
    void laLibreriaPuedeTransformarCSVEnInstanciaConPosicion() throws IOException {
        List<PruebaConOrden> pruebas = new ParserCSV<>(PruebaConOrden.class)
                .parseFileToCollection("src/test/resources/ParserCsv.csv");

        Assertions.assertEquals("hola", pruebas.get(0).getFoo());
        Assertions.assertEquals(456, pruebas.get(1).getBar());
    }

    public static class PruebaConOrden {
        @CsvBindByPosition(position = 1)
        private Integer bar;
        @CsvBindByPosition(position = 0)
        private String foo;

        public PruebaConOrden() {
        }

        public PruebaConOrden(String foo, Integer bar) {
            this.foo = foo;
            this.bar = bar;
        }

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

    public static class PruebaConNombres {
        @CsvBindByName(column = "foo")
        private String foo;
        @CsvBindByName
        private Integer bar;

        public PruebaConNombres() {
        }

        public PruebaConNombres(String foo, Integer bar) {
            this.foo = foo;
            this.bar = bar;
        }

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
}
