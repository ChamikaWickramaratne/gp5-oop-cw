package tetris.service;

import java.util.ArrayList;
import java.util.List;

public class FakeHighScoreManager extends HighScoreManager {

    private final List<Score> scores = new ArrayList<>();

    @Override
    public void addScore(Score score) {
        scores.add(score);
        scores.sort((a, b) -> Integer.compare(b.points, a.points));
        if (scores.size() > 10) {
            // keep only top 10
            while (scores.size() > 10) {
                scores.remove(scores.size() - 1);
            }
        }
    }

    @Override
    public List<Score> loadScores() {
        return new ArrayList<>(scores); // return copy to mimic file read
    }

    @Override
    public void saveScores(List<Score> scores) {
        this.scores.clear();
        this.scores.addAll(scores);
    }

    @Override
    public void clear() {
        scores.clear();
    }
}