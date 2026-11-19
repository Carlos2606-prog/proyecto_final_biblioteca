package co.edu.uptc.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Reader {
    private Gson gson;
    
    public Reader() {
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .setPrettyPrinting()
            .create();
    }
    
    public <T> List<T> readList(String path, Type type) throws IOException {
        File file = new File(path);
        
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            return new ArrayList<>();
        }
        
        try (FileReader reader = new FileReader(file)) {
            List<T> result = gson.fromJson(reader, type);
            return result != null ? result : new ArrayList<>();
        }
    }
}