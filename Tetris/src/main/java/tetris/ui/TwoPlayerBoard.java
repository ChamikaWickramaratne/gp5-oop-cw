// src/main/java/tetris/ui/TwoPlayerBoard.java
package tetris.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TwoPlayerBoard extends Application {

    // Configure players
    private final boolean leftExternal  = false;
    private final boolean rightExternal = false;
    private final boolean rightAI       = true;

    private final String  externalHost  = "127.0.0.1"; // prefer 127.0.0.1 to avoid IPv6 issues
    private final int     externalPort  = 3000;

    @Override
    public void start(Stage stage) {
        GamePane left  = new GamePane();
        GamePane right = new GamePane();

        long seed = System.currentTimeMillis();
        left.setSeed(seed);
        right.setSeed(seed);

        // Enable exactly one “brain” per side *before* starting
        if (leftExternal)  left.enableExternal(externalHost, externalPort);
        // Example enabling AI:
        if (rightAI)       right.enableAI(new tetris.ai.BetterHeuristic());
        if (rightExternal) right.enableExternal(externalHost, externalPort);

        // --- UI layout (unchanged) ---
        Button back = new Button("Back");
        back.setOnAction(e -> {
            left.dispose(); right.dispose();
            try { new MainMenu().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });
        HBox topBar = new HBox(back);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(8));

        HBox boards = new HBox(16, left, right);
        boards.setPadding(new Insets(12));
        boards.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(boards);

        Scene scene = new Scene(root, UIConfigurations.WINDOW_WIDTH * 2 + 80, UIConfigurations.WINDOW_HEIGHT + 40);
        stage.setScene(scene);
        stage.setTitle("Tetris — Two Player");
        back.setFocusTraversable(false);
        root.setFocusTraversable(true);
        stage.show();
        javafx.application.Platform.runLater(root::requestFocus);

        // Key mapping
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case A -> left.tryMoveLeft();
                case D -> left.tryMoveRight();
                case W -> left.tryRotate();
                case S -> left.boost(true);
                case P -> { left.pauseToggle(); right.pauseToggle(); }

                case LEFT  -> right.tryMoveLeft();
                case RIGHT -> right.tryMoveRight();
                case UP    -> right.tryRotate();
                case DOWN  -> right.boost(true);
            }
        });
        scene.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case S    -> left.boost(false);
                case DOWN -> right.boost(false);
            }
        });

        stage.setOnCloseRequest(ev -> { left.dispose(); right.dispose(); });

        // *** Start after enabling brains so piece #1 is controlled ***
        left.startGame();
        right.startGame();
    }

    public static void main(String[] args) { launch(args); }
}
