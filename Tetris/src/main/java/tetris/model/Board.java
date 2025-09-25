package tetris.model;

import javafx.scene.paint.Color;
import tetris.model.piece.ActivePiece;

public class Board {
    public static final int CELL = 20;   // keep cell size constant for drawing

    private final int width;
    private final int height;
    private final Color[][] grid;

    // Default (10x20) for backwards compatibility
    public Board() { this(10, 20); }

    // Configurable constructor
    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Color[height][width];
    }

    public int width()  { return width; }
    public int height() { return height; }
    public Color[][] cells() { return grid; }

    public boolean inside(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean occupied(int x, int y) { return grid[y][x] != null; }

    public boolean canPlace(ActivePiece p) {
        for (Vec c : p.worldCells()) {
            if (!inside(c.x(), c.y()) || occupied(c.x(), c.y())) return false;
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
        for (Vec c : p.worldCells()) grid[c.y()][c.x()] = color;
    }

    public int clearLines() {
        int write = height - 1, cleared = 0;
        for (int y = height - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < width; x++) {
                if (grid[y][x] == null) { full = false; break; }
            }
            if (!full) {
                if (write != y) System.arraycopy(grid[y], 0, grid[write], 0, width);
                write--;
            } else {
                cleared++;
            }
        }
        for (int y = write; y >= 0; y--) {
            for (int x = 0; x < width; x++) grid[y][x] = null;
        }
        return cleared;
    }
}
