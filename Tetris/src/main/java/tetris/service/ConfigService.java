package tetris.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigService {
    private static final String FILE_NAME = "config.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private String getFilePath() {
        return Paths.get("src", "main", "resources", FILE_NAME).toString();
    }

    public Config loadConfig() {
        try (Reader reader = Files.newBufferedReader(Paths.get(getFilePath()))) {
            return gson.fromJson(reader, Config.class);
        } catch (IOException e) {
            // default values if missing
            return new Config(10, 20, true, true);
        }
    }

    public void saveConfig(Config config) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(getFilePath()))) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
