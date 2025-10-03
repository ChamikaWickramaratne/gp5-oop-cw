package tetris;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.paint.Color;
import tetris.model.Board;
import tetris.model.TetrominoType;
import tetris.model.Vec;
import tetris.model.piece.ActivePiece;
import tetris.model.service.HighScoreManager;
import tetris.model.service.Score;
import tetris.config.ConfigService;
import tetris.config.TetrisConfig;
import tetris.model.service.ScoreService;

import java.util.ArrayList;
import java.util.List;

class GameTests {

    @Test
    void testBoardInsideBounds() {
        Board board = new Board(10, 20);
        assertTrue(board.inside(5, 5));
        assertFalse(board.inside(-1, 0));
        assertFalse(board.inside(10, 19));
    }

    @Test
    void testBoardInvalidSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Board(0, 20));
        assertThrows(IllegalArgumentException.class, () -> new Board(10, -5));
    }

    @Test
    void testActivePieceRotation() {
        ActivePiece piece = new ActivePiece(TetrominoType.T, new Vec(5, 5));
        var before = piece.worldCells();
        piece.rotateCW();
        var after = piece.worldCells();
        assertNotEquals(before, after, "Rotating piece should change occupied cells");
    }

    @Test
    void testHighScoreSaveAndLoad() {
        HighScoreManager manager = new HighScoreManager();
        List<Score> scores = new ArrayList<>();
        scores.add(new Score("Jay", 500));
        manager.saveScores(scores);

        List<Score> loaded = manager.loadScores();
        assertEquals(1, loaded.size());
        assertEquals("Jay", loaded.get(0).playerName);
        assertEquals(500, loaded.get(0).points);
    }


    @Test
    public void testConfigSaveAndLoad() {
        // Create a config with custom values
        TetrisConfig config = new TetrisConfig();
        config.setGameLevel(7);
        config.setFieldWidth(12);
        config.setFieldHeight(22);
        config.setMusic(false);

        // Save to disk
        ConfigService.save(config);

        // Load back from disk
        TetrisConfig loaded = ConfigService.load();

        // Verify values are correctly persisted
        assertEquals(7, loaded.getGameLevel());
        assertEquals(12, loaded.getFieldWidth());
        assertEquals(22, loaded.getFieldHeight());
        assertFalse(loaded.isMusic());
    }

    @Test
    public void testPointsForMultipleLines() {
        assertEquals(100, ScoreService.pointsFor(1));
        assertEquals(300, ScoreService.pointsFor(2));
        assertEquals(600, ScoreService.pointsFor(3));
    }

    @Test
    public void testSingletonConfigInstance() {
        TetrisConfig config1 = TetrisConfig.getInstance();
        TetrisConfig config2 = TetrisConfig.getInstance();
        assertSame(config1, config2); // both references must be identical
    }

    @Test
    public void testAddScoreSortsDescending() {
        HighScoreManager manager = new HighScoreManager();
        manager.clear();
        manager.addScore(new Score("Alice", 500, "Human"));
        manager.addScore(new Score("Bob", 1000, "Human"));

        List<Score> scores = manager.loadScores();
        assertEquals("Bob", scores.get(0).playerName); // highest first
    }

    @Test
    public void testScoreCreation() {
        Score s = new Score("TestPlayer", 750, "Single",
                10, 20, 5, "Classic");

        assertEquals("TestPlayer", s.getPlayerName());
        assertEquals(750, s.getPoints());
        assertEquals("Single", s.getGameType());
        assertEquals(10, s.getBoardWidth());
        assertEquals(20, s.getBoardHeight());
        assertEquals(5, s.getLevel());
        assertEquals("Classic", s.getMode());
    }
}
