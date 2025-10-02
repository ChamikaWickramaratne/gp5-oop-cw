package tetris.model.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class HighScoreManager {
    // 1) Primary, portable location: ~/.tetris/scores.json
    private static final Path HOME_DIR = Paths.get(System.getProperty("user.home"), ".tetris");
    private static final Path HOME_FILE = HOME_DIR.resolve("scores.json");

    // 2) Fallback: working directory (next to the app)
    private static final Path WD_FILE = Paths.get(System.getProperty("user.dir")).resolve("scores.json");

    // Read-only default inside the JAR (optional)
    private static final String CLASSPATH_DEFAULT = "/scores.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Type scoreListType = new TypeToken<List<Score>>(){}.getType();

    private Path resolveWritablePath() {
        // Prefer ~/.tetris
        try {
            Files.createDirectories(HOME_DIR);
            if (!Files.exists(HOME_FILE)) seedFromClasspathOrEmpty(HOME_FILE);
            return HOME_FILE;
        } catch (IOException ignored) { }

        // Fallback: working directory
        try {
            if (!Files.exists(WD_FILE)) seedFromClasspathOrEmpty(WD_FILE);
            return WD_FILE;
        } catch (IOException ignored) { }

        // Last resort: temp dir (non-ideal, but avoids crashing)
        try {
            Path tmp = Files.createTempDirectory("tetris");
            Path f = tmp.resolve("scores.json");
            seedFromClasspathOrEmpty(f);
            return f;
        } catch (IOException e) {
            // Give up: return a path that will cause load to return empty and save to no-op
            return null;
        }
    }

    private void seedFromClasspathOrEmpty(Path target) throws IOException {
        try (InputStream in = HighScoreManager.class.getResourceAsStream(CLASSPATH_DEFAULT)) {
            if (in != null) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        }
        Files.writeString(target, "[]", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public List<Score> loadScores() {
        Path path = resolveWritablePath();
        if (path == null) return new ArrayList<>();
        try (Reader r = Files.newBufferedReader(path)) {
            List<Score> list = gson.fromJson(r, scoreListType);
            return (list != null) ? list : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void saveScores(List<Score> scores) {
        Path path = resolveWritablePath();
        if (path == null) return; // nowhere to write, silently ignore
        try (Writer w = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(scores, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addScore(Score score) {
        List<Score> scores = loadScores();
        scores.add(score);
        scores.sort((a, b) -> Integer.compare(b.points, a.points));
        if (scores.size() > 10) scores = new ArrayList<>(scores.subList(0, 10));
        saveScores(scores);
    }

    public void clear() {
        saveScores(new ArrayList<>());
    }
}
