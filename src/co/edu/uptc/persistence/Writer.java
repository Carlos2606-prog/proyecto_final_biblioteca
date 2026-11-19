package co.edu.uptc.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class Writer {
    private Gson gson;
    
    public Writer() {
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .setPrettyPrinting()
            .create();
    }
    
    public <T> void writeList(String path, List<T> data) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        }
    }
}