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

import tetris.dto.OpMove;
import tetris.config.ConfigService;
import tetris.config.TetrisConfig;
import tetris.model.Board;
import tetris.model.TetrominoType;
import tetris.model.Vec;
import tetris.model.piece.ActivePiece;
import tetris.model.rules.RotationStrategy;
import tetris.model.rules.SrsRotation;
import tetris.service.ScoreService;
import tetris.service.HighScoreManager;
import tetris.service.Score;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

import java.util.Optional;
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
    private MediaPlayer musicPlayer;
    private MediaPlayer beepPlayer;


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

    private Stage mainStage;

    @Override
    public void start(Stage stage) {

        this.mainStage = stage;
        
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
        backButton.setOnAction(e -> {
            if (gameOver) {
                if (timer != null) timer.stop();

                // stop music before leaving
                if (musicPlayer != null) {
                    musicPlayer.stop();
                }

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

                        // stop music before leaving
                        if (musicPlayer != null) {
                            musicPlayer.stop();
                        }

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
        // backButton.setOnAction(e -> handleBack(stage));

        // === Background Music setup ===
        if (config.isMusic()) {
            URL musicUrl = getClass().getResource("/sounds/theme.mp3");
            if (musicUrl != null) {
                Media backgroundMusic = new Media(musicUrl.toExternalForm());
                musicPlayer = new MediaPlayer(backgroundMusic);
                musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);

                musicPlayer.setOnReady(() -> {
                    musicPlayer.play();
                });
            }
        }



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
                case M -> toggleMusic();
                case N -> toggleSound();
            }
        });
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

        URL soundUrl = getClass().getResource("/sounds/beep.mp3");
        if (soundUrl != null) {
            Media beep = new Media(soundUrl.toExternalForm());
            beepPlayer = new MediaPlayer(beep);
            beepPlayer.setOnEndOfMedia(() -> beepPlayer.stop());  // reset after play
        }

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
        nextType  = randomType();
        nextColor = randomColor();
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
        //TetrominoType type = TetrominoType.values()[rng.nextInt(TetrominoType.values().length)];
        Vec[] base = type.offsets();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        for (Vec v : base) { if (v.x() < minX) minX = v.x(); if (v.x() > maxX) maxX = v.x(); }
        int shapeWidth = (maxX - minX + 1);
        int startCol = Math.max(0, (board.width() - shapeWidth) / 2 - minX);

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
        // game over check. if cant spawn a piece at the top
        for (Vec c : current.worldCells()) {
            if (!board.inside(c.x(), c.y()) || board.occupied(c.x(), c.y())) {
                gameOver = true;

                // stop background music if game is over
                if (musicPlayer != null) {
                    musicPlayer.stop();
                }
              
                handleGameOver();
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

    //lock piece when they hit the bottom
    private void lockPiece() {
        board.lock(current, currentColor);
        int cleared = board.clearLines();
        score += ScoreService.pointsFor(cleared);
        if (scoreLabel != null) scoreLabel.setText("Score: " + score);

        if (cleared > 0 && config.isSoundEffect() && beepPlayer != null) {
            beepPlayer.stop();   // ensure it starts from beginning
            beepPlayer.play();
        }


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
        dropSpeed = pressed ? 100_000_000L : 1_000_000_000L / Math.max(1, config.getGameLevel());
    }


    private void pauseGame() {
        paused = !paused;
        if (paused) {
            // pause background music if it’s playing
            if (musicPlayer != null && config.isMusic()) {
                musicPlayer.pause();
            }
        } else {
            // resume music only if music is enabled
            if (musicPlayer != null && config.isMusic()) {
                musicPlayer.play();
            }
            lastDropTime = 0; // reset drop timer so piece doesn’t insta-drop
        }
    }

    //drawing the board each time
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
  
    private void toggleMusic() {
        boolean newVal = !config.isMusic();
        config.setMusic(newVal);
        ConfigService.save(config);
        if (newVal) {
            initMusicPlayerIfNeeded();
            if (musicPlayer != null) {
                // play immediately if ready; otherwise play when ready
                musicPlayer.setOnReady(() -> musicPlayer.play());
                try { musicPlayer.play(); } catch (Exception ignore) { /* onReady fallback */ }
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
        URL musicUrl = getClass().getResource("/sounds/theme.mp3");
        if (musicUrl == null) {
            System.err.println("theme.mp3 not found under /sounds");
            return;
        }
        Media bg = new Media(musicUrl.toExternalForm());
        musicPlayer = new MediaPlayer(bg);
        musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
    }

    // new method: handle saving score on game over
    private void handleGameOver() {
        if (timer != null) timer.stop();
        gameOver = true;

        // Run dialog after timer stops to avoid IllegalStateException
        javafx.application.Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog("Player");
            dialog.initOwner(mainStage);
            dialog.setTitle("Game Over");
            dialog.setHeaderText("Your Score: " + score);
            dialog.setContentText("Enter your name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                HighScoreManager manager = new HighScoreManager();
                manager.addScore(new Score(name, score));
            });

            try {
                new HighScore().start(mainStage); // show high score screen
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    public static void main(String[] args) { launch(args); }
}
