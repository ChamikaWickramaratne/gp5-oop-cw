package tetris.model;

import javafx.scene.paint.Color;
import tetris.model.piece.ActivePiece;

/**
 * Instance-scoped playfield (no static/shared grid).
 */
public class Board {
    public static final int CELL = 20; // fine to keep static (UI constant)

    // Instance state
    private final int w;
    private final int h;
    private final Color[][] grid;

    /** Default 10x20 board. */
    public Board() {
        this(10, 20);
    }

    /** Sized board (e.g., from config). */
    public Board(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid board size: " + width + "x" + height);
        }
        this.w = width;
        this.h = height;
        this.grid = new Color[h][w];
    }

    // ---- Instance API (use these everywhere) ----
    public int width()  { return w; }
    public int height() { return h; }
    public Color[][] cells() { return grid; }

    public boolean inside(int x, int y) {
        return x >= 0 && x < w && y >= 0 && y < h;
    }

    public boolean occupied(int x, int y) {
        return grid[y][x] != null;
    }

    public boolean canPlace(ActivePiece p) {
        for (Vec c : p.worldCells()) {
            if (!inside(c.x(), c.y()) || occupied(c.x(), c.y())) return false;
        }
        return true;
    }

    /** Try to nudge a piece by (dx,dy); revert if collision. */
    public boolean tryNudge(ActivePiece p, int dx, int dy) {
        p.moveBy(dx, dy);
        if (canPlace(p)) return true;
        p.moveBy(-dx, -dy);
        return false;
    }

    /** Lock the piece's blocks into this board (bounds-checked). */
    public void lock(ActivePiece p, Color color) {
        for (Vec c : p.worldCells()) {
            if (inside(c.x(), c.y())) {
                grid[c.y()][c.x()] = color;
            }
        }
    }

    /**
     * Clear all full rows; return number cleared.
     * Compact with a write-pointer from bottom to top.
     */
    public int clearLines() {
        int write = h - 1;
        int cleared = 0;

        for (int y = h - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < w; x++) {
                if (grid[y][x] == null) { full = false; break; }
            }
            if (!full) {
                if (write != y) {
                    // copy row y -> write
                    for (int x = 0; x < w; x++) grid[write][x] = grid[y][x];
                }
                write--;
            } else {
                cleared++;
            }
        }

        // clear rows 0..write
        for (int y = write; y >= 0; y--) {
            for (int x = 0; x < w; x++) grid[y][x] = null;
        }
        return cleared;
    }

    // ---- Legacy getters (if some old code still calls them) ----
    // Prefer width()/height() above; keep these only if you truly need them.
    public int getWidth()  { return w; }
    public int getHeight() { return h; }
}
