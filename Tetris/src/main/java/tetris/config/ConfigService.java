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

    // JSON file in project root
    private static final Path CONFIG_PATH = Path.of("TetrisConfig.json");

    private ConfigService() {}

    public static TetrisConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                return MAPPER.readValue(Files.readAllBytes(CONFIG_PATH), TetrisConfig.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Fallback if missing file → create default config
        return new TetrisConfig();
    }

    public static void save(TetrisConfig cfg) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(cfg);
            Files.write(CONFIG_PATH, json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

            System.out.println("Saved config to: " + CONFIG_PATH.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Path configPath() {
        return CONFIG_PATH;
    }
}