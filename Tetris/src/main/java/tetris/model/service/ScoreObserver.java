// src/main/java/tetris/model/service/ScoreObserver.java
package tetris.model.service;

public interface ScoreObserver {
    void onScoreChanged(int newScore);
}
