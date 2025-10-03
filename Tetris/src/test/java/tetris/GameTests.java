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
        scores.add(new Score("Jay", 500,"SinglePlayer",15,10,1, "HUMAN"));
        manager.saveScores(scores);

        List<Score> loaded = manager.loadScores();
        assertEquals(1, loaded.size());
        assertEquals("Jay", loaded.get(0).playerName);
        assertEquals(500, loaded.get(0).points);
    }


    @Test
    void testConfigSaveAndLoad() {
        TetrisConfig config = new TetrisConfig();
        config.setFieldWidth(15);
        config.setFieldHeight(25);

        ConfigService.save(config);
        TetrisConfig loaded = ConfigService.load();

        assertEquals(15, loaded.getFieldWidth());
        assertEquals(25, loaded.getFieldHeight());
    }

}
