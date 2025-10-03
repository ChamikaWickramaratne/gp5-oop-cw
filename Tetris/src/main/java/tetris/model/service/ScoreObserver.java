package tetris.model.service;

public interface ScoreObserver {
    void onScoreChanged(int newScore);
}
