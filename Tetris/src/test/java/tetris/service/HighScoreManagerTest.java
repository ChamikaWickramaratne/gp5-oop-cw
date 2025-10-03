package tetris.service;

import org.junit.jupiter.api.Test;
import tetris.model.PlayerType;
import tetris.model.service.Score;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HighScoreManagerTest {

    @Test
    void testAddScoreAndKeepSorted() {
        FakeHighScoreManager fake = new FakeHighScoreManager();

        fake.addScore(new Score("Alice", 300,"SinglePlayer",15,10,1, "HUMAN"));
        fake.addScore(new Score("Bob", 1000,"SinglePlayer",15,10,1, "HUMAN"));
        fake.addScore(new Score("Charlie", 600,"SinglePlayer",15,10,1, "HUMAN"));

        List<Score> scores = fake.loadScores();

        assertEquals(3, scores.size());
        assertEquals("Bob", scores.get(0).playerName);
        assertEquals(1000, scores.get(0).points);
        assertEquals("Charlie", scores.get(1).playerName);
        assertEquals("Alice", scores.get(2).playerName);
    }
}