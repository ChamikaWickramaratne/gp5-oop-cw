package tetris.model;

import javafx.scene.paint.Color;
import tetris.model.piece.ActivePiece;

public class Board {
    public static final int WIDTH = 10, HEIGHT = 20, CELL = 20;

    private final Color[][] grid = new Color[HEIGHT][WIDTH];

    public Color[][] cells() { return grid; }

    public boolean inside(int x, int y) { return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT; }
    public boolean occupied(int x, int y) { return grid[y][x] != null; }

    public boolean canPlace(ActivePiece p) {
        for (Vec c : p.worldCells()) {
            if (!inside(c.x(), c.y()) || occupied(c.x(), c.y())) return false;  // ENHANCED FOR used on piece cells
        }
        return true;
    }

    public boolean tryNudge(ActivePiece p, int dx, int dy) {
        p.moveBy(dx, dy);
        if (canPlace(p)) return true;
        p.moveBy(-dx, -dy);
        return false;
    }

    public void lock(ActivePiece p, Color color) {
        for (Vec c : p.worldCells()) grid[c.y()][c.x()] = color;              // ENHANCED FOR
    }

    /** Clears full lines and returns count. */
    public int clearLines() {
        int write = HEIGHT - 1, cleared = 0;
        for (int y = HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < WIDTH; x++) if (grid[y][x] == null) { full = false; break; }
            if (!full) { if (write != y) System.arraycopy(grid[y], 0, grid[write], 0, WIDTH); write--; }
            else cleared++;
        }
        for (int y = write; y >= 0; y--) for (int x = 0; x < WIDTH; x++) grid[y][x] = null;
        return cleared;
    }
}
