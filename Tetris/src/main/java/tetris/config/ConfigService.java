// src/main/java/tetris/config/ConfigService.java
package tetris.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ConfigService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final Path CONFIG_PATH = Path.of("TetrisConfig.json");

    private ConfigService() {}

    /**
     * Loads config from disk. If file is missing or corrupt, returns defaults.
     * Also updates the TetrisConfig singleton.
     */
    public static TetrisConfig load() {
        TetrisConfig cfg = null;
        try {
            if (Files.exists(CONFIG_PATH)) {
                cfg = MAPPER.readValue(Files.readAllBytes(CONFIG_PATH), TetrisConfig.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (cfg == null) {
            cfg = new TetrisConfig(); // fallback defaults
        }

        // ✅ Make sure singleton stays synced with disk state
        TetrisConfig.setInstance(cfg);
        return cfg;
    }

    /**
     * Saves the given config to disk and updates the singleton.
     */
    public static void save(TetrisConfig cfg) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(cfg);
            Files.write(CONFIG_PATH, json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            TetrisConfig.setInstance(cfg);
            System.out.println("✅ Saved config to: " + CONFIG_PATH.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Path configPath() {
        return CONFIG_PATH;
    }

    /**
     * Force-refreshes the singleton by reloading from disk.
     */
    public static TetrisConfig reloadSingleton() {
        return load();
    }
}
