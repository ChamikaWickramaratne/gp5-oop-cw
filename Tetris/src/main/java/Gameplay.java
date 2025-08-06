import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Gameplay extends Application {

    private static final int CELL_SIZE    = 20;
    private static final int BOARD_WIDTH  = 10;
    private static final int BOARD_HEIGHT = 20;

    private final int[][] shape = {
            {0,0}, {1,0}, {2,0}, {2,1}    // L shape need to add the other shapes
    };

    private double shapeY = 0;
    private long   lastDropTime = 0;

    @Override
    public void start(Stage stage) {
        Label titleLabel = new Label("Play");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox topBar = new HBox(titleLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        Canvas boardCanvas = new Canvas(
                BOARD_WIDTH * CELL_SIZE,
                BOARD_HEIGHT * CELL_SIZE
        );
        boardCanvas.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> stage.close());

        backButton.setOnAction(e -> {
            MainMenu mainView = new MainMenu();
            try {
                mainView.start(stage);  // Navigate back to Main menu
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

        stage.setScene(new Scene(root));
        stage.setTitle("Tetris");
        stage.setMinWidth(500);
        stage.show();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastDropTime == 0) {
                    lastDropTime = now;
                } else if (now - lastDropTime > 1_000_000_000) { //falling speed. currentlty 1 second
                    shapeY += CELL_SIZE;
                    lastDropTime = now;
                    if (shapeY > BOARD_HEIGHT * CELL_SIZE) {    //reset fall
                        shapeY = 0;
                    }
                }
                drawFrame(boardCanvas.getGraphicsContext2D());
            }
        };
        timer.start();
    }

    private void drawFrame(GraphicsContext gc) {
        // clear
        gc.clearRect(0, 0,
                BOARD_WIDTH * CELL_SIZE,
                BOARD_HEIGHT * CELL_SIZE);

        gc.setStroke(Color.GRAY);
        gc.setLineWidth(2);
        gc.strokeRect(
                0,
                0,
                BOARD_WIDTH * CELL_SIZE,
                BOARD_HEIGHT * CELL_SIZE
        );

        gc.setFill(Color.HOTPINK);
        for (int[] block : shape) {
            int bx = block[0], by = block[1];
            double px = bx * CELL_SIZE;
            double py = shapeY + by * CELL_SIZE;
            gc.fillRect(px, py, CELL_SIZE, CELL_SIZE);
            gc.setStroke(Color.BLACK);
            gc.strokeRect(px, py, CELL_SIZE, CELL_SIZE);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
