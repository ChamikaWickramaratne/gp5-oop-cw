package tetris.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HighScoreManagerTest {

    @Test
    void testAddScoreAndKeepSorted() {
        FakeHighScoreManager fake = new FakeHighScoreManager();

        fake.addScore(new Score("Alice", 300));
        fake.addScore(new Score("Bob", 1000));
        fake.addScore(new Score("Charlie", 600));

        List<Score> scores = fake.loadScores();

        assertEquals(3, scores.size());
        assertEquals("Bob", scores.get(0).playerName);
        assertEquals(1000, scores.get(0).points);
        assertEquals("Charlie", scores.get(1).playerName);
        assertEquals("Alice", scores.get(2).playerName);
    }
}