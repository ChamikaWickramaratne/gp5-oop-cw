// src/main/java/tetris/ui/GamePane.java
package tetris.view;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.Optional;

import tetris.config.ConfigService;
import tetris.config.TetrisConfig;
import tetris.controller.*;
import tetris.model.Board;
import tetris.model.TetrominoType;
import tetris.model.Vec;
import tetris.model.piece.ActivePiece;
import tetris.model.rules.RotationStrategy;
import tetris.model.rules.SrsRotation;
import tetris.model.service.HighScoreManager;
import tetris.model.service.Score;
import tetris.model.service.ScoreService;

public class GamePane extends BorderPane {
    private static final int cellSize = 20;

    private long lastDropTime = 0L;
    private long dropSpeed   = 1_000_000_000L;
    private boolean paused   = false;
    private boolean gameOver = false;
    private int score = 0;

    private static final Color[] colourOptions = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW
    };

    private final java.util.Random rng = new java.util.Random();

    // Config & Audio
    private final TetrisConfig config = ConfigService.load();
    private MediaPlayer musicPlayer;
    private MediaPlayer beepPlayer;

    // Instance-owned board (no static/global sharing!)
    private Board board = new Board();

    private ActivePiece current;
    private Color currentColor;
    private final RotationStrategy rotator = new SrsRotation();
    private AnimationTimer timer;

    private TetrominoType nextType;
    private Color nextColor;

    private Canvas boardCanvas;
    private Canvas nextCanvas;
    private Label  scoreLabel;

    // ===== external player integration (optional per pane) =====
    private boolean useExternal = false;
    private INetwork net;
    private Player extPlayer;
    private boolean extControlsThisPiece = false;

    // ===== AI integration (optional per pane) =====
    private boolean useAI = false;
    private AIPlayer aiPlayer;
    private boolean aiAnimating = false;

    private enum AiPhase { ROTATE, SHIFT, DROP }
    private AiPhase aiPhase;
    private int aiTargetX = 0;   // interpreted as LEFTMOST column target
    private int aiRotLeft = 0;
    private int aiRotateAttempts = 0;
    private int aiRotateMax = 12;
    private static final long BOOST_NANOS = 100_000_000L;
    // add near other flags
    private boolean extLateJoinAsked = false;
    private Label playerTypeLabel;
    private Label levelLabel;
    private Label linesLabel;
    private int linesCleared = 0;
    private boolean humanBoosting = false;

    private long baseDropSpeed() {
        return 1_000_000_000L / Math.max(1, config.getGameLevel());
    }

    private void applyAutoBoostIfNeeded() {
        boolean botControlling =
                (useAI && (aiAnimating || extControlsThisPiece)) ||
                        (useExternal && extControlsThisPiece);

        if (botControlling) {
            dropSpeed = BOOST_NANOS;                 // bot control = always boosted
        } else {
            dropSpeed = humanBoosting ? BOOST_NANOS  // human holding boost
                    : baseDropSpeed();
        }
    }


    public void enableAI(tetris.model.ai.Heuristic h) {
        useAI = true;
        aiPlayer = new AIPlayer(h);
        applyAutoBoostIfNeeded();
        if (playerTypeLabel != null) playerTypeLabel.setText("Player: " + currentPlayerType());
    }

    private String currentPlayerType() {
        // Reflects the *actual* control for Player 1 this run
        if (useExternal) return "External";
        if (useAI) return "AI";
        return "Human";
    }

    public void startGame() {
        resetGameState();
        spawnNewPiece();
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                applyAutoBoostIfNeeded(); // ✅ keeps gravity boosted if AI/External controls
                if (!paused && !gameOver) {
                    if (useExternal
                            && net != null && net.isConnected()
                            && current != null && !gameOver
                            && !extControlsThisPiece
                            && !aiAnimating
                            && !extLateJoinAsked) {
                        requestExternalForCurrent();
                    }
                    if (lastDropTime == 0) lastDropTime = now;
                    else if (now - lastDropTime > dropSpeed) {
                        if (aiAnimating) {
                            doOneAiStep();
                        }

                        boolean fell = tryBoost();

                        if (!fell) {
                            lockPiece();
                            aiAnimating = false;
                            extControlsThisPiece = false;
                        }

                        lastDropTime = now;
                    }
                }
                draw(boardCanvas.getGraphicsContext2D());
            }
        };
        timer.start();
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
                        // shimmy one cell toward target to help wall kicks
                        int left = currentLeft();
                        int target = clampTargetLeft(aiTargetX);
                        if (left != target) {
                            int dir = (target > left) ? +1 : -1;
                            move(dir, 0); // safe; reverts if blocked
                        }
                        if (++aiRotateAttempts >= aiRotateMax) {
                            aiRotLeft = 0; // give up rotating
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

                // ✅ If already aligned, stop shifting and let gravity do the rest
                if (left == target) {
                    aiPhase = AiPhase.DROP; // move to final drop phase
                    return;
                }

                // ✅ Only attempt one step per tick toward the target
                int dir = (target > left) ? +1 : -1;
                int before = left;
                move(dir, 0);

                int afterLeft = currentLeft();
                if (afterLeft == before) {
                    // blocked horizontally → stop trying
                    aiPhase = AiPhase.DROP;
                }
            }

            case DROP -> {
                // ✅ Do nothing — gravity (tryBoost) handles this phase
            }
        }
    }

    public GamePane() {
        // UI
        scoreLabel = new Label();
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox topBar = new HBox(scoreLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        boardCanvas = new Canvas(board.width() * cellSize, board.height() * cellSize);
        boardCanvas.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");

        nextCanvas = new Canvas(6 * cellSize, 6 * cellSize);

// Info labels (match Gameplay)
        playerTypeLabel = new Label("Player: " + currentPlayerType());
        playerTypeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        levelLabel = new Label("Level: " + config.getGameLevel());
        levelLabel.setStyle("-fx-font-size: 13px;");

        linesLabel = new Label("Lines: 0");
        linesLabel.setStyle("-fx-font-size: 13px;");

// Styled info box (same style string you use in Gameplay)
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

// Final sidebar
        VBox rightBar = new VBox(
                new Label("Next"),
                nextCanvas,
                new javafx.scene.control.Separator(),
                infoBox
        );
        rightBar.setAlignment(Pos.TOP_CENTER);
        rightBar.setSpacing(12);
        rightBar.setPadding(new Insets(10));
        rightBar.setStyle("-fx-background-color: #fafafa;");

        setRight(rightBar);


        setTop(topBar);
        setCenter(boardCanvas);
        setRight(rightBar);
        setStyle("-fx-background-color: #f9f9f9;");

        // ---- Audio setup (music + beep) ----
        if (config.isMusic()) {
            URL musicUrl = getClass().getResource("/sounds/background.mp3");
            if (musicUrl != null) {
                Media backgroundMusic = new Media(musicUrl.toExternalForm());
                musicPlayer = new MediaPlayer(backgroundMusic);
                musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                musicPlayer.setOnReady(() -> musicPlayer.play());
            }
        }

        URL soundUrl = getClass().getResource("/sounds/erase-line.wav");
        if (soundUrl != null) {
            Media beep = new Media(soundUrl.toExternalForm());
            beepPlayer = new MediaPlayer(beep);
            beepPlayer.setOnEndOfMedia(() -> beepPlayer.stop());
        }

        resetGameState();
        spawnNewPiece();

        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                applyAutoBoostIfNeeded();
                if (!paused && !gameOver) {
                    if (useExternal
                            && net != null && net.isConnected()
                            && current != null && !gameOver
                            && !extControlsThisPiece
                            && !aiAnimating
                            && !extLateJoinAsked) {
                        requestExternalForCurrent();
                    }
                    if (lastDropTime == 0) lastDropTime = now;

                    if (now - lastDropTime > dropSpeed) {
                        if (aiAnimating) {
                            doOneAiStep();
                        }

                        boolean fell = tryBoost();

                        if (!fell) {
                            lockPiece();
                            aiAnimating = false;
                            extControlsThisPiece = false;
                        }

                        lastDropTime = now;
                    }
                }
                draw(boardCanvas.getGraphicsContext2D());
            }
        };

        timer.start();
    }

    public void tryMoveLeft()  {
        if (!paused && !extControlsThisPiece) {
            if (move(-1, 0) && config.isSoundEffect()) playMoveTurn();
        }
    }
    public void tryMoveRight() {
        if (!paused && !extControlsThisPiece) {
            if (move(+1, 0) && config.isSoundEffect()) playMoveTurn();
        }
    }
    private void playMoveTurn() { playSound("/sounds/move-turn.wav"); }

    // was: public void tryRotate() { if (!paused && !extControlsThisPiece) rotator.tryRotateCW(current, board); }
    public void tryRotate() {
        if (!paused && !extControlsThisPiece) {
            boolean ok = rotator.tryRotateCW(current, board);
            if (ok && config.isSoundEffect()) playMoveTurn();
        }
    }
    public void boost(boolean pressed) {
        humanBoosting = pressed;
        applyAutoBoostIfNeeded();
    }

    public void pauseToggle() {
        paused = !paused;
        if (!paused) {
            lastDropTime = 0;
            if (musicPlayer != null && config.isMusic()) musicPlayer.play();
        } else {
            if (musicPlayer != null && config.isMusic()) musicPlayer.pause();
        }
    }

    public void setSeed(long seed) { rng.setSeed(seed); }

    public void enableExternal(String host, int port) throws Exception {
        useExternal = true;
        if (playerTypeLabel != null) playerTypeLabel.setText("Player: " + currentPlayerType());

        net = new ExternalPlayerClient(host, port);
        extPlayer = new ExternalPlayer(net);
        try {
            net.connect();
            if (!net.isConnected()) {
                throw new IllegalStateException("External player not reachable at " + host + ":" + port);
            }
            applyAutoBoostIfNeeded();

            if (current != null && !gameOver) {
                var snap = snapshot();
                extPlayer.requestMoveAsync(
                        snap,
                        mv -> Platform.runLater(() -> {
                            aiRotLeft = mv.opRotate & 3;
                            aiTargetX = mv.opX;
                            aiPhase = AiPhase.ROTATE;
                            aiAnimating = true;
                            extControlsThisPiece = true;
                            dropSpeed = BOOST_NANOS;
                            lastDropTime = 0L;
                            System.out.println("[EXT] plan (late join): rotate=" + aiRotLeft + " targetLeft=" + aiTargetX);
                            doOneAiStep();
                        }),
                        err -> Platform.runLater(() -> {
                            System.err.println("[EXT] request failed (late join): " + err.getMessage());
                        })
                );
            }

        } catch (Exception e) {
            if (net != null) {
                try { net.disconnect(); } catch (Exception ignore) {}
            }
            net = null;
            throw e;
        }
    }

    public void dispose() {
        if (timer != null) timer.stop();
        if (musicPlayer != null) { musicPlayer.stop(); musicPlayer.dispose(); musicPlayer = null; }
        if (net != null) { net.disconnect(); net = null; extPlayer = null; }
    }

    private void resetGameState() {
        linesCleared = 0;
        if (linesLabel != null) linesLabel.setText("Lines: 0");
        if (playerTypeLabel != null) playerTypeLabel.setText("Player: " + currentPlayerType());
        if (levelLabel != null) levelLabel.setText("Level: " + config.getGameLevel());

        board = new Board();
        score = 0; paused = false; gameOver = false;
        lastDropTime = 0L; dropSpeed = baseDropSpeed();
        if (scoreLabel != null) scoreLabel.setText("Score: 0");
        nextType  = randomType();
        nextColor = randomColor();

        if (boardCanvas != null) {
            boardCanvas.setWidth(board.width() * cellSize);
            boardCanvas.setHeight(board.height() * cellSize);
        }
    }

    private void spawnNewPiece() {
        TetrominoType type = nextType;
        Color color = nextColor;
        nextType  = randomType();
        nextColor = randomColor();

        aiAnimating = false;
        extControlsThisPiece = false;
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
                    applyAutoBoostIfNeeded();
                    var snap = snapshot();

                    extPlayer.requestMoveAsync(
                            snap,
                            mv -> {
                                int r = mv.opRotate & 3;
                                aiRotLeft = r;
                                aiTargetX = mv.opX;
                                aiPhase = AiPhase.ROTATE;
                                aiAnimating = true;
                                extControlsThisPiece = true;
                                lastDropTime = 0L;
                                System.out.println("[EXT] plan: rotate=" + aiRotLeft + " targetLeft=" + aiTargetX);
                            },
                            err -> {
                                System.err.println("[EXT] request failed: " + err.getMessage());
                            }
                    );
                } else {
                    System.err.println("[EXT] Failed to reconnect to external server.");
                }
            } catch (Exception e) {
                useExternal = false;
                extPlayer = null;
                net = null;
                System.err.println("[EXT] Connection error: " + e.getMessage());
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
                        applyAutoBoostIfNeeded();
                        lastDropTime = 0L;
                        System.out.println("[AI] plan: rotate=" + aiRotLeft + " targetLeft=" + aiTargetX);
                    },
                    err -> { extControlsThisPiece = false; }
            );
        }
        extLateJoinAsked = false;
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

        // NEW: track lines like Gameplay
        linesCleared += cleared;
        if (linesLabel != null) linesLabel.setText("Lines: " + linesCleared);

        score += ScoreService.pointsFor(cleared);
        if (scoreLabel != null) scoreLabel.setText("Score: " + score);

        if (cleared > 0 && config.isSoundEffect() && beepPlayer != null) {
            beepPlayer.stop();
            beepPlayer.play();
        }
        aiAnimating = false;
        extControlsThisPiece = false;
        extLateJoinAsked = false;
        spawnNewPiece();
    }


    private boolean move(int dx, int dy) {
        int beforeX = currentLeft();
        current.moveBy(dx, dy);
        if (!board.canPlace(current)) {
            current.moveBy(-dx, -dy);
            return false;
        }
        return (dx != 0 || dy != 0) && (currentLeft() != beforeX || dy != 0);
    }

    private void draw(GraphicsContext gc) {
        int W = board.width(), H = board.height();
        gc.clearRect(0, 0, W * cellSize, H * cellSize);

        gc.setStroke(Color.LIGHTGRAY);
        for (int y=0; y<H; y++) for (int x=0; x<W; x++)
            gc.strokeRect(x*cellSize, y*cellSize, cellSize, cellSize);

        for (int y=0; y<H; y++) for (int x=0; x<W; x++) {
            Color cell = board.cells()[y][x];
            if (cell != null) {
                double px = x*cellSize, py = y*cellSize;
                gc.setFill(cell); gc.fillRect(px, py, cellSize, cellSize);
                gc.setStroke(Color.BLACK); gc.strokeRect(px, py, cellSize, cellSize);
            }
        }

        gc.setFill(currentColor);
        for (Vec c : current.worldCells()) {
            double px = c.x()*cellSize, py = c.y()*cellSize;
            gc.fillRect(px, py, cellSize, cellSize);
            gc.setStroke(Color.BLACK); gc.strokeRect(px, py, cellSize, cellSize);
        }

        if (paused || gameOver) {
            double w = W*cellSize, h = H*cellSize;
            gc.save();
            gc.setGlobalAlpha(0.45); gc.setFill(Color.BLACK); gc.fillRect(0,0,w,h);
            gc.setGlobalAlpha(1.0); gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER); gc.setTextBaseline(VPos.CENTER);
            String title = gameOver ? "Game Over" : "PAUSED";
            String hint  = gameOver ? "Press Back" : "Press 'P'";
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            gc.fillText(title, w/2.0, h/2.0 - 18);
            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
            gc.fillText(hint,  w/2.0, h/2.0 + 16);
            gc.restore();
        }

        drawNextPreview();
    }

    private void drawNextPreview() {
        if (nextCanvas == null) return;
        GraphicsContext ng = nextCanvas.getGraphicsContext2D();
        ng.clearRect(0, 0, nextCanvas.getWidth(), nextCanvas.getHeight());

        Vec[] offs = nextType.offsets();

        int minX=Integer.MAX_VALUE, maxX=Integer.MIN_VALUE;
        int minY=Integer.MAX_VALUE, maxY=Integer.MIN_VALUE;
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
        p.width = board.width(); p.height = board.height();
        p.cells = new int[p.height][p.width];
        for (int y=0;y<p.height;y++) for (int x=0;x<p.width;x++)
            p.cells[y][x] = (board.cells()[y][x] != null) ? 1 : 0;
        p.currentShape = toMatrixFromCells(current.worldCells());
        p.nextShape    = toMatrixFromCells(java.util.Arrays.asList(nextType.offsets()));
        return p;
    }
    private int[][] toMatrixFromCells(java.util.Collection<Vec> cells) {
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE;
        for (Vec v : cells){ minX=Math.min(minX,v.x()); minY=Math.min(minY,v.y()); maxX=Math.max(maxX,v.x()); maxY=Math.max(maxY,v.y()); }
        int w=maxX-minX+1,h=maxY-minY+1; int[][] m=new int[h][w];
        for (Vec v : cells) m[v.y()-minY][v.x()-minX]=1; return m;
    }


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

    private int clampTargetX(int desiredX) {
        if (desiredX < 0) return 0;
        if (desiredX >= board.width()) return board.width() - 1;
        return desiredX;
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

        // optional finish sound
        playSound("/sounds/game-finish.wav");

        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog("Player");
            dialog.setTitle("Game Over");
            dialog.setHeaderText("Your Score: " + score);
            dialog.setContentText("Enter your name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                HighScoreManager manager = new HighScoreManager();
                String type = currentPlayerType(); // "Human", "AI", or "External"
                manager.addScore(new Score(name, score, type));
            });
        });
    }

    private void requestExternalForCurrent() {
        if (!useExternal || net == null || !net.isConnected() || current == null || gameOver) return;

        var snap = snapshot();
        extPlayer.requestMoveAsync(
                snap,
                mv -> Platform.runLater(() -> {
                    aiRotLeft = mv.opRotate & 3;
                    aiTargetX = mv.opX;
                    aiPhase = AiPhase.ROTATE;
                    aiAnimating = true;
                    extControlsThisPiece = true;
                    dropSpeed = BOOST_NANOS;
                    lastDropTime = 0L;
                    extLateJoinAsked = true;
                    doOneAiStep();
                }),
                err -> Platform.runLater(() -> {
                    System.err.println("[EXT] late-join request failed: " + err.getMessage());
                    extLateJoinAsked = false;
                })
        );
    }

}
