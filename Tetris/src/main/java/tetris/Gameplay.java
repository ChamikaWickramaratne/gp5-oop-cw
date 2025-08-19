package tetris;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.Random;

record Vec(int x, int y) {}

public class Gameplay extends Application {

    private static final int cellSize = 20;
    private final int width = 10;
    private final int height = 20;

    private static final Color[] colourOptions = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW
    };

    enum TetrominoType {
        I, O, T, L, J, S, Z;

        Vec[] offsets() {
            return switch (this) {
                case I -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(2,0), new Vec(3,0) };
                case O -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(0,1), new Vec(1,1) };
                case T -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(2,0), new Vec(1,1) };
                case L -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(2,0), new Vec(2,1) };
                case J -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(2,0), new Vec(0,1) };
                case S -> new Vec[]{ new Vec(1,0), new Vec(2,0), new Vec(0,1), new Vec(1,1) };
                case Z -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(1,1), new Vec(2,1) };
            };
        }
    }

    interface CollisionChecker {
        boolean blocked(int row, int col);
        int rows();
        int cols();
    }

    abstract static class Piece {
        protected final TetrominoType type;
        protected final Color color; // NEW: color stored per piece
        protected int row;
        protected int col;

        protected Piece(TetrominoType type, int startRow, int startCol, Color color) {
            this.type = type;
            this.color = color;
            this.row = startRow;
            this.col = startCol;
        }

        abstract Vec[] offsets();

        protected static Vec rotateCW(Vec v) { return new Vec(v.y(), -v.x()); }

        boolean tryRotateCW(CollisionChecker grid) {
            Vec[] source = offsets();
            Vec[] rotated = new Vec[source.length];
            for (int i = 0; i < source.length; i++) rotated[i] = rotateCW(source[i]);
            if (canPlace(grid, row, col, rotated)) {
                setOffsets(rotated);
                return true;
            }
            return false;
        }

        boolean tryMove(CollisionChecker grid, int dRow, int dCol) {
            if (canPlace(grid, row + dRow, col + dCol, offsets())) {
                row += dRow;
                col += dCol;
                return true;
            }
            return false;
        }

        Vec[] worldCells() {
            Vec[] offs = offsets();
            Vec[] cells = new Vec[offs.length];
            for (int i = 0; i < offs.length; i++) {
                Vec o = offs[i];
                cells[i] = new Vec(col + o.x(), row + o.y());
            }
            return cells;
        }

        protected abstract void setOffsets(Vec[] newOffsets);

        private static boolean canPlace(CollisionChecker grid, int r, int c, Vec[] offs) {
            for (Vec o : offs) {
                int rr = r + o.y();
                int cc = c + o.x();
                if (rr < 0 || rr >= grid.rows() || cc < 0 || cc >= grid.cols()) return false;
                if (grid.blocked(rr, cc)) return false;
            }
            return true;
        }
    }

    static class StandardPiece extends Piece {
        private Vec[] localOffsets;
        StandardPiece(TetrominoType type, int startRow, int startCol, Vec[] baseOffsets, Color color) {
            super(type, startRow, startCol, color); // UPDATED ctor
            this.localOffsets = baseOffsets;
        }
        @Override Vec[] offsets() { return localOffsets; }
        @Override protected void setOffsets(Vec[] newOffsets) { this.localOffsets = newOffsets; }
    }

    private final Random rng = new Random();
    // CHANGED: store Color instead of TetrominoType
    private Color[][] board = new Color[height][width];
    private Piece currentPiece;
    private long lastDropTime = 0L;
    private long dropSpeed = 1_000_000_000L;
    private boolean paused = false;
    private boolean gameOver = false;
    private int score = 0;
    private Label scoreLabel;
    private AnimationTimer timer;

    private final CollisionChecker grid = new CollisionChecker() {
        @Override public boolean blocked(int row, int col) { return board[row][col] != null; }
        @Override public int rows() { return height; }
        @Override public int cols() { return width; }
    };

    @Override
    public void start(Stage stage) {
        // UI
        scoreLabel = new Label();
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox topBar = new HBox(scoreLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        Canvas boardCanvas = new Canvas(
                width * cellSize,
                height * cellSize
        );
        boardCanvas.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            if (timer != null) timer.stop();
            try { new MainMenu().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        HBox backBar = new HBox(backButton);
        backBar.setAlignment(Pos.CENTER);
        backBar.setPadding(new Insets(10));

        Label authorLabel = new Label("Author: Chamika Wickramarathne");
        HBox authorBar = new HBox(authorLabel);
        authorBar.setAlignment(Pos.CENTER);
        authorBar.setPadding(new Insets(5));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(boardCanvas);
        root.setBottom(new VBox(backBar, authorBar));
        root.setStyle("-fx-background-color: #f9f9f9;");
        Scene scene = new Scene(root);
        stage.setScene(scene);

        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case A -> tryMoveLeft();
                case D -> tryMoveRight();
                case S -> boost(true);
                case P -> pauseGame();
                case W, UP -> tryRotate();
            }
        });
        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.S) boost(false);
        });

        stage.setTitle("Tetris");
        stage.setMinWidth(500);
        stage.show();

        resetGameState();
        spawnNewPiece();

        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (!paused && !gameOver) {
                    if (lastDropTime == 0) lastDropTime = now;
                    else if (now - lastDropTime > dropSpeed) {
                        if (!tryBoost()) lockPiece();
                        lastDropTime = now;
                    }
                }
                draw(boardCanvas.getGraphicsContext2D());
            }
        };
        timer.start();
    }

    private void resetGameState() {
        board = new Color[height][width]; // UPDATED type
        score = 0;
        paused = false;
        gameOver = false;
        lastDropTime = 0L;
        dropSpeed = 1_000_000_000L;
        if (scoreLabel != null) scoreLabel.setText("Score: 0");
    }

    //restart method. not used yet. can assign it to a button if needed
    private void restartGame() {
        resetGameState();
        spawnNewPiece();
    }

    private void spawnNewPiece() {
        TetrominoType type = TetrominoType.values()[rng.nextInt(TetrominoType.values().length)];
        Vec[] base = type.offsets();

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        for (Vec v : base) { if (v.x() < minX) minX = v.x(); if (v.x() > maxX) maxX = v.x(); }
        int shapeWidth = (maxX - minX + 1);
        int startCol = Math.max(0, (width - shapeWidth) / 2 - minX);

        // NEW: choose random color for this piece
        Color color = colourOptions[rng.nextInt(colourOptions.length)];
        currentPiece = new StandardPiece(type, 0, startCol, base, color);

        for (Vec c : currentPiece.worldCells()) {
            if (c.y() < 0 || c.y() >= height || c.x() < 0 || c.x() >= width || board[c.y()][c.x()] != null) {
                gameOver = true;
                return;
            }
        }
    }

    private boolean tryBoost() { return currentPiece.tryMove(grid, +1, 0); }

    private void lockPiece() {
        for (Vec c : currentPiece.worldCells()) {
            if (c.y() >= 0 && c.y() < height && c.x() >= 0 && c.x() < width) {
                board[c.y()][c.x()] = currentPiece.color; // UPDATED: store color
            }
        }
        clearFullLines();
        spawnNewPiece();
    }

    private void tryMoveLeft()  { currentPiece.tryMove(grid, 0, -1); }
    private void tryMoveRight() { currentPiece.tryMove(grid, 0, +1); }
    private void tryRotate()    { currentPiece.tryRotateCW(grid); }

    private void boost(boolean pressed) { dropSpeed = pressed ? 100_000_000L : 1_000_000_000L; }

    private void pauseGame() {
        paused = !paused;
        if (!paused) lastDropTime = 0;
    }

    //drawing the board each time
    private void draw(GraphicsContext gc) {
        int W = width, H = height;
        gc.clearRect(0, 0, W * cellSize, H * cellSize);

        //border color
        gc.setStroke(Color.LIGHTGRAY);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) gc.strokeRect(x * cellSize, y * cellSize, cellSize, cellSize);
        }

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                Color cell = board[y][x];
                if (cell != null) {
                    double px = x * cellSize, py = y * cellSize;
                    gc.setFill(cell);
                    gc.fillRect(px, py, cellSize, cellSize);
                    gc.setStroke(Color.BLACK);
                    gc.strokeRect(px, py, cellSize, cellSize);
                }
            }
        }

        gc.setFill(currentPiece.color);
        for (Vec c : currentPiece.worldCells()) {
            double px = c.x() * cellSize, py = c.y() * cellSize;
            gc.fillRect(px, py, cellSize, cellSize);
            gc.setStroke(Color.BLACK);
            gc.strokeRect(px, py, cellSize, cellSize);
        }

        //gameover/paused overlay
        if (paused || gameOver) {
            double w = W * cellSize, h = H * cellSize;
            gc.save();
            gc.setGlobalAlpha(0.45);
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, w, h);

            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(gameOver ? "Game Over" : "PAUSED", w / 2.0, h / 2.0);
            gc.restore();
        }
    }

    //clear last full line and add marks
    private void clearFullLines() {
        int H = height, W = width;
        int writeRow = H - 1;

        for (int y = H - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < W; x++) {
                if (board[y][x] == null) { full = false; break; }
            }
            if (!full) {
                //copy the none empty line to a copy of the array to replace the current array
                if (writeRow != y) System.arraycopy(board[y], 0, board[writeRow], 0, W);
                writeRow--;
            } else {
                score += 100;
            }
        }
        for (int y = writeRow; y >= 0; y--) for (int x = 0; x < W; x++) board[y][x] = null;
        if (scoreLabel != null) scoreLabel.setText("Score: " + score);
    }

    public static void main(String[] args) { launch(args); }
}
