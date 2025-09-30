package tetris.dto;

// dto/PureGame.java
public class PureGame {
    public int width, height;
    public int[][] cells;        // board grid
    public int[][] currentShape; // active tetromino (matrix)
    public int[][] nextShape;    // next tetromino (matrix)
}