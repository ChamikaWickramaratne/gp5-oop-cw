package tetris.controller;

import javafx.scene.paint.Color;
import tetris.model.Board;
import tetris.model.TetrominoType;
import tetris.model.Vec;
import tetris.model.piece.ActivePiece;
import tetris.model.rules.RotationStrategy;
import tetris.model.rules.SrsRotation;
import tetris.service.ScoreService;
import java.util.Random;

public final class GameController {
    private static final long NORMAL_DROP_NS = 1_000_000_000L;
    private static final long FAST_DROP_NS = 100_000_000L;
    private static final Color[] COLOUR_OPTIONS = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
    private final Random rng = new Random();
    private final RotationStrategy rotator = new SrsRotation();
    private Board board = new Board();
    private ActivePiece current;
    private Color currentColor;
    private long lastDropTime = 0L;
    private long dropSpeed = NORMAL_DROP_NS;
    private boolean paused = false;
    private boolean gameOver = false;
    private int score = 0;

    //start new game
    public void resetGameState() {
        board = new Board();
        score = 0;
        paused = false;
        gameOver = false;
        lastDropTime = 0L;
        dropSpeed = NORMAL_DROP_NS;
    }

    public void spawnNewPiece() {
        //get random piece type
        TetrominoType type = TetrominoType.values()[rng.nextInt(TetrominoType.values().length)];
        Vec[] base = type.offsets();

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        for (Vec v : base) { if (v.x() < minX) minX = v.x(); if (v.x() > maxX) maxX = v.x(); }
        int shapeWidth = (maxX - minX + 1);
        int startCol = Math.max(0, (Board.WIDTH - shapeWidth) / 2 - minX);

        current = new ActivePiece(type, new Vec(startCol, 0));
        currentColor = COLOUR_OPTIONS[rng.nextInt(COLOUR_OPTIONS.length)];

        //game over check. if cant spawn a piece at the top
        for (Vec c : current.worldCells()) {
            if (c.y() < 0 || c.y() >= Board.HEIGHT || c.x() < 0 || c.x() >= Board.WIDTH || board.cells()[c.y()][c.x()] != null) {
                gameOver = true;
                return;
            }
        }
    }

    public void tick(long now) {
        if (!paused && !gameOver) {
            if (lastDropTime == 0) lastDropTime = now;
            else if (now - lastDropTime > dropSpeed) {
                if (!tryBoost()) lockPiece();
                lastDropTime = now;
            }
        }
    }

    public void tryMoveLeft() { if (!paused && !gameOver) move(-1, 0); }
    public void tryMoveRight() { if (!paused && !gameOver) move(1, 0); }
    public void tryRotate() { if (!paused && !gameOver) rotator.tryRotateCW(current, board); }
    public void boost(boolean pressed) { dropSpeed = pressed ? FAST_DROP_NS : NORMAL_DROP_NS; }
    public void pauseGame() { paused = !paused; if (!paused) lastDropTime = 0L; }

    public Board getBoard() { return board; }
    public ActivePiece getCurrentPiece() { return current; }
    public Color getCurrentColor() { return currentColor; }
    public int getScore() { return score; }
    public boolean isPaused() { return paused; }
    public boolean isGameOver() { return gameOver; }
    public void stop() {}

    public boolean tryBoost() {
        current.moveBy(0, +1);
        if (board.canPlace(current)) return true;
        current.moveBy(0, -1);
        return false;
    }

    public void lockPiece() {
        board.lock(current, currentColor);
        int cleared = board.clearLines();
        score += ScoreService.pointsFor(cleared);
        spawnNewPiece();
    }

    private void move(int dx, int dy) {
        current.moveBy(dx, dy);
        if (!board.canPlace(current)) current.moveBy(-dx, -dy);
    }
}
