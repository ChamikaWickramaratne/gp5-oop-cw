package tetris.service;

public final class ScoreService {
    private ScoreService() {}

    // Linear scoring: 1→100, 2→200, 3→300, 4→400, etc.
    public static int pointsFor(int lines) {
        return Math.max(lines, 0) * 100;
    }
}