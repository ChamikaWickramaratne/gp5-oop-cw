// src/main/java/tetris/controller/GameplayController.java
package tetris.controller;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import tetris.config.ConfigService;
import tetris.config.PlayerType;
import tetris.config.TetrisConfig;

import tetris.model.Board;
import tetris.model.Vec;
import tetris.model.TetrominoType;
import tetris.model.piece.ActivePiece;
import tetris.model.rules.RotationStrategy;
import tetris.model.rules.SrsRotation;

import tetris.model.service.HighScoreManager;
import tetris.model.service.ScoreObserver;
import tetris.model.service.ScoreService;
import tetris.model.service.Score;

import tetris.view.SinglePlayerView;
import tetris.view.HighScore;
import tetris.view.MainMenu;

// Brains & networking (as per your packages)

// State pattern

import java.net.URL;
import java.util.Random;

public class GameplayController {

    // ======== Config ========
    private final TetrisConfig config = TetrisConfig.getInstance();

    // ======== Visual constants (from view) ========
    private static final int cellSize = SinglePlayerView.CELL_SIZE;

    // ======== Game ticking ========
    private long lastDropTime = 0L;
    private long dropSpeed;
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

    // UI (via view)
    private SinglePlayerView view;
    private AnimationTimer timer;
    private TetrominoType nextType;
    private Color nextColor;
    private int linesCleared = 0;

    // ======== External brain (network) ========
    private boolean useExternal = false;
    private INetwork net;
    private Player extPlayer;
    private boolean extControlsThisPiece = false;

    // ======== AI brain ========
    private boolean useAI = false;
    private AIPlayer aiPlayer;
    private boolean aiAnimating = false;

    private enum AiPhase { ROTATE, SHIFT }
    private AiPhase aiPhase;
    private int aiTargetX = 0;          // LEFTMOST column target
    private int aiRotLeft = 0;
    private int aiRotateAttempts = 0;
    private int aiRotateMax = 12;

    // ======== External micro-steps ========
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

    // ======== Score observer (for Observer pattern) ========
    private final ScoreObserver scoreObserver = newScore ->
            Platform.runLater(() -> { if (view != null) view.setScore(newScore); });


    // Stage
    private Stage stage;

    // ======== State pattern ========
    private GameState state;

    private void setState(GameState next) {
        if (state != null) state.onExit();
        state = next;
        state.onEnter();
    }
    private boolean humanInputEnabled() {
        return state != null && state.allowsHumanInput()
                && !useAI && !useExternal && !extControlsThisPiece;
    }

