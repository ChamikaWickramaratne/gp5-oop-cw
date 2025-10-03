package tetris.model;

import javafx.scene.paint.Color;
import tetris.model.piece.ActivePiece;

public class Board {
    private final int w;
    private final int h;
    private final Color[][] grid;

    public Board(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid board size: " + width + "x" + height);
        }
        this.w = width;
        this.h = height;
        this.grid = new Color[h][w];
    }

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

    public boolean tryNudge(ActivePiece p, int dx, int dy) {
        p.moveBy(dx, dy);
        if (canPlace(p)) return true;
        p.moveBy(-dx, -dy);
        return false;
    }

    public void lock(ActivePiece p, Color color) {
        for (Vec c : p.worldCells()) {
            if (inside(c.x(), c.y())) {
                grid[c.y()][c.x()] = color;
            }
        }
    }

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
                    for (int x = 0; x < w; x++) grid[write][x] = grid[y][x];
                }
                write--;
            } else {
                cleared++;
            }
        }

        for (int y = write; y >= 0; y--) {
            for (int x = 0; x < w; x++) grid[y][x] = null;
        }
        return cleared;
    }
}
