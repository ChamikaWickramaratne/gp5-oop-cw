package tetris.ui;

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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import tetris.dto.OpMove;
import tetris.model.Board;
import tetris.model.TetrominoType;
import tetris.model.Vec;
import tetris.model.piece.ActivePiece;
import tetris.model.rules.RotationStrategy;
import tetris.model.rules.SrsRotation;
import tetris.service.ScoreService;

import java.util.Random;

public class Gameplay extends Application {

    //constants
    private static final int cellSize = 20;
    private final int width = 10;
    private final int height = 20;
    private long lastDropTime = 0L;
    private long dropSpeed = 1_000_000_000L;
    private boolean paused = false;
    private boolean gameOver = false;
    private int score = 0;

    //the color options array
    private static final Color[] colourOptions = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW
    };

    //collision checker used to check for collisions before rotating and locking blocks in
    interface CollisionChecker {
        boolean blocked(int row, int col);
        int rows();
        int cols();
    }

    private final Random rng = new Random();
    private Board board = new Board();
    private ActivePiece current;                 // the active falling piece
    private Color currentColor;                  // colour for the active piece
    private final RotationStrategy rotator = new SrsRotation();  // rotation rules
    private Label scoreLabel;
    private AnimationTimer timer;
    private TetrominoType nextType;
    private Color nextColor;
    private Canvas nextCanvas;
    private boolean useExternal = false;
    private tetris.net.INetwork net;
    private tetris.players.Player extPlayer;
    private boolean extControlsThisPiece = false;
    private boolean useAI = false;
    private tetris.players.AIPlayer aiPlayer;

    public void enableAI(tetris.ai.Heuristic h) {
        useAI = true;
        aiPlayer = new tetris.players.AIPlayer(h);
    }


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
            if (gameOver) {
                // If game is already over → no alert, just go to main menu
                if (timer != null) timer.stop();
                try {
                    new MainMenu().start(stage);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                // Pause while the dialog is open
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
                        try {
                            new MainMenu().start(stage);   // exit gameplay and return to main menu
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        // No → resume gameplay (only if it wasn't already paused before clicking Back)
                        if (!wasPaused) {
                            paused = false;
                            lastDropTime = 0;  // reset drop timer so it doesn't insta-drop
                        }
                    }
                });
            }
        });

        HBox backBar = new HBox(backButton);
        backBar.setAlignment(Pos.CENTER);
        backBar.setPadding(new Insets(10));

        nextCanvas = new Canvas(6 * cellSize, 6 * cellSize);
        VBox rightBar = new VBox(new Label("Next"), nextCanvas);
        rightBar.setAlignment(Pos.TOP_CENTER);
        rightBar.setSpacing(6);
        rightBar.setPadding(new Insets(10));

        Label authorLabel = new Label("Version : v2.0.0");
        HBox authorBar = new HBox(authorLabel);
        authorBar.setAlignment(Pos.CENTER);
        authorBar.setPadding(new Insets(5));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(boardCanvas);
        root.setRight(rightBar);
        root.setBottom(new VBox(backBar, authorBar));
        root.setStyle("-fx-background-color: #f9f9f9;");
        Scene scene = new Scene(root, UIConfigurations.WINDOW_WIDTH, UIConfigurations.WINDOW_HEIGHT);
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
        //stop boost when button released
        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.S) boost(false);
        });

        stage.setTitle("Tetris");
        stage.setMinWidth(500);
        stage.show();
        useExternal = false;
        String host = "localhost";
        int port = 3000;

        if (useExternal) {
            net = new tetris.net.ExternalPlayerClient(host, port);
            extPlayer = new tetris.players.ExternalPlayer(net);
            net.connect();
        }
        resetGameState();
        spawnNewPiece();

        //gameplay timer.
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

    //start new game
    private void resetGameState() {
        board = new Board();
        score = 0;
        paused = false;
        gameOver = false;
        lastDropTime = 0L;
        dropSpeed = 1_000_000_000L;
        if (scoreLabel != null) scoreLabel.setText("Score: 0");
        nextType  = randomType();
        nextColor = randomColor();
    }

    //restart method. not used yet. can assign it to a button if needed
    private void restartGame() {
        resetGameState();
        spawnNewPiece();
    }

    private void spawnNewPiece() {
        TetrominoType type = nextType;
        Color color = nextColor;
        nextType  = randomType();
        nextColor = randomColor();
        Vec[] base = type.offsets();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        for (Vec v : base) { if (v.x() < minX) minX = v.x(); if (v.x() > maxX) maxX = v.x(); }
        int shapeWidth = (maxX - minX + 1);
        int startCol = Math.max(0, (width - shapeWidth) / 2 - minX);

        current = new ActivePiece(type, new Vec(startCol, 0));
        currentColor = color;

        // Game-over check
        for (Vec c : current.worldCells()) {
            if (c.y() < 0 || c.y() >= height || c.x() < 0 || c.x() >= width || board.cells()[c.y()][c.x()] != null) {
                gameOver = true; return;
            }
        }
        if (useExternal && extPlayer != null && net != null && net.isConnected()) {
            extControlsThisPiece = true; // ignore human input while waiting
            var snap = snapshot();
            extPlayer.requestMoveAsync(snap,
                    mv  -> { extControlsThisPiece = false; applyExternalMove(mv); },
                    err -> { extControlsThisPiece = false; /* optional: show warning; fall back to human */ }
            );
        }
        if (useAI && aiPlayer != null) {
            extControlsThisPiece = true; // reuse the same “pane is busy” guard
            var snap = snapshot(); // you already have this
            aiPlayer.requestMoveAsync(
                    snap,
                    mv  -> { extControlsThisPiece = false; applyExternalMove(mv); },
                    err -> { extControlsThisPiece = false; /* fallback to human */ }
            );
        }
    }

    //boose piece fall speed
    private boolean tryBoost() {
        current.moveBy(0, +1);
        if (board.canPlace(current)) return true;
        current.moveBy(0, -1);
        return false;
    }

    //lock peice when the hit the bottom
    private void lockPiece() {
        board.lock(current, currentColor);
        int cleared = board.clearLines();
        score += ScoreService.pointsFor(cleared);
        if (scoreLabel != null) scoreLabel.setText("Score: " + score);
        spawnNewPiece();
    }

    //movement methods
    private void tryMoveLeft()  { if (!paused && !extControlsThisPiece) move(-1, 0); }
    private void tryMoveRight() { if (!paused && !extControlsThisPiece) move(+1, 0); }
    private void tryRotate()    { if (!paused && !extControlsThisPiece) rotator.tryRotateCW(current, board); }


    private void move(int dx, int dy) {
        current.moveBy(dx, dy);
        if (!board.canPlace(current)) current.moveBy(-dx, -dy);
    }

    private void boost(boolean pressed) {
        dropSpeed = pressed ? 100_000_000L : 1_000_000_000L;
    }

    private void pauseGame() {
        paused = !paused;
        if (!paused)
            lastDropTime = 0;
    }

    //drawing the board each time
    private void draw(GraphicsContext gc) {
        int W = width, H = height;
        gc.clearRect(0, 0, W * cellSize, H * cellSize);

        //border color
        gc.setStroke(Color.LIGHTGRAY);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++)
                gc.strokeRect(x * cellSize, y * cellSize, cellSize, cellSize);
        }

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                Color cell = board.cells()[y][x];
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

        // game over / paused overlay
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
            gc.fillText(title, w / 2.0, h / 2.0 - 18);    // slightly above center

            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
            gc.fillText(hint,  w / 2.0, h / 2.0 + 16);    // slightly below center
            gc.restore();
        }
        drawNextPreview();
    }

    private void drawNextPreview() {
        if (nextCanvas == null) return;
        GraphicsContext ng = nextCanvas.getGraphicsContext2D();
        ng.clearRect(0, 0, nextCanvas.getWidth(), nextCanvas.getHeight());

        Vec[] offs = nextType.offsets();

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Vec v : offs) {
            minX = Math.min(minX, v.x()); maxX = Math.max(maxX, v.x());
            minY = Math.min(minY, v.y()); maxY = Math.max(maxY, v.y());
        }
        int w = (maxX - minX + 1), h = (maxY - minY + 1);

        double boxW = nextCanvas.getWidth(), boxH = nextCanvas.getHeight();
        double startPx = (boxW - w * cellSize) / 2.0 - minX * cellSize;
        double startPy = (boxH - h * cellSize) / 2.0 - minY * cellSize;

        ng.setFill(nextColor);
        ng.setStroke(Color.BLACK);
        for (Vec v : offs) {
            double px = startPx + v.x() * cellSize;
            double py = startPy + v.y() * cellSize;
            ng.fillRect(px, py, cellSize, cellSize);
            ng.strokeRect(px, py, cellSize, cellSize);
        }
    }

    public void setSeed(long seed) { rng.setSeed(seed); }

    private TetrominoType randomType() {
        TetrominoType[] vals = TetrominoType.values();
        return vals[rng.nextInt(vals.length)];
    }
    private Color randomColor() {
        return colourOptions[rng.nextInt(colourOptions.length)];
    }

    private tetris.dto.PureGame snapshot() {
        tetris.dto.PureGame p = new tetris.dto.PureGame();
        p.width = width;
        p.height = height;

        // Board occupancy
        p.cells = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                p.cells[y][x] = (board.cells()[y][x] != null) ? 1 : 0;
            }
        }

        // Current piece → normalize worldCells()
        p.currentShape = toMatrixFromCells(current.worldCells());

        // Next piece → just its canonical offsets (rotation 0)
        p.nextShape = toMatrixFromCells(java.util.Arrays.asList(nextType.offsets()));

        return p;
    }

    private int[][] toMatrixFromCells(java.util.Collection<tetris.model.Vec> cells) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (tetris.model.Vec v : cells) {
            minX = Math.min(minX, v.x());
            minY = Math.min(minY, v.y());
            maxX = Math.max(maxX, v.x());
            maxY = Math.max(maxY, v.y());
        }

        int w = maxX - minX + 1;
        int h = maxY - minY + 1;
        int[][] m = new int[h][w];

        for (tetris.model.Vec v : cells) {
            m[v.y() - minY][v.x() - minX] = 1;
        }
        return m;
    }

    private void applyExternalMove(tetris.dto.OpMove mv) {
        // Rotate CW as many times as requested
        for (int i = 0; i < mv.opRotate; i++) {
            rotator.tryRotateCW(current, board);
        }

        // Align X
        while (current.origin().x() < mv.opX) move(+1, 0);
        while (current.origin().x() > mv.opX) move(-1, 0);

        // Drop
        while (tryBoost()) { /* fall until blocked */ }
        lockPiece();
    }

    public void enableExternal(String host, int port) {
        useExternal = true;
        net = new tetris.net.ExternalPlayerClient(host, port);
        extPlayer = new tetris.players.ExternalPlayer(net);
        net.connect();
    }

    public static void main(String[] args) { launch(args); }
}
