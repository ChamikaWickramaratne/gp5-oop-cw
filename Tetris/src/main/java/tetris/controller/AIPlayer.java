package tetris.controller;

import javafx.application.Platform;
import tetris.model.dto.OpMove;
import tetris.model.dto.PureGame;
import tetris.model.ai.Heuristic;

import java.util.concurrent.*;

public class AIPlayer implements Player {
    private final Heuristic heuristic;                       // Heuristic used to score boards
    private final ExecutorService exec = Executors.newSingleThreadExecutor(); // Background thread for AI
    private static final double LOOKAHEAD_GAMMA = 0.9;       // Discount for next-piece lookahead

    public AIPlayer(Heuristic heuristic) { this.heuristic = heuristic; }

    @Override
    public void requestMoveAsync(PureGame game,
                                 java.util.function.Consumer<OpMove> onReady,
                                 java.util.function.Consumer<Throwable> onError) {
        // Compute move off the FX thread, then deliver result back on FX thread
        exec.submit(() -> {
            try {
                OpMove mv = computeBest(game);               // Find best rotation/column for current piece
                Platform.runLater(() -> onReady.accept(mv)); // Notify UI/game logic safely
            } catch (Throwable t) {
                Platform.runLater(() -> onError.accept(t));  // Surface any failure
            }
        });
    }

    private OpMove computeBest(PureGame g) {
        int[][] board = clone2D(g.cells);                    // Work on a copy of the board
        int bestRot = 0, bestX = 0;
        double bestScore = -1e100;                           // Very low sentinel score

        // Try all 4 rotations
        for (int rot = 0; rot < 4; rot++) {
            int[][] shape = rotate(g.currentShape, rot);
            int W = board[0].length, w = shape[0].length;

            // Slide across all legal x positions
            for (int x = 0; x <= W - w; x++) {
                SimResult cur = simulateDrop(board, shape, x); // Drop piece and get resulting board
                if (!cur.valid) continue;

                // Base score: evaluate resulting board after drop/clears
                double base = heuristic.evaluate(cur.after, cur.linesCleared);

                // One-piece lookahead using nextShape (if present)
                double look = 0.0;
                if (g.nextShape != null && g.nextShape.length > 0) {
                    look = bestNextScore(cur.after, g.nextShape);
                }

                // Combine immediate and lookahead scores
                double total = base + LOOKAHEAD_GAMMA * look;

                // Track the best (rotation, x)
                if (total > bestScore) {
                    bestScore = total;
                    bestRot = rot;
                    bestX   = x;
                }
            }
        }

        // Return the chosen operation
        OpMove mv = new OpMove();
        mv.opX = bestX;            // target left column
        mv.opRotate = bestRot;     // number of CW rotations
        return mv;
    }

    // Result of simulating a drop
    private static class SimResult {
        final boolean valid; final int[][] after; final int linesCleared;
        SimResult(boolean v, int[][] a, int lc){ valid=v; after=a; linesCleared=lc; }
    }

    // Simulate dropping 'shape' at x offset 'ox' on 'board'
    private SimResult simulateDrop(int[][] board, int[][] shape, int ox) {
        int H = board.length, W = board[0].length, h = shape.length, w = shape[0].length;
        int y = -h; // start above the board so tall shapes can enter

        while (true) {
            // If moving one row down collides, place here (if placement is valid)
            if (collides(board, shape, ox, y + 1)) {
                if (collides(board, shape, ox, y)) return new SimResult(false, null, 0); // can't even place
                int[][] placed = clone2D(board);

                // Merge shape into board
                for (int r=0; r<h; r++) for (int c=0; c<w; c++)
                    if (shape[r][c] != 0) {
                        int px = ox + c, py = y + r;
                        if (py >= 0) placed[py][px] = 1; // ignore parts still above top
                    }

                // Clear full lines and count how many were removed
                int[][] cleared = clearLinesAnyFull(placed);
                int lines = countLinesRemoved(placed, cleared);
                return new SimResult(true, cleared, lines);
            }

            y++;                    // keep falling
            if (y > H) return new SimResult(false, null, 0); // safety break (shouldn't happen)
        }
    }

    // Collision check between shape at (ox, oy) and board edges/blocks
    private static boolean collides(int[][] board, int[][] shape, int ox, int oy) {
        int H = board.length, W = board[0].length;
        int h = shape.length, w = shape[0].length;
        for (int r=0; r<h; r++) for (int c=0; c<w; c++) if (shape[r][c] != 0) {
            int x = ox + c, y = oy + r;
            if (x < 0 || x >= W || y >= H) return true;     // outside bounds
            if (y >= 0 && board[y][x] != 0) return true;     // hits an existing cell
        }
        return false;
    }

    // Remove any full lines and compact the board downward
    private static int[][] clearLinesAnyFull(int[][] b){
        int H = b.length, W = b[0].length;
        int[][] out = new int[H][W];
        int dst = H - 1;                                     // fill from bottom up

        for (int src = H - 1; src >= 0; src--){
            boolean full = true;
            for (int x=0;x<W;x++) if (b[src][x]==0){ full=false; break; }
            if (!full) {
                out[dst] = b[src].clone();                   // keep non-full row
                dst--;
            }
        }

        // Fill remaining top rows with empty
        while (dst >= 0) {
            out[dst] = new int[W];
            dst--;
        }
        return out;
    }

    // Approximate number of lines removed from before→after
    private static int countLinesRemoved(int[][] before, int[][] after){
        int H = before.length, W = before[0].length;
        int cb=0, ca=0;
        for (int y=0;y<H;y++) for (int x=0;x<W;x++){
            cb += (before[y][x]!=0?1:0);
            ca += (after [y][x]!=0?1:0);
        }
        return Math.max(0, (cb - ca) / W);                   // cells diff normalized by width
    }

    // Rotate a matrix CW 'times' (0..3)
    private static int[][] rotate(int[][] m, int times){
        times &= 3;
        int[][] r = m;
        for (int i=0;i<times;i++) r = rotCW(r);
        return r;
    }

    // Single 90° CW rotation
    private static int[][] rotCW(int[][] m){
        int h = m.length, w = m[0].length;
        int[][] r = new int[w][h];
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) r[x][h-1-y] = m[y][x];
        return r;
    }

    // Deep copy of 2D int array
    private static int[][] clone2D(int[][] a){
        int[][] c = new int[a.length][];
        for (int i=0;i<a.length;i++) c[i] = a[i].clone();
        return c;
    }

    // Evaluate the best possible score for the next piece on a given board
    private double bestNextScore(int[][] boardAfterCurrent, int[][] nextShape) {
        double best = -1e100;
        for (int rot = 0; rot < 4; rot++) {
            int[][] shp = rotate(nextShape, rot);
            int W = boardAfterCurrent[0].length, w = shp[0].length;

            for (int x = 0; x <= W - w; x++) {
                SimResult nxt = simulateDrop(boardAfterCurrent, shp, x);
                if (!nxt.valid) continue;

                double s = heuristic.evaluate(nxt.after, nxt.linesCleared); // score next-state board
                if (s > best) best = s;
            }
        }
        return (best == -1e100) ? 0.0 : best;                 // 0 if no legal placement
    }
}
