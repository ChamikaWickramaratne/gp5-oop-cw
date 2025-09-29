package tetris.service;

public final class ScoreService {
    public ScoreService() {}

    public static int pointsFor(int lines) {
        switch (lines) {
            case 1: return 100;
            case 2: return 300;
            case 3: return 600;
            case 4: return 1000;
            default: return 0;
        }
    }
}