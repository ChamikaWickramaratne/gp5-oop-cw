// src/main/java/tetris/view/GameplayView.java
package tetris.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class SinglePlayerView {

    // Visual constants (only view concerns)
    public static final int CELL_SIZE = 20;

    // Root + major nodes
    private final BorderPane root = new BorderPane();
    private final Label scoreLabel = new Label("Score: 0");
    private final Label playerTypeLabel = new Label("Player: —");
    private final Label levelLabel = new Label("Level: —");
    private final Label linesLabel = new Label("Lines: 0");
    private final Canvas boardCanvas;
    private final Canvas nextCanvas = new Canvas(6 * CELL_SIZE, 6 * CELL_SIZE);
    private final Button backButton = new Button("Back");
    private final Label footerLabel = new Label("Version : v2.0.0");

    public SinglePlayerView(int boardWidth, int boardHeight) {
        // Top bar
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox topBar = new HBox(scoreLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        // Center: board
        boardCanvas = new Canvas(boardWidth * CELL_SIZE, boardHeight * CELL_SIZE);
        boardCanvas.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");

        // Right: sidebar
        playerTypeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        levelLabel.setStyle("-fx-font-size: 13px;");
        linesLabel.setStyle("-fx-font-size: 13px;");

        VBox infoBox = new VBox(6,
                new Label("Info"),
                playerTypeLabel,
                levelLabel,
                linesLabel
        );
        infoBox.setAlignment(Pos.TOP_CENTER);
        infoBox.setPadding(new Insets(10));
        infoBox.setSpacing(6);
        infoBox.setStyle("""
            -fx-background-color: #f4f4f4;
            -fx-border-color: #888;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);
        """);

        VBox rightBar = new VBox(
                new Label("Next"),
                nextCanvas,
                new Separator(),
                infoBox
        );
        rightBar.setAlignment(Pos.TOP_CENTER);
        rightBar.setSpacing(12);
        rightBar.setPadding(new Insets(10));
        rightBar.setStyle("-fx-background-color: #fafafa;");

        // Bottom: back + footer
        HBox backBar = new HBox(backButton);
        backBar.setAlignment(Pos.CENTER);
        backBar.setPadding(new Insets(10));

        HBox footer = new HBox(footerLabel);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(5));

        VBox bottom = new VBox(backBar, footer);

        // Root
        root.setTop(topBar);
        root.setCenter(boardCanvas);
        root.setRight(rightBar);
        root.setBottom(bottom);
        root.setStyle("-fx-background-color: #f9f9f9;");
    }

    // ---- Scene/Stage helpers
    public Scene makeScene(int pxWidth, int pxHeight) {
        return new Scene(root, pxWidth, pxHeight);
    }

    public void attachTo(Stage stage, String title, int pxWidth, int pxHeight) {
        stage.setTitle(title);

        // Do NOT lock the stage's min size here (that "sticks" across screens)
        stage.setMaximized(false);
        stage.setFullScreen(false);

        // Set the scene and let the stage size to it
        stage.setScene(makeScene(pxWidth, pxHeight));
        stage.sizeToScene();        // fit window to this scene's preferred size
        stage.centerOnScreen();
        stage.show();
    }


    // ---- Getters the controller needs
    public Canvas getBoardCanvas() { return boardCanvas; }
    public GraphicsContext getBoardGC() { return boardCanvas.getGraphicsContext2D(); }
    public Canvas getNextCanvas() { return nextCanvas; }

    public Button getBackButton() { return backButton; }

    // ---- Small view updates
    public void setScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    public void setPlayerTypeText(String text) {
        playerTypeLabel.setText("Player: " + text);
    }

    public void setLevel(int level) {
        levelLabel.setText("Level: " + level);
    }

    public void setLines(int lines) {
        linesLabel.setText("Lines: " + lines);
    }

    // A generic overlay drawer (controller decides *when* to call)
    public void drawOverlay(GraphicsContext gc, double widthPx, double heightPx, String title, String hint) {
        gc.save();
        gc.setGlobalAlpha(0.45);
        gc.setFill(javafx.scene.paint.Color.BLACK);
        gc.fillRect(0, 0, widthPx, heightPx);

        gc.setGlobalAlpha(1.0);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 36));
        gc.fillText(title, widthPx / 2.0, heightPx / 2.0 - 18);

        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.NORMAL, 18));
        gc.fillText(hint,  widthPx / 2.0, heightPx / 2.0 + 16);
        gc.restore();
    }
}
