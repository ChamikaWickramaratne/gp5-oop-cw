// src/main/java/tetris/ui/Gameplay.java
package tetris.view;

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

import tetris.config.PlayerType;
import tetris.config.ConfigService;
import tetris.config.TetrisConfig;
import tetris.controller.*;
import tetris.model.Board;
import tetris.model.TetrominoType;
import tetris.model.Vec;
import tetris.model.piece.ActivePiece;
import tetris.model.rules.RotationStrategy;
import tetris.model.rules.SrsRotation;
import tetris.model.service.ScoreService;
import tetris.model.service.HighScoreManager;
import tetris.model.service.Score;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;
import java.util.Optional;
import java.util.Random;

public class Gameplay extends Application {

    // ======== Config ========
    private final TetrisConfig config = ConfigService.load();

    // ======== Visual constants ========
    private static final int cellSize = 20;

    // ======== Game ticking ========
    private long lastDropTime = 0L;
    private long dropSpeed;
    private boolean paused = false;
    private boolean gameOver = false;
    private int score = 0;

    // ======== Audio ========
    private MediaPlayer musicPlayer;
    private MediaPlayer beepPlayer;

    // ======== Colors ========
    private static final Color[] colourOptions = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW
    };

    // ======== Core model ========
    private final Random rng = new Random();
    private Board board;                     // created with config sizes
    private ActivePiece current;             // active falling piece
    private Color currentColor;              // colour for the active piece
    private final RotationStrategy rotator = new SrsRotation();

    // UI
    private Label scoreLabel;
    private AnimationTimer timer;
    private TetrominoType nextType;
    private Color nextColor;
    private Canvas boardCanvas;
    private Canvas nextCanvas;
    private Label playerTypeLabel;
    private Label levelLabel;
    private Label linesLabel;
    private int linesCleared = 0;

    // ======== External brain (network) ========
    private boolean useExternal = false;
    private INetwork net;
    private Player extPlayer;
    private boolean extControlsThisPiece = false;

    // ======== AI brain (like GamePane, non-blocking) ========
    private boolean useAI = false;
    private AIPlayer aiPlayer;
    private boolean aiAnimating = false;

    private enum AiPhase { ROTATE, SHIFT, DROP }
    private AiPhase aiPhase;
    private int aiTargetX = 0;          // LEFTMOST column target
    private int aiRotLeft = 0;
    private int aiRotateAttempts = 0;
    private int aiRotateMax = 12;

    // ======== External micro-steps (real-time like AI) ========
    private boolean extAnimating = false;

    private enum ExtPhase { ROTATE, SHIFT }
    private ExtPhase extPhase;

    private int extTargetX = 0;          // LEFTMOST column target (same semantics as AI)
    private int extRotLeft = 0;
    private int extRotateAttempts = 0;
    private int extRotateMax = 12;
    // Gravity helpers
    private static final long BOOST_NANOS = 100_000_000L;
    private boolean humanBoosting = false;

    private long baseDropSpeed() {
        return 1_000_000_000L / Math.max(1, config.getGameLevel());
    }

    private void applyAutoBoostIfNeeded() {
        boolean botControlling =
                (useAI && (aiAnimating || extControlsThisPiece)) ||
                        (useExternal && extControlsThisPiece);

        if (botControlling) {
            dropSpeed = BOOST_NANOS;
        } else {
            dropSpeed = humanBoosting ? BOOST_NANOS : baseDropSpeed();
        }
    }


    private String currentPlayerType() {
        // Reflects the *actual* control for Player 1 this run
        if (useExternal) return "External";
        if (useAI) return "AI";
        return "Human";
    }

    public void enableAI(tetris.model.ai.Heuristic h) {
        useAI = true;
        aiPlayer = new AIPlayer(h);
    }

    // in Gameplay (add next to enableAI)
    public void enableExternal(String host, int port) {
        useExternal = true;
        useAI = false;

        net = new ExternalPlayerClient(host, port);
        extPlayer = new ExternalPlayer(net);
        try {
            net.connect();
            if (!net.isConnected()) throw new IllegalStateException("Not connected");
            applyAutoBoostIfNeeded();
        } catch (Exception ex) {
            extPlayer = null;
            net = null;
            showErrorAlert(
                    "External Player Unavailable",
                    "Could not connect to the external player at " + host + ":" + port + ".",
                    ex.getClass().getSimpleName() + (ex.getMessage()!=null?(": "+ex.getMessage()):"")
            );
        }
    }


    private Stage mainStage;

    private void showErrorAlert(String title, String header, String details) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(mainStage);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(details);
            alert.showAndWait();
        });
    }

    private boolean humanInputEnabled() {
        return !useAI && !useExternal && !extControlsThisPiece && !paused && !gameOver;
    }

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;

        // Create board with config size
        board = new Board(config.getFieldWidth(), config.getFieldHeight());

        dropSpeed = baseDropSpeed();

        PlayerFactory.configureForType(this, config.getPlayer1Type(), "localhost", 3000);

        if (config.isAiPlay() && config.getPlayer1Type() == PlayerType.HUMAN) {
            enableAI(new tetris.model.ai.BetterHeuristic());
            applyAutoBoostIfNeeded();
        }

        scoreLabel = new Label("Score: 0");
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox topBar = new HBox(scoreLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        boardCanvas = new Canvas(board.width() * cellSize, board.height() * cellSize);
        boardCanvas.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            if (gameOver) {
                if (timer != null) timer.stop();
                if (musicPlayer != null) musicPlayer.stop();
                try {
                    new MainMenu().start(stage);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
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
                        if (musicPlayer != null) musicPlayer.stop();
                        try {
                            new MainMenu().start(stage);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        if (!wasPaused) {
                            paused = false;
                            lastDropTime = 0;
                        }
                    }
                });
            }
        });

        // Background Music
        if (config.isMusic()) {
            URL musicUrl = getClass().getResource("/sounds/background.mp3");
            if (musicUrl != null) {
                Media backgroundMusic = new Media(musicUrl.toExternalForm());
                musicPlayer = new MediaPlayer(backgroundMusic);
                musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                musicPlayer.setOnReady(() -> musicPlayer.play());
            }
        }

        HBox backBar = new HBox(backButton);
        backBar.setAlignment(Pos.CENTER);
        backBar.setPadding(new Insets(10));

        // --- Sidebar: create controls FIRST ---
        nextCanvas = new Canvas(6 * cellSize, 6 * cellSize);
        playerTypeLabel = new Label("Player: " + currentPlayerType());
        playerTypeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        levelLabel = new Label("Level: " + config.getGameLevel());
        levelLabel.setStyle("-fx-font-size: 13px;");

        linesLabel = new Label("Lines: 0");
        linesLabel.setStyle("-fx-font-size: 13px;");

