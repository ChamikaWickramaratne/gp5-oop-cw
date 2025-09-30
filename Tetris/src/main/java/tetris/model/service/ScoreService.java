package tetris.model.service;

public final class ScoreService {
    private ScoreService() {}

    public static int pointsFor(int lines) {
        if (lines <= 0) return 0;

        int points = 0;
        int increment = 100;

        for (int i = 1; i <= lines; i++) {
            points += increment;
            increment += 100; // difference increases by 100 each step
        }

        return points;
    }
}
