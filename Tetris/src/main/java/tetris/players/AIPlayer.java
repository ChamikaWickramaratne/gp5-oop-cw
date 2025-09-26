// src/main/java/tetris/players/AIPlayer.java
package tetris.players;

import javafx.application.Platform;
import tetris.dto.OpMove;
import tetris.dto.PureGame;
import tetris.ai.Heuristic;

import java.util.concurrent.*;

public class AIPlayer implements Player {
    private final Heuristic heuristic;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private static final double LOOKAHEAD_GAMMA = 0.9;

    public AIPlayer(Heuristic heuristic) { this.heuristic = heuristic; }

    @Override
    public void requestMoveAsync(PureGame game,
                                 java.util.function.Consumer<OpMove> onReady,
                                 java.util.function.Consumer<Throwable> onError) {
        exec.submit(() -> {
            try {
                OpMove mv = computeBest(game);
                Platform.runLater(() -> onReady.accept(mv));
            } catch (Throwable t) {
                Platform.runLater(() -> onError.accept(t));
            }
        });
    }

    private OpMove computeBest(PureGame g) {
        int[][] board = clone2D(g.cells);
        int bestRot = 0, bestX = 0;
        double bestScore = -1e100;

        for (int rot = 0; rot < 4; rot++) {
            int[][] shape = rotate(g.currentShape, rot);
            int W = board[0].length, w = shape[0].length;
            for (int x = 0; x <= W - w; x++) {
                SimResult cur = simulateDrop(board, shape, x);
                if (!cur.valid) continue;

                // base score: after placing the current piece
                double base = heuristic.evaluate(cur.after, cur.linesCleared);

                // one-ply lookahead using nextShape (if present)
                double look = 0.0;
                if (g.nextShape != null && g.nextShape.length > 0) {
                    look = bestNextScore(cur.after, g.nextShape);
                }

                double total = base + LOOKAHEAD_GAMMA * look;
                if (total > bestScore) {
                    bestScore = total;
                    bestRot = rot;
                    bestX   = x;
                }
            }
        }
        OpMove mv = new OpMove();
        mv.opX = bestX;
        mv.opRotate = bestRot;
        return mv;
    }

    // ---- simulation helpers ----
    private static class SimResult {
        final boolean valid; final int[][] after; final int linesCleared;
        SimResult(boolean v, int[][] a, int lc){ valid=v; after=a; linesCleared=lc; }
    }

    private SimResult simulateDrop(int[][] board, int[][] shape, int ox) {
        int H = board.length, W = board[0].length, h = shape.length, w = shape[0].length;
        int y = -h;
        while (true) {
            if (collides(board, shape, ox, y + 1)) {
                // place at y
                if (collides(board, shape, ox, y)) return new SimResult(false, null, 0);
                int[][] placed = clone2D(board);
                for (int r=0; r<h; r++) for (int c=0; c<w; c++)
                    if (shape[r][c] != 0) {
                        int px = ox + c, py = y + r;
                        if (py >= 0) placed[py][px] = 1;
                    }

                // --- STANDARD "CLEAR ANY FULL ROWS" LOGIC ---
                int[][] cleared = clearLinesAnyFull(placed);
                int lines = countLinesRemoved(placed, cleared);
                return new SimResult(true, cleared, lines);
            }
            y++;
            if (y > H) return new SimResult(false, null, 0);
        }
    }

    private static boolean collides(int[][] board, int[][] shape, int ox, int oy) {
        int H = board.length, W = board[0].length;
        int h = shape.length, w = shape[0].length;
        for (int r=0; r<h; r++) for (int c=0; c<w; c++) if (shape[r][c] != 0) {
            int x = ox + c, y = oy + r;
            if (x < 0 || x >= W || y >= H) return true;
            if (y >= 0 && board[y][x] != 0) return true;
        }
        return false;
    }

    /** Clears all full rows anywhere (standard Tetris). */
    private static int[][] clearLinesAnyFull(int[][] b){
        int H = b.length, W = b[0].length;
        int[][] out = new int[H][W];
        int dst = H - 1;
        for (int src = H - 1; src >= 0; src--){
            boolean full = true;
            for (int x=0;x<W;x++) if (b[src][x]==0){ full=false; break; }
            if (!full) {
                out[dst] = b[src].clone();
                dst--;
            }
        }
        while (dst >= 0) {
            out[dst] = new int[W];
            dst--;
        }
        return out;
    }

    private static int countLinesRemoved(int[][] before, int[][] after){
        int H = before.length, W = before[0].length;
        int cb=0, ca=0;
        for (int y=0;y<H;y++) for (int x=0;x<W;x++){ cb += (before[y][x]!=0?1:0); ca += (after[y][x]!=0?1:0); }
        return Math.max(0, (cb - ca) / W);
    }

    private static int[][] rotate(int[][] m, int times){
        times &= 3;
        int[][] r = m;
        for (int i=0;i<times;i++) r = rotCW(r);
        return r;
    }
    private static int[][] rotCW(int[][] m){
        int h = m.length, w = m[0].length;
        int[][] r = new int[w][h];
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) r[x][h-1-y] = m[y][x];
        return r;
    }

    private static int[][] clone2D(int[][] a){
        int[][] c = new int[a.length][];
        for (int i=0;i<a.length;i++) c[i] = a[i].clone();
        return c;
    }

    private double bestNextScore(int[][] boardAfterCurrent, int[][] nextShape) {
        double best = -1e100;
        for (int rot = 0; rot < 4; rot++) {
            int[][] shp = rotate(nextShape, rot);
            int W = boardAfterCurrent[0].length, w = shp[0].length;
            for (int x = 0; x <= W - w; x++) {
                SimResult nxt = simulateDrop(boardAfterCurrent, shp, x);
                if (!nxt.valid) continue;
                double s = heuristic.evaluate(nxt.after, nxt.linesCleared);
                if (s > best) best = s;
            }
        }
        // If no valid next placement somehow, treat as 0 lookahead value
        return (best == -1e100) ? 0.0 : best;
    }
}
