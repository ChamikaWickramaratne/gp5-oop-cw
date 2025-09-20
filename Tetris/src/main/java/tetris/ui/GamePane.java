// src/main/java/tetris/ui/GamePane.java
package tetris.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import tetris.model.Board;
import tetris.model.TetrominoType;
import tetris.model.Vec;
import tetris.model.piece.ActivePiece;
import tetris.model.rules.RotationStrategy;
import tetris.model.rules.SrsRotation;
import tetris.service.ScoreService;

public class GamePane extends BorderPane {
    // ====== copied/trimmed from Gameplay ======
    private static final int cellSize = 20;
    private final int width = 10, height = 20;

    private long lastDropTime = 0L;
    private long dropSpeed   = 1_000_000_000L;
    private boolean paused   = false;
    private boolean gameOver = false;
    private int score = 0;

    private static final Color[] colourOptions = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW
    };

    private final java.util.Random rng = new java.util.Random();
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
    private tetris.net.INetwork net;
    private tetris.players.Player extPlayer;
    private boolean extControlsThisPiece = false;

    public GamePane() {
        // UI
        scoreLabel = new Label();
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox topBar = new HBox(scoreLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        boardCanvas = new Canvas(width * cellSize, height * cellSize);
        boardCanvas.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");

        nextCanvas = new Canvas(6 * cellSize, 6 * cellSize);
        VBox rightBar = new VBox(new Label("Next"), nextCanvas);
        rightBar.setAlignment(Pos.TOP_CENTER);
        rightBar.setSpacing(6);
        rightBar.setPadding(new Insets(10));

        setTop(topBar);
        setCenter(boardCanvas);
        setRight(rightBar);
        setStyle("-fx-background-color: #f9f9f9;");

        // start game loop
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

    // ---------- public controls so parent can map keys ----------
    public void tryMoveLeft()  { if (!paused && !extControlsThisPiece) move(-1, 0); }
    public void tryMoveRight() { if (!paused && !extControlsThisPiece) move(+1, 0); }
    public void tryRotate()    { if (!paused && !extControlsThisPiece) rotator.tryRotateCW(current, board); }
    public void boost(boolean pressed) { dropSpeed = pressed ? 100_000_000L : 1_000_000_000L; }
    public void pauseToggle() { paused = !paused; if (!paused) lastDropTime = 0; }

    // seed / external toggles
    public void setSeed(long seed) { rng.setSeed(seed); }
    public void enableExternal(String host, int port) {
        useExternal = true;
        net = new tetris.net.ExternalPlayerClient(host, port);
        extPlayer = new tetris.players.ExternalPlayer(net);
        net.connect();
    }

    // clean shutdown (call when leaving screen)
    public void dispose() {
        if (timer != null) timer.stop();
        if (net != null) { net.disconnect(); net = null; extPlayer = null; }
    }

    // ---------- internals (copied from Gameplay, trimmed) ----------
    private void resetGameState() {
        board = new Board();
        score = 0; paused = false; gameOver = false;
        lastDropTime = 0L; dropSpeed = 1_000_000_000L;
        if (scoreLabel != null) scoreLabel.setText("Score: 0");
        nextType  = randomType();
        nextColor = randomColor();
    }

    private void spawnNewPiece() {
        TetrominoType type = nextType; Color color = nextColor;
        nextType  = randomType(); nextColor = randomColor();

        Vec[] base = type.offsets();
        int minX=Integer.MAX_VALUE, maxX=Integer.MIN_VALUE;
        for (Vec v : base) { if (v.x()<minX) minX=v.x(); if (v.x()>maxX) maxX=v.x(); }
        int shapeWidth = (maxX - minX + 1);
        int startCol = Math.max(0, (width - shapeWidth) / 2 - minX);

        current = new ActivePiece(type, new Vec(startCol, 0));
        currentColor = color;

        for (Vec c : current.worldCells()) {
            if (c.y() < 0 || c.y() >= height || c.x() < 0 || c.x() >= width || board.cells()[c.y()][c.x()] != null) {
                gameOver = true; return;
            }
        }

        // ask external brain for this piece (optional)
        if (useExternal && extPlayer != null && net != null) {
            extControlsThisPiece = true;
            var snap = snapshot();
            extPlayer.requestMoveAsync(
                    snap,
                    mv  -> { extControlsThisPiece = false; applyExternalMove(mv); },
                    err -> { extControlsThisPiece = false; /* fallback silently */ }
            );
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

    private void move(int dx, int dy) {
        current.moveBy(dx, dy);
        if (!board.canPlace(current)) current.moveBy(-dx, -dy);
    }

    private void draw(GraphicsContext gc) {
        int W = width, H = height;
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
            gc.setFont(javafx.scene.text.Font.font("Arial", FontWeight.BOLD, 36));
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

    // ---------- external snapshot & apply ----------
    private tetris.dto.PureGame snapshot() {
        tetris.dto.PureGame p = new tetris.dto.PureGame();
        p.width = width; p.height = height;
        p.cells = new int[height][width];
        for (int y=0;y<height;y++) for (int x=0;x<width;x++)
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
    private void applyExternalMove(tetris.dto.OpMove mv) {
        // 1) rotate CW safely
        int r = mv.opRotate & 3; // 0..3
        for (int i = 0; i < r; i++) {
            rotator.tryRotateCW(current, board);
        }

        // 2) clamp target x to a sane range
        int target = clampTargetX(mv.opX);

        // 3) step toward target, but bail if we can't move further
        int guard = 0;
        while (current.origin().x() != target && guard++ < (width * 2)) {
            int dir = (target > current.origin().x()) ? +1 : -1;
            int before = current.origin().x();
            move(dir, 0);
            if (current.origin().x() == before) {
                // stuck (blocked by wall/pile) → stop trying to reach target
                break;
            }
        }

        // 4) hard drop
        while (tryBoost()) { /* fall until blocked */ }

        // 5) lock as usual
        lockPiece();
    }

    /** Clamp desired x to board range. (Conservative: 0..width-1)  */
    private int clampTargetX(int desiredX) {
        if (desiredX < 0) return 0;
        if (desiredX >= width) return width - 1;
        return desiredX;
    }
}
