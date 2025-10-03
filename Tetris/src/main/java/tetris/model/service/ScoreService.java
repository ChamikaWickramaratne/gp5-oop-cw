package tetris.model.service;

import java.util.ArrayList;
import java.util.List;

public final class ScoreService {
    private static final List<ScoreObserver> observers = new ArrayList<>();

    private ScoreService() {}

    public static int pointsFor(int lines) {
        if (lines <= 0) return 0;

        int points = 0;
        int increment = 100;
        for (int i = 1; i <= lines; i++) {
            points += increment;
            increment += 100;
        }
        return points;
    }

    public static void addObserver(ScoreObserver observer) {
        observers.add(observer);
    }

    public static void removeObserver(ScoreObserver observer) {
        observers.remove(observer);
    }

    public static void notifyScoreChanged(int newScore) {
        for (ScoreObserver obs : observers) {
            obs.onScoreChanged(newScore);
        }
    }
}
