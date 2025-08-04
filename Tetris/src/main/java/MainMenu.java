import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainMenu extends Application {

    @Override
    public void start(Stage primaryStage) {
        double buttonWidth = 200;

        // Create buttons
        Button playButton = new Button("Play");
        Button highScoreButton = new Button("High Score");
        Button configButton = new Button("Configurations");
        Button exitButton = new Button("Exit");

        // Style buttons
        Button[] buttons = { playButton, highScoreButton, configButton, exitButton };
        for (Button btn : buttons) {
            btn.setPrefWidth(buttonWidth);
            btn.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");
        }

        // Play button launches a mock game screen
        playButton.setOnAction(e -> {
            primaryStage.close();
            Stage gameStage = new Stage();
            StackPane gameLayout = new StackPane(new Label("🎮 Game starts here..."));
            Scene gameScene = new Scene(gameLayout, 600, 400);
            gameStage.setScene(gameScene);
            gameStage.setTitle("Tetris Game");
            gameStage.show();
        });

        // High Score
        highScoreButton.setOnAction(e -> {
            try {
                HighScore highScoreView = new HighScore();
                highScoreView.start(primaryStage);
            } catch (Exception ex) {
                System.err.println("⚠️ HighScore screen not available.");
                ex.printStackTrace();
            }
        });

        // Configurations
        configButton.setOnAction(e -> {
            try {
                ConfigScreen configView = new ConfigScreen();
                configView.start(primaryStage);
            } catch (Exception ex) {
                System.err.println("⚠️ ConfigScreen not available.");
                ex.printStackTrace();
            }
        });

        // Exit button
        exitButton.setOnAction(e -> {
            primaryStage.close();
            Platform.exit();
        });

        // Layout
        VBox root = new VBox(15, playButton, highScoreButton, configButton, exitButton);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-padding: 30;");

        Scene scene = new Scene(root, 400, 350);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Tetris Main Menu");
        primaryStage.show();
    }
}
