package tetris.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HighScoreManager {
    private static final String FILE_NAME = "scores.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Type scoreListType = new TypeToken<List<Score>>(){}.getType();

    private String getFilePath() {
        return Paths.get("src", "main", "resources", FILE_NAME).toString();
    }

    // Load all scores from file
    public List<Score> loadScores() {
        try (Reader reader = Files.newBufferedReader(Paths.get(getFilePath()))) {
            return gson.fromJson(reader, scoreListType);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    // Save the given list of scores to file
    public void saveScores(List<Score> scores) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(getFilePath()))) {
            gson.toJson(scores, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add a new score (keeps only top 10 sorted)
    public void addScore(Score score) {
        List<Score> scores = loadScores();
        scores.add(score);
        scores.sort((a, b) -> Integer.compare(b.points, a.points)); // highest first
        if (scores.size() > 10) {
            scores = new ArrayList<>(scores.subList(0, 10)); // ensure subList copy
        }
        saveScores(scores);
    }

    // Clear all scores (used for "Clear High Scores" button)
    public void clear() {
        saveScores(new ArrayList<>()); // write empty list to file
    }
}
