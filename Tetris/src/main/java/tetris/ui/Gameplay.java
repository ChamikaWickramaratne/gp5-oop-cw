package tetris.ui;

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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import tetris.model.Board;
import tetris.model.Vec;
import tetris.model.piece.ActivePiece;
import tetris.controller.GameController;
import tetris.ui.ConfigScreen;

public class Gameplay extends Application {

    //constants
    private static final int CELL_SIZE = 20;
    private final int WIDTH = 10;
    private final int HEIGHT = 20;
    private Label scoreLabel;
    private AnimationTimer timer;
    private GameController controller = new GameController();

    @Override
    public void start(Stage stage) {
        controller = new GameController();
        controller.setAiEnabled(ConfigScreen.AI_ENABLED);

        // UI
        scoreLabel = new Label();
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox topBar = new HBox(scoreLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        Canvas boardCanvas = new Canvas(
                WIDTH * CELL_SIZE,
                HEIGHT * CELL_SIZE
        );
        boardCanvas.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            if (controller.isGameOver()) {
                if (timer != null) timer.stop();
                controller.stop();
                try {
                    new MainMenu().start(stage);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                boolean wasPaused = controller.isPaused();
                controller.pauseGame();
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.initOwner(stage);
                alert.setTitle("Leave Game?");
                alert.setHeaderText("Exit to Main Menu");
                alert.setContentText("Your current game will be lost. Are you sure?");
                ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
                ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(yes, no);
                alert.showAndWait().ifPresent(response -> {
                    if (response == yes) {
                        if (timer != null) timer.stop();
                        controller.stop();
                        try {
                            new MainMenu().start(stage);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        if (!wasPaused) controller.pauseGame();
                    }
                });
            }
        });

        HBox backBar = new HBox(backButton);
        backBar.setAlignment(Pos.CENTER);
        backBar.setPadding(new Insets(10));

        Label authorLabel = new Label("Version : v2.0.0");
        HBox authorBar = new HBox(authorLabel);
        authorBar.setAlignment(Pos.CENTER);
        authorBar.setPadding(new Insets(5));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(boardCanvas);
        root.setBottom(new VBox(backBar, authorBar));
        root.setStyle("-fx-background-color: #f9f9f9;");
        Scene scene = new Scene(root, UIConfigurations.WINDOW_WIDTH, UIConfigurations.WINDOW_HEIGHT);
        stage.setScene(scene);

        //controls
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case A -> controller.tryMoveLeft();
                case D -> controller.tryMoveRight();
                case S -> controller.boost(true);
                case P -> controller.pauseGame();
                case W, UP -> controller.tryRotate();

            }
        });
        //stop boost when button released
        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.S) controller.boost(false);
        });

        stage.setTitle("Tetris");
        stage.setMinWidth(500);
        stage.show();

        controller.resetGameState();
        controller.spawnNewPiece();

        //gameplay timer.
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                controller.tick(now);
                draw(boardCanvas.getGraphicsContext2D());
            }
        };
        timer.start();
    }

    //drawing the board each time
    private void draw(GraphicsContext gc) {
        int W = Board.WIDTH, H = Board.HEIGHT;
        gc.clearRect(0, 0, W * CELL_SIZE, H * CELL_SIZE);
        gc.setStroke(Color.LIGHTGRAY);

        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) gc.strokeRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

        Board board = controller.getBoard();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                Color cell = board.cells()[y][x];
                if (cell != null) {
                    double px = x * CELL_SIZE, py = y * CELL_SIZE;
                    gc.setFill(cell);
                    gc.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    gc.setStroke(Color.BLACK);
                    gc.strokeRect(px, py, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        ActivePiece current = controller.getCurrentPiece();
        if (current != null) {
            Color activeColor = controller.getCurrentColor();
            if (activeColor == null) activeColor = Color.DEEPSKYBLUE;

            for (Vec c : current.worldCells()) {
                double px = c.x() * CELL_SIZE, py = c.y() * CELL_SIZE;
                gc.setFill(activeColor);
                gc.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                gc.setStroke(Color.BLACK);
                gc.strokeRect(px, py, CELL_SIZE, CELL_SIZE);
            }
        }
        scoreLabel.setText("Score: " + controller.getScore());
        if (controller.isPaused() || controller.isGameOver()) {
            double w = W * CELL_SIZE, h = H * CELL_SIZE;
            gc.save();
            gc.setGlobalAlpha(0.45);
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, w, h);
            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            String title = controller.isGameOver() ? "Game Over" : "PAUSED";
            String hint = controller.isGameOver() ? "Press Back to return" : "Press 'P' to resume";
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            gc.fillText(title, w / 2.0, h / 2.0 - 18);
            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
            gc.fillText(hint, w / 2.0, h / 2.0 + 16);
            gc.restore();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
