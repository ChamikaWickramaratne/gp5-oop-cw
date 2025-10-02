package tetris.service;

public class DummyScoreService {
    public int pointsFor(int lines) {
        return lines * 100; // simple logic
    }
}
