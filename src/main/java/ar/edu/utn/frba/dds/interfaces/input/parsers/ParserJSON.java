package ar.edu.utn.frba.dds.interfaces.input.parsers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class ParserJSON<T, S> {
    private final Type type;
    private final Class<S> subtype;

    public ParserJSON(Class<T> type, Class<S> subtype) {
        this.type = type;
        this.subtype = subtype;
    }

    public ParserJSON(Class<S> subtype) {
        this.type = List.class;
        this.subtype = subtype;
    }

    public S parseElement(String json) {
        return new Gson().fromJson(json, subtype);
    }

    public List<S> parseCollection(String json) {
        Type listType = TypeToken.getParameterized(type, subtype).getType();
        return new Gson().fromJson(json, listType);
    }

    public S parseFileToElement(String archivo) throws IOException {
        return new Gson().fromJson(new FileReader(archivo), subtype);
    }

    public List<S> parseFileToCollection(String archivo) throws IOException {
        Type listType = TypeToken.getParameterized(type, subtype).getType();
        return new Gson().fromJson(new FileReader(archivo), listType);
    }

    public <P> P parseBounded(String json) {
        Type boundedType = TypeToken.getParameterized(type, subtype).getType();
        return new Gson().fromJson(json, boundedType);
    }
}