// Wrap the info labels in a styled VBox card
        VBox infoBox = new VBox(6,
                new Label("Info"),
                playerTypeLabel,
                levelLabel,
                linesLabel
        );
        infoBox.setAlignment(Pos.TOP_CENTER);
        infoBox.setPadding(new Insets(10));
        infoBox.setSpacing(6);
        infoBox.setStyle("""
    -fx-background-color: #f4f4f4;
    -fx-border-color: #888;
    -fx-border-radius: 8;
    -fx-background-radius: 8;
    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);
""");

        VBox rightBar = new VBox(
                new Label("Next"),
                nextCanvas,
                new Separator(),
                infoBox
        );
        rightBar.setAlignment(Pos.TOP_CENTER);
        rightBar.setSpacing(12);
        rightBar.setPadding(new Insets(10));
        rightBar.setStyle("-fx-background-color: #fafafa;");



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
        int sceneWidth  = board.width()  * cellSize + 40;
        int sceneHeight = board.height() * cellSize + 120;
        Scene scene = new Scene(root, sceneWidth, sceneHeight);
        stage.setScene(scene);

        // Controls
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                // Human-only movement
                case A -> { if (humanInputEnabled()) { tryMoveLeft();  playSound("/sounds/move-turn.wav"); } }
                case D -> { if (humanInputEnabled()) { tryMoveRight(); playSound("/sounds/move-turn.wav"); } }
                case W, UP -> { if (humanInputEnabled()) { tryRotate(); playSound("/sounds/move-turn.wav"); } }

                // Boost: human can toggle; AI ignores (always boosted below)
                case X -> { if (humanInputEnabled()) boost(true); }

                // Global controls allowed anytime
                case P -> pauseGame();
                case M -> toggleMusic();
                case S -> toggleSound();
            }
        });

        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.X && humanInputEnabled()) {
                boost(false);
            }
        });


        stage.setTitle("Tetris");
        stage.setMinWidth(500);
        stage.show();
        resetGameState();
        spawnNewPiece();

        // Tick loop — now mirrors GamePane: AI micro-steps then gravity
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                applyAutoBoostIfNeeded();
                if (!paused && !gameOver) {
                    if (lastDropTime == 0) lastDropTime = now;
                    else if (now - lastDropTime > dropSpeed) {
                        if (useExternal && (net == null || !net.isConnected()) && !extControlsThisPiece && !extAnimating) {
                            tryReconnectAndRequestExternal();
                        }
                        // 1) AI micro-step (never blocks gravity)
                        if (extAnimating) {
                            doOneExternalStep();
                        } else if (aiAnimating) {
                            // 2) AI micro-step (only if external isn't animating)
                            doOneAiStep();
                        }

                        // 2) Gravity: drop exactly one row per tick
                        boolean fell = tryBoost();

                        // 3) If we couldn’t drop → lock the piece
                        if (!fell) {
                            lockPiece();
                            extAnimating = false;
                            aiAnimating  = false;
                            extControlsThisPiece = false;
                        }

                        lastDropTime = now;
                    }
                }
                draw(boardCanvas.getGraphicsContext2D());
            }
        };
        timer.start();

        URL soundUrl = getClass().getResource("/sounds/erase-line.wav");
        if (soundUrl != null) {
            Media beep = new Media(soundUrl.toExternalForm());
            beepPlayer = new MediaPlayer(beep);
            beepPlayer.setOnEndOfMedia(() -> beepPlayer.stop());
        }
    }

    private void doOneExternalStep() {
        switch (extPhase) {
            case ROTATE -> {
                if (extRotLeft > 0) {
                    boolean rotated = tryRotateWithKicks(1);
                    if (rotated) {
                        extRotLeft--;
                        extRotateAttempts = 0;
                    } else {
                        int left   = currentLeft();
                        int target = clampTargetLeft(extTargetX);
                        if (left != target) {
                            int dir = (target > left) ? +1 : -1;
                            move(dir, 0); // safe; reverts if blocked
                        }
                        if (++extRotateAttempts >= extRotateMax) {
                            // give up rotating; proceed to SHIFT
                            extRotLeft = 0;
                        }
                    }

                    if (extRotLeft == 0) {
                        extPhase = ExtPhase.SHIFT;
                        extRotateAttempts = 0;
                    }
                    return; // stay in ROTATE for this tick
                }

                // nothing left to rotate → go to SHIFT
                extPhase = ExtPhase.SHIFT;
            }

            case SHIFT -> {
                int left   = currentLeft();
                int target = clampTargetLeft(extTargetX);

                if (left == target) {
                    // finished external plan; let gravity continue
                    extAnimating = false;
                    return;
                }
                int dir = (target > left) ? +1 : -1;
                int before = left;
                move(dir, 0);

                int afterLeft = currentLeft();
                if (afterLeft == before) {
                    // blocked horizontally; stop trying — gravity will drop and we’ll lock later
                    extAnimating = false;
                }
            }
        }
    }


    // ======== AI micro-steps (same behavior as GamePane) ========
    private void doOneAiStep() {
        switch (aiPhase) {
            case ROTATE -> {
                if (aiRotLeft > 0) {
                    boolean rotated = tryRotateWithKicks(1);
                    if (rotated) {
                        aiRotLeft--;
                        aiRotateAttempts = 0;
                    } else {
                        int left = currentLeft();
                        int target = clampTargetLeft(aiTargetX);
                        if (left != target) {
                            int dir = (target > left) ? +1 : -1;
                            move(dir, 0);
                        }
                        if (++aiRotateAttempts >= aiRotateMax) {
                            aiRotLeft = 0;
                        }
                    }

                    if (aiRotLeft == 0) {
                        aiPhase = AiPhase.SHIFT;
                        aiRotateAttempts = 0;
                    }
                    return;
                }
                aiPhase = AiPhase.SHIFT;
            }

            case SHIFT -> {
                int left = currentLeft();
                int target = clampTargetLeft(aiTargetX);
                if (left == target) {
                    aiAnimating = false;
                    return;
                }
                int dir = (target > left) ? +1 : -1;
                int before = left;
                move(dir, 0);

                int afterLeft = currentLeft();
                if (afterLeft == before) {
                    aiAnimating = false;
                }
            }
        }
    }

    private void tryMoveLeft()  { if (!paused && !extControlsThisPiece) move(-1, 0); }
    private void tryMoveRight() { if (!paused && !extControlsThisPiece) move(+1, 0); }
    private void tryRotate()    { if (!paused && !extControlsThisPiece) rotator.tryRotateCW(current, board); }

    private void move(int dx, int dy) {
        current.moveBy(dx, dy);
        if (!board.canPlace(current)) current.moveBy(-dx, -dy);
    }

    private void boost(boolean pressed) {
        if (useAI || useExternal) return;

        humanBoosting = pressed;
        applyAutoBoostIfNeeded();
    }


    private void pauseGame() {
        paused = !paused;
        if (paused) {
            if (musicPlayer != null && config.isMusic()) musicPlayer.pause();
        } else {
            if (musicPlayer != null && config.isMusic()) musicPlayer.play();
            lastDropTime = 0;
        }
    }

    private void resetGameState() {
        board = new Board(config.getFieldWidth(), config.getFieldHeight());
        score = 0;
        paused = false;
        gameOver = false;
        lastDropTime = 0L;
        dropSpeed = 1_000_000_000L / Math.max(1, config.getGameLevel());
        if (scoreLabel != null) scoreLabel.setText("Score: 0");
        nextType  = randomType();
        nextColor = randomColor();
        humanBoosting = false;

        if (boardCanvas != null) {
            boardCanvas.setWidth(board.width() * cellSize);
            boardCanvas.setHeight(board.height() * cellSize);
        }
    }

    private void restartGame() {
        resetGameState();
        spawnNewPiece();
    }

    private void spawnNewPiece() {
        TetrominoType type = nextType;
        Color color = nextColor;
        nextType  = randomType();
        nextColor = randomColor();

        extAnimating = false;
        aiAnimating = false;
        extRotateAttempts = 0;
        aiRotateAttempts = 0;
        boolean requested = false;

        Vec[] base = type.offsets();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        for (Vec v : base) {
            if (v.x() < minX) minX = v.x();
            if (v.x() > maxX) maxX = v.x();
        }
        int shapeWidth = (maxX - minX + 1);
        int startCol = Math.max(0, (board.width() - shapeWidth) / 2 - minX);

        current = new ActivePiece(type, new Vec(startCol, 0));
        currentColor = color;

        for (Vec c : current.worldCells()) {
            if (c.y() < 0 || c.y() >= board.height() || c.x() < 0 || c.x() >= board.width()
                    || board.cells()[c.y()][c.x()] != null) {
                gameOver = true;
                if (musicPlayer != null) musicPlayer.stop();
                handleGameOver();
                return;
            }
        }

        if (useExternal) {
            try {
                if (net != null) {
                    try { net.disconnect(); } catch (Exception ignored) {}
                }

                String host = "localhost";
                int port = 3000;

                net = new ExternalPlayerClient(host, port);
                net.connect();
                extPlayer = new ExternalPlayer(net);

                if (net.isConnected()) {
                    requested = true;
                    extControlsThisPiece = true;
                    var snap = snapshot();

                    extPlayer.requestMoveAsync(
                            snap,
                            mv -> {
                                extRotLeft = (mv.opRotate & 3);
                                extTargetX = mv.opX;
                                extPhase   = ExtPhase.ROTATE;
                                extAnimating = true;
                                applyAutoBoostIfNeeded();
                                extControlsThisPiece = true;
                                lastDropTime = 0L;

                                System.out.println("[EXT] plan: rotate=" + extRotLeft + " targetLeft=" + extTargetX);
                            },
                            err -> {
                                System.err.println("[EXT] request failed: " + err.getMessage());
                                extAnimating = false;
                                extPlayer = null;
                                net = null;
                            }
                    );
                } else {
                    System.err.println("[EXT] Failed to reconnect to external server.");
                    extAnimating = false;
                    extPlayer = null;
                    net = null;
                }
            } catch (Exception e) {
                extAnimating = false;
                extPlayer = null;
                net = null;
                System.err.println("[EXT] Connection error: " + e.getMessage());
                showErrorAlert(
                        "External Player Unavailable",
                        "Failed to reconnect for new piece. External control disabled for this game.",
                        e.getClass().getSimpleName() + (e.getMessage() != null ? (": " + e.getMessage()) : "")
                );
            }
        }

        if (useAI && aiPlayer != null && !requested) {
            requested = true;
            extControlsThisPiece = true;
            var snap = snapshot();
            aiPlayer.requestMoveAsync(
                    snap,
                    mv -> {
                        int r = mv.opRotate & 3;
                        aiRotLeft = r;
                        aiTargetX = mv.opX;
                        aiPhase = AiPhase.ROTATE;
                        aiAnimating = true;
                        extControlsThisPiece = true;
                        lastDropTime = 0L;
                        System.out.println("[AI] plan: rotate=" + aiRotLeft + " targetLeft=" + aiTargetX);
                    },
                    err -> { extControlsThisPiece = false; }
            );
        }
    }

    private void tryReconnectAndRequestExternal() {
        if (!useExternal || net != null && net.isConnected()) return;

        try {
            String host = "localhost";
            int port = 3000;
            net = new ExternalPlayerClient(host, port);
            net.connect();
            if (net.isConnected()) {
                extPlayer = new ExternalPlayer(net);
                System.out.println("[EXT] Reconnected mid-piece. Requesting move...");

                extPlayer.requestMoveAsync(
                        snapshot(),
                        mv -> {
                            extRotLeft = (mv.opRotate & 3);
                            extTargetX = mv.opX;
                            extPhase   = ExtPhase.ROTATE;
                            extAnimating = true;
                            extControlsThisPiece = true;
                            applyAutoBoostIfNeeded();
                            lastDropTime = 0L;

                            System.out.println("[EXT] mid-piece plan: rotate=" + extRotLeft + " targetLeft=" + extTargetX);
                        },
                        err -> {
                            System.err.println("[EXT] mid-piece request failed: " + err.getMessage());
                            extAnimating = false;
                            extPlayer = null;
                            net = null;
                        }
                );
            }
        } catch (Exception e) {
            System.err.println("[EXT] mid-piece reconnect error: " + e.getMessage());
            extPlayer = null;
            net = null;
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
        linesCleared += cleared;
        if (linesLabel != null) linesLabel.setText("Lines: " + linesCleared);
        if (scoreLabel != null) scoreLabel.setText("Score: " + score);

        if (cleared > 0 && config.isSoundEffect() && beepPlayer != null) {
            beepPlayer.stop();
            beepPlayer.play();
        }

        spawnNewPiece();
    }

    private void draw(GraphicsContext gc) {
        Color[][] grid = board.cells();
        int H = grid.length;
        int W = grid[0].length;

        gc.clearRect(0, 0, W * cellSize, H * cellSize);

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

    private tetris.model.dto.PureGame snapshot() {
        tetris.model.dto.PureGame p = new tetris.model.dto.PureGame();
        p.width = board.width();
        p.height = board.height();

        p.cells = new int[p.height][p.width];
        for (int y = 0; y < p.height; y++) {
            for (int x = 0; x < p.width; x++) {
                p.cells[y][x] = (board.cells()[y][x] != null) ? 1 : 0;
            }
        }

        p.currentShape = toMatrixFromCells(current.worldCells());
        p.nextShape    = toMatrixFromCells(java.util.Arrays.asList(nextType.offsets()));
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

    private void applyExternalMove(tetris.model.dto.OpMove mv) {
        final int r = (mv.opRotate & 3);

        boolean rotated = tryRotateWithKicks(r);

        int target = clampTargetLeft(mv.opX);
        int guard = 0;
        while (currentLeft() != target && guard++ < (board.width() * 2)) {
            int left = currentLeft();
            int dir = (target > left) ? +1 : -1;
            int before = left;
            move(dir, 0);
            if (currentLeft() == before) break;
            if (!rotated) rotated = tryRotateWithKicks(r);
        }

        while (tryBoost()) { /* fall until blocked */ }

        lockPiece();
    }

    /** Try CW rotate 'r' times; on failure, attempt tiny horizontal nudges then retry. */
    private boolean tryRotateWithKicks(int r) {
        if (r == 0) return true;

        int applied = 0;
        for (int i = 0; i < r; i++) {
            if (rotator.tryRotateCW(current, board)) { applied++; }
            else break;
        }
        if (applied == r) return true;

        int remaining = r - applied;
        int[] kicks = {+1, -1, +2, -2};
        for (int k : kicks) {
            if (!board.tryNudge(current, k, 0)) continue;
            int ok = 0;
            for (int i = 0; i < remaining; i++) {
                if (rotator.tryRotateCW(current, board)) ok++;
                else break;
            }
            if (ok == remaining) return true;
            board.tryNudge(current, -k, 0);
        }
        return false;
    }

    private int currentLeft() {
        int min = Integer.MAX_VALUE;
        for (Vec v : current.worldCells()) if (v.x() < min) min = v.x();
        return min;
    }
    private int currentRight() {
        int max = Integer.MIN_VALUE;
        for (Vec v : current.worldCells()) if (v.x() > max) max = v.x();
        return max;
    }
    private int clampTargetLeft(int desiredLeft) {
        int pieceWidth = currentRight() - currentLeft() + 1;
        int min = 0;
        int max = Math.max(0, board.width() - pieceWidth);
        if (desiredLeft < min) return min;
        if (desiredLeft > max) return max;
        return desiredLeft;
    }

    private void toggleMusic() {
        boolean newVal = !config.isMusic();
        config.setMusic(newVal);
        ConfigService.save(config);
        if (newVal) {
            initMusicPlayerIfNeeded();
            if (musicPlayer != null) {
                musicPlayer.setOnReady(() -> musicPlayer.play());
                try { musicPlayer.play(); } catch (Exception ignore) { }
            }
        } else {
            if (musicPlayer != null) musicPlayer.pause();
        }
    }

    private void toggleSound() {
        boolean newVal = !config.isSoundEffect();
        config.setSoundEffect(newVal);
        ConfigService.save(config);
    }

    private void initMusicPlayerIfNeeded() {
        if (musicPlayer != null) return;
        URL musicUrl = getClass().getResource("/sounds/background.mp3");
        if (musicUrl == null) {
            System.err.println("background.mp3 not found under /sounds");
            return;
        }
        Media bg = new Media(musicUrl.toExternalForm());
        musicPlayer = new MediaPlayer(bg);
        musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
    }

    private void playSound(String resource) {
        if (config.isSoundEffect()) {
            URL soundUrl = getClass().getResource(resource);
            if (soundUrl != null) {
                Media media = new Media(soundUrl.toExternalForm());
                MediaPlayer player = new MediaPlayer(media);
                player.setOnEndOfMedia(player::dispose);
                player.play();
            }
        }
    }

    private void handleGameOver() {
        if (timer != null) timer.stop();
        gameOver = true;

        playSound("/sounds/game-finish.wav");
        javafx.application.Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog("Player");
            dialog.initOwner(mainStage);
            dialog.setTitle("Game Over");
            dialog.setHeaderText("Your Score: " + score);
            dialog.setContentText("Enter your name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                HighScoreManager manager = new HighScoreManager();
                String type = currentPlayerType(); // "Human", "AI", or "External"
                manager.addScore(new Score(name, score, type));
            });

            try {
                new HighScore().start(mainStage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) { launch(args); }
}
