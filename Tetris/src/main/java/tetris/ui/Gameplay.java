package tetris.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import tetris.config.ConfigService;
import tetris.config.TetrisConfig;
import tetris.model.Board;
import tetris.model.TetrominoType;
import tetris.model.Vec;
import tetris.model.piece.ActivePiece;
import tetris.model.rules.RotationStrategy;
import tetris.model.rules.SrsRotation;
import tetris.service.ScoreService;

import java.util.Random;

public class Gameplay extends Application {

    // Config
    private final TetrisConfig config = ConfigService.load();

    // constants
    private static final int cellSize = 20;
    private long lastDropTime = 0L;
    private long dropSpeed;
    private boolean paused = false;
    private boolean gameOver = false;
    private int score = 0;

    //the color options array
    private static final Color[] colourOptions = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW
    };

    private final Random rng = new Random();
    private Board board;                     // now created with config sizes
    private ActivePiece current;             // the active falling piece
    private Color currentColor;              // colour for the active piece
    private final RotationStrategy rotator = new SrsRotation();
    private Label scoreLabel;
    private AnimationTimer timer;

    @Override
    public void start(Stage stage) {
        // Create board with config size
        board = new Board(config.getFieldWidth(), config.getFieldHeight());

        // Drop speed depends on init level
        dropSpeed = 1_000_000_000L / Math.max(1, config.getGameLevel());

        // UI
        scoreLabel = new Label("Score: 0");
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox topBar = new HBox(scoreLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        Canvas boardCanvas = new Canvas(
                board.width() * cellSize,
                board.height() * cellSize
        );
        boardCanvas.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> handleBack(stage));

        HBox backBar = new HBox(backButton);
        backBar.setAlignment(Pos.CENTER);
        backBar.setPadding(new Insets(10));

        Label authorLabel = new Label("Version : v2.0.0");
        HBox authorBar = new HBox(authorLabel);
        authorBar.setAlignment(Pos.CENTER);
        authorBar.setPadding(new Insets(5));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(boardCanvas);
        root.setBottom(new VBox(backBar, authorBar));
        root.setStyle("-fx-background-color: #f9f9f9;");
        int sceneWidth  = board.width()  * cellSize + 40;  // +40 for padding/margins
        int sceneHeight = board.height() * cellSize + 120; // +120 for top/bottom bars
        Scene scene = new Scene(root, sceneWidth, sceneHeight);

        stage.setScene(scene);

        //controls
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

        //gameplay timer
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

    private void handleBack(Stage stage) {
        if (gameOver) {
            if (timer != null) timer.stop();
            try { new MainMenu().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        } else {
            boolean wasPaused = paused;
            paused = true;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(stage);
            alert.setTitle("Leave Game?");
            alert.setHeaderText("Exit to Main Menu");
            alert.setContentText("Your current game will be lost. Are you sure?");

            ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
            ButtonType no  = new ButtonType("No",  ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yes, no);

            alert.showAndWait().ifPresent(response -> {
                if (response == yes) {
                    if (timer != null) timer.stop();
                    try { new MainMenu().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
                } else {
                    if (!wasPaused) {
                        paused = false;
                        lastDropTime = 0;
                    }
                }
            });
        }
    }

    //start new game
    private void resetGameState() {
        board = new Board(config.getFieldWidth(), config.getFieldHeight());
        score = 0;
        paused = false;
        gameOver = false;
        lastDropTime = 0L;
        dropSpeed = 1_000_000_000L / Math.max(1, config.getGameLevel());
        if (scoreLabel != null) scoreLabel.setText("Score: 0");
    }

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
        int startCol = Math.max(0, (board.width() - shapeWidth) / 2 - minX);

        current = new ActivePiece(type, new Vec(startCol, 0));
        currentColor = colourOptions[rng.nextInt(colourOptions.length)];

        // game over check
        for (Vec c : current.worldCells()) {
            if (!board.inside(c.x(), c.y()) || board.occupied(c.x(), c.y())) {
                gameOver = true;
                return;
            }
        }
    }

    private boolean tryBoost() {
        current.moveBy(0, +1);
        if (board.canPlace(current)) return true;
        current.moveBy(0, -1);
        return false;
    }

    private void lockPiece() {
        board.lock(current, currentColor);
        int cleared = board.clearLines();
        score += ScoreService.pointsFor(cleared);
        if (scoreLabel != null) scoreLabel.setText("Score: " + score);
        spawnNewPiece();
    }

    private void tryMoveLeft()  { if (!paused) move(-1, 0); }
    private void tryMoveRight() { if (!paused) move( 1, 0); }
    private void tryRotate()    { if (!paused) rotator.tryRotateCW(current, board); }

    private void move(int dx, int dy) {
        current.moveBy(dx, dy);
        if (!board.canPlace(current)) current.moveBy(-dx, -dy);
    }

    private void boost(boolean pressed) {
        dropSpeed = pressed ? 100_000_000L : 1_000_000_000L / Math.max(1, config.getGameLevel());
    }

    private void pauseGame() {
        paused = !paused;
        if (!paused) lastDropTime = 0;
    }

    private void draw(GraphicsContext gc) {
        Color[][] grid = board.cells();
        int H = grid.length;
        int W = grid[0].length;

        gc.clearRect(0, 0, W * cellSize, H * cellSize);

        // border color
        gc.setStroke(Color.LIGHTGRAY);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++)
                gc.strokeRect(x * cellSize, y * cellSize, cellSize, cellSize);
        }

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                Color cell = grid[y][x];
                if (cell != null) {
                    double px = x * cellSize, py = y * cellSize;
                    gc.setFill(cell);
                    gc.fillRect(px, py, cellSize, cellSize);
                    gc.setStroke(Color.BLACK);
                    gc.strokeRect(px, py, cellSize, cellSize);
                }
            }
        }

        gc.setFill(currentColor);
        for (Vec c : current.worldCells()) {
            double px = c.x() * cellSize, py = c.y() * cellSize;
            gc.fillRect(px, py, cellSize, cellSize);
            gc.setStroke(Color.BLACK);
            gc.strokeRect(px, py, cellSize, cellSize);
        }

        // overlay
        if (paused || gameOver) {
            double w = W * cellSize, h = H * cellSize;
            gc.save();
            gc.setGlobalAlpha(0.45);
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, w, h);

            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);

            String title = gameOver ? "Game Over" : "PAUSED";
            String hint  = gameOver ? "Press Back to return" : "Press 'P' to resume";

            gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            gc.fillText(title, w / 2.0, h / 2.0 - 18);

            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
            gc.fillText(hint,  w / 2.0, h / 2.0 + 16);
            gc.restore();
        }
    }

    public static void main(String[] args) { launch(args); }
}