    // ======== Public wiring ========
    public void start(Stage stage) {
        this.stage = stage;

        // Create board with config size
        board = new Board(config.getFieldWidth(), config.getFieldHeight());
        dropSpeed = baseDropSpeed();

        // View
        view = new SinglePlayerView(board.width(), board.height());
        view.setPlayerTypeText(currentPlayerType());
        view.setLevel(config.getGameLevel());
        view.setLines(0);
        ScoreService.addObserver(scoreObserver);
        int sceneWidth  = board.width()  * cellSize + 40;
        int sceneHeight = board.height() * cellSize + 120;
        view.attachTo(stage, "Tetris", sceneWidth, sceneHeight);

        // Let PlayerFactory decide AI/External based on config
        PlayerFactory.configureForType(this, config.getPlayer1Type(), "localhost", 3000);

        // Controls
        stage.getScene().setOnKeyPressed(e -> {
            switch (e.getCode()) {
                // Human-only movement
                case A -> { if (humanInputEnabled()) { tryMoveLeft();  playSound("/sounds/move-turn.wav"); } }
                case D -> { if (humanInputEnabled()) { tryMoveRight(); playSound("/sounds/move-turn.wav"); } }
                case W, UP -> { if (humanInputEnabled()) { tryRotate(); playSound("/sounds/move-turn.wav"); } }

                // Boost: human can toggle; AI ignores (always boosted below)
                case X -> { if (state != null && state.allowsHumanInput()) boost(true); }

                // Global controls
                case P -> togglePause();
                case M -> toggleMusic();
                case S -> toggleSound();
            }
        });

        stage.getScene().setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.X && state != null && state.allowsHumanInput()) {
                boost(false);
            }
        });

        // Back button
        view.getBackButton().setOnAction(e -> {
            if (state instanceof GameOverState) {
                stopTimer();
                if (musicPlayer != null) musicPlayer.stop();
                try { new MainMenu().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
            } else {
                GameState before = state;
                setState(new PausedState(this));

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
                        stopTimer();
                        if (musicPlayer != null) musicPlayer.stop();
                        try { new MainMenu().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
                    } else {
                        if (!(before instanceof GameOverState)) setState(new RunningState(this));
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

        // Beep SFX
        URL soundUrl = getClass().getResource("/sounds/erase-line.wav");
        if (soundUrl != null) {
            Media beep = new Media(soundUrl.toExternalForm());
            beepPlayer = new MediaPlayer(beep);
            beepPlayer.setOnEndOfMedia(() -> beepPlayer.stop());
        }

        stage.show();
        resetGameState();
        spawnNewPiece();

        // Start in Running state
        setState(new RunningState(this));

        // Tick loop — delegates to current state
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (state != null) state.onTick(now);
                draw(view.getBoardGC());
            }
        };
        timer.start();
    }

    // ======== Public hooks kept from original ========
    public void enableAI(tetris.model.ai.Heuristic h) {
        useAI = true;
        aiPlayer = new AIPlayer(h);
    }

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

    public void setSeed(long seed) { rng.setSeed(seed); }

    // ======== Helpers used by states/tick ========
    long getLastDropTime() { return lastDropTime; }
    void setLastDropTime(long v) { lastDropTime = v; }
    long getDropSpeedNanos() { return dropSpeed; }
    void resetDropTimer() { lastDropTime = 0; }
    void stopTimer() {
        ScoreService.removeObserver(scoreObserver);
        if (timer != null) timer.stop();
    }

    void stepBrainsOnce() {
        // External reconnect opportunistically
        if (useExternal && (net == null || !net.isConnected()) && !extControlsThisPiece && !extAnimating) {
            tryReconnectAndRequestExternal();
        }
        // 1) External micro-step
        if (extAnimating) {
            doOneExternalStep();
        } else if (aiAnimating) {
            // 2) AI micro-step (only if external isn't animating)
            doOneAiStep();
        }
    }

    boolean tryGravity() { return tryBoost(); }

    void lockPieceAndSpawn() {
        lockPiece();
        extAnimating = false;
        aiAnimating  = false;
        extControlsThisPiece = false;
    }

    void pauseMusicIfEnabled() { if (musicPlayer != null && config.isMusic()) musicPlayer.pause(); }
    void resumeMusicIfEnabled(){ if (musicPlayer != null && config.isMusic()) musicPlayer.play(); }

    void tryReconnectIfNeeded() {
        if (useExternal && (net == null || !net.isConnected()) && !extControlsThisPiece && !extAnimating) {
            tryReconnectAndRequestExternal();
        }
    }

    // ======== Original helpers (all retained) ========
    private long baseDropSpeed() {
        return 1_000_000_000L / Math.max(1, config.getGameLevel());
    }

    public void applyAutoBoostIfNeeded() {
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
        if (config.getPlayer1Type() == PlayerType.EXTERNAL) return "External";
        if (config.getPlayer1Type() == PlayerType.AI) return "AI";
        return "Human";
    }

    private void showErrorAlert(String title, String header, String details) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(stage);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(details);
            alert.showAndWait();
        });
    }

    // ======== Bot micro-steps (present & unchanged semantics) ========
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

    // ======== Movement (unchanged API) ========
    private void tryMoveLeft()  { if (state.allowsHumanInput() && !extControlsThisPiece) move(-1, 0); }
    private void tryMoveRight() { if (state.allowsHumanInput() && !extControlsThisPiece) move(+1, 0); }
    private void tryRotate()    { if (state.allowsHumanInput() && !extControlsThisPiece) rotator.tryRotateCW(current, board); }

    private void move(int dx, int dy) {
        current.moveBy(dx, dy);
        if (!board.canPlace(current)) current.moveBy(-dx, -dy);
    }

    private void boost(boolean pressed) {
        if (useAI || useExternal) return;
        humanBoosting = pressed;
        applyAutoBoostIfNeeded();
    }

    // ======== Pause via state ========
    private void togglePause() {
        if (state instanceof PausedState) setState(new RunningState(this));
        else if (state instanceof RunningState) setState(new PausedState(this));
        // ignore in GameOverState
    }

    private void resetGameState() {
        board = new Board(config.getFieldWidth(), config.getFieldHeight());
        score = 0;
        linesCleared = 0;
        lastDropTime = 0L;
        dropSpeed = baseDropSpeed();
        view.setScore(0);
        view.setLines(0);

        // 🟢 Let observers (UI, etc.) know we’re back to zero
        ScoreService.notifyScoreChanged(0);

        nextType  = randomType();
        nextColor = randomColor();
        humanBoosting = false;

        Canvas boardCanvas = view.getBoardCanvas();
        boardCanvas.setWidth(board.width() * cellSize);
        boardCanvas.setHeight(board.height() * cellSize);
    }


    private void restartGame() {
        resetGameState();
        spawnNewPiece();
        setState(new RunningState(this));
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
                // Transition to GameOver
                setState(new GameOverState(this));
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
                            },
                            err -> {
                                extAnimating = false;
                                extPlayer = null;
                                net = null;
                            }
                    );
                } else {
                    extAnimating = false;
                    extPlayer = null;
                    net = null;
                }
            } catch (Exception e) {
                extAnimating = false;
                extPlayer = null;
                net = null;
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
                        },
                        err -> {
                            extAnimating = false;
                            extPlayer = null;
                            net = null;
                        }
                );
            }
        } catch (Exception e) {
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

        // 🟢 Notify all observers
        ScoreService.notifyScoreChanged(score);

        linesCleared += cleared;
        view.setLines(linesCleared);
        view.setScore(score); // harmless duplicate to keep existing behavior

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

        // Overlay via state
        if (state != null && (state.isPaused() || state.isGameOver())) {
            double w = W * cellSize, h = H * cellSize;
            gc.save();
            gc.setGlobalAlpha(0.45);
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, w, h);

            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);

            String title = state.isGameOver() ? "Game Over" : "PAUSED";
            String hint  = state.isGameOver() ? "Press Back to return" : "Press 'P' to resume";

            gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            gc.fillText(title, w / 2.0, h / 2.0 - 18);

            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
            gc.fillText(hint,  w / 2.0, h / 2.0 + 16);
            gc.restore();
        }
        drawNextPreview();
    }

    private void drawNextPreview() {
        Canvas nextCanvas = view.getNextCanvas();
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

        lockPieceAndSpawn();
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

    // === State-driven game over ===
    private void handleGameOver() {
        setState(new GameOverState(this));
    }

    // Called by GameOverState.onEnter()
    public void onGameOverDialog() {
        playSound("/sounds/game-finish.wav");

        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog("Player");
            dialog.initOwner(stage);
            dialog.setTitle("Game Over");
            dialog.setHeaderText("Your Score: " + score);
            dialog.setContentText("Enter your name:");

            dialog.showAndWait().ifPresent(rawName -> {
                String name = (rawName != null) ? rawName.trim() : "Player";
                if (name.isEmpty()) name = "Player";
                HighScoreManager manager = new HighScoreManager();
                String gameType = currentPlayerType();
                TetrisConfig cfg = TetrisConfig.getInstance();
                String mode = isMultiplayerGame() ? "Multiplayer" : "Single";
                Score s = new Score(
                        name,
                        score,
                        gameType,
                        cfg.getFieldWidth(),
                        cfg.getFieldHeight(),
                        cfg.getGameLevel(),
                        mode
                );
                manager.addScore(s);
            });

            try {
                new HighScore().start(stage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isMultiplayerGame() {
        return false;
    }
}
