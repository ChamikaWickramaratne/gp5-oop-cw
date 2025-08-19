import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.util.Random;

public class Gameplay extends Application {

    private static final int CELL_SIZE    = 20;
    private static final int BOARD_WIDTH  = 10;
    private static final int BOARD_HEIGHT = 20;

    private static final int[][][] SHAPES = {
            // I
            { {0,0}, {1,0}, {2,0}, {3,0} },
            // O
            { {0,0}, {1,0}, {0,1}, {1,1} },
            // T
            { {0,0}, {1,0}, {2,0}, {1,1} },
            // L
            { {0,0}, {1,0}, {2,0}, {2,1} },
            // Inverse L
            { {0,0}, {1,0}, {2,0}, {0,1} },
            // S
            { {1,0}, {2,0}, {0,1}, {1,1} },
            // Z
            { {0,0}, {1,0}, {1,1}, {2,1} }
    };

    private final Random rng = new Random();


    private int[][] currentShape = SHAPES[0];
    private int shapeCol;
    private int shapeRow;

    private long lastDropTime = 0;
    private static long dropSpeed = 1_000_000_000;

    private int[][] board = new int[BOARD_HEIGHT][BOARD_WIDTH];

    private static boolean paused = false;
    private static boolean gameOver = false;

    private static int score = 0;

    private static Label scoreLabel;

