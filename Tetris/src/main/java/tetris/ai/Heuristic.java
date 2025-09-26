// tetris/ai/Heuristic.java
package tetris.ai;

public interface Heuristic {
    // Higher is better
    double evaluate(int[][] board, int linesCleared);
}
