package tetris.model.ai;

public interface Heuristic {
    double evaluate(int[][] board, int linesCleared);
}