    @Override
    public void start(Stage stage) {
        scoreLabel = new Label("Score: "+ score);
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox topBar = new HBox(scoreLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        Canvas boardCanvas = new Canvas(
                BOARD_WIDTH * CELL_SIZE,
                BOARD_HEIGHT * CELL_SIZE
        );
        boardCanvas.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            try {
                new MainMenu().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox backBar = new HBox(backButton);
        backBar.setAlignment(Pos.CENTER);
        backBar.setPadding(new Insets(10));

        Label authorLabel = new Label("Author: Chamika Wickramarathne");
        HBox authorBar = new HBox(authorLabel);
        authorBar.setAlignment(Pos.CENTER);
        authorBar.setPadding(new Insets(5));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(boardCanvas);
        root.setBottom(new VBox(backBar, authorBar));
        root.setStyle("-fx-background-color: #f9f9f9;");
        Scene scene = new Scene(root);
        stage.setScene(scene);
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case A -> moveLeft();
                case D -> moveRight();
                case S -> boost(true);
                case P -> pauseGame();
                case W -> rotateShape();
            }
        });
        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.S) {
                boost(false);// back to normal speed
            }
        });
        stage.setTitle("Tetris");
        stage.setMinWidth(500);
        stage.show();

        // Spawn the first random shape at the top
        spawnNewShape();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!paused && !gameOver) {
                    if (lastDropTime == 0) {
                        lastDropTime = now;
                    } else if (now - lastDropTime > dropSpeed) {
                        if (willCollideDown()) {
                            lockPiece();
                        } else {
                            shapeRow += 1;
                        }
                        lastDropTime = now;
                    }
                }
                // Always render (even when paused)
                drawFrame(boardCanvas.getGraphicsContext2D());
            }
        };
        timer.start();
    }

    private void spawnNewShape() {
        currentShape = SHAPES[rng.nextInt(SHAPES.length)];
        shapeRow = 0;

        // Center horizontally based on the current shape's width
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        for (int[] b : currentShape) {
            minX = Math.min(minX, b[0]);
            maxX = Math.max(maxX, b[0]);
        }
        int shapeWidth = (maxX - minX + 1);
        shapeCol = Math.max(0, (BOARD_WIDTH - shapeWidth) / 2 - minX);

        if (gameOverCheck()) {
            gameOver = true;
        }
    }

    private boolean willCollideDown() {
        for (int[] b : currentShape) {
            int nextRow = shapeRow + b[1] + 1;
            int col     = shapeCol + b[0];
            if (nextRow >= BOARD_HEIGHT) return true;
            if (board[nextRow][col] == 1) return true;
        }
        return false;
    }

    private void drawFrame(GraphicsContext gc) {
        gc.clearRect(0, 0, BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);

        //Draw grid
        gc.setStroke(Color.LIGHTGRAY);
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                double px = x * CELL_SIZE;
                double py = y * CELL_SIZE;
                gc.strokeRect(px, py, CELL_SIZE, CELL_SIZE);
            }
        }

        //Draw the blocked that hit bottom
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (board[y][x] == 1) {
                    double px = x * CELL_SIZE;
                    double py = y * CELL_SIZE;
                    gc.setFill(Color.GRAY);
                    gc.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    gc.setStroke(Color.BLACK);
                    gc.strokeRect(px, py, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // draw current falling piece
        gc.setFill(Color.BLUE);
        for (int[] block : currentShape) {
            int bx = shapeCol + block[0];
            int by = shapeRow + block[1];
            double px = bx * CELL_SIZE;
            double py = by * CELL_SIZE;
            gc.fillRect(px, py, CELL_SIZE, CELL_SIZE);
            gc.setStroke(Color.BLACK);
            gc.strokeRect(px, py, CELL_SIZE, CELL_SIZE);
        }

        //Pause screen overlay when paused
        if (paused) {
            double W = BOARD_WIDTH * CELL_SIZE;
            double H = BOARD_HEIGHT * CELL_SIZE;

            gc.save();
            gc.setGlobalAlpha(0.45);
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, W, H);

            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText("PAUSED", W / 2.0, H / 2.0);
            gc.restore();
        }

        //Game over overlay when game over
        if (gameOver) {
            double W = BOARD_WIDTH * CELL_SIZE;
            double H = BOARD_HEIGHT * CELL_SIZE;

            gc.save();
            gc.setGlobalAlpha(0.45);
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, W, H);

            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText("Game Over", W / 2.0, H / 2.0);
            gc.restore();
        }
    }


    private void lockPiece() {
        for (int[] block : currentShape) {
            int x = shapeCol + block[0];
            int y = shapeRow + block[1];
            if (y >= 0 && y < BOARD_HEIGHT && x >= 0 && x < BOARD_WIDTH) {
                board[y][x] = 1;
            }
        }
        clearFullLines();
        spawnNewShape();
    }

    private void moveLeft() {
        System.out.println("left");
        if (canMove(-1)) {
            shapeCol--;
        }
    }

    private void moveRight() {
        System.out.println("Right");
        if (canMove(1)) {
            shapeCol++;
        }
    }

    private boolean canMove(int dx) {
        for (int[] b : currentShape) {
            int newCol = shapeCol + b[0] + dx;
            int row    = shapeRow + b[1];
            if (newCol < 0 || newCol >= BOARD_WIDTH) {
                return false;
            }
            if (board[row][newCol] == 1) {
                return false;
            }
        }
        return true;
    }

    private void boost(boolean pressed){
        if(pressed)
            dropSpeed = 100_000_000;
        else
            dropSpeed = 1_000_000_000;
    }

    private void pauseGame() {
        paused = !paused;
        if (!paused) {
            lastDropTime = 0;
        }
    }

    private boolean gameOverCheck() {
        for (int[] b : currentShape) {
            int x = shapeCol + b[0];
            int y = shapeRow + b[1];
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT) {
                return true;
            }
            if (board[y][x] == 1) {
                return true;
            }
        }
        return false;
    }

    private int clearFullLines() {
        int writeRow = BOARD_HEIGHT - 1;
        int cleared = 0;

        for (int y = BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (board[y][x] == 0) {
                    full = false;
                    break;
                }
            }

            if (!full) {
                if (writeRow != y) {
                    System.arraycopy(board[y], 0, board[writeRow], 0, BOARD_WIDTH);
                }
                writeRow--;
            } else {
                cleared++;
                score += 100;
                if (scoreLabel != null) {
                    scoreLabel.setText("Score: " + score);
                }
            }
        }

        for (int y = writeRow; y >= 0; y--) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                board[y][x] = 0;
            }
        }
        return cleared;
    }

    private void rotateShape() {
        int[][] rotated = new int[currentShape.length][2];

        for (int i = 0; i < currentShape.length; i++) {
            int x = currentShape[i][0];
            int y = currentShape[i][1];
            rotated[i][0] = y;
            rotated[i][1] = -x;
        }

        if (canRotate(rotated, shapeCol, shapeRow)) {
            currentShape = rotated;
        }
    }

    private boolean canRotate(int[][] shape, int col, int row) {
        for (int[] b : shape) {
            int x = col + b[0];
            int y = row + b[1];
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT) {
                return false;
            }
            if (board[y][x] == 1) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
