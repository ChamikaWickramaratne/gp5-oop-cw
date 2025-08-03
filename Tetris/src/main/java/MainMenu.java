import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainMenu extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Button width
        double buttonWidth = 200;

        // Create buttons
        Button playButton = new Button("Play");
        Button highScoreButton = new Button("High Score");
        Button configButton = new Button("Configurations");
        Button exitButton = new Button("Exit");

        // Set uniform width & style
        Button[] buttons = { playButton, highScoreButton, configButton, exitButton };
        for (Button btn : buttons) {
            btn.setPrefWidth(buttonWidth);
            btn.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");
        }

        // Button actions
        highScoreButton.setOnAction(e -> {
            HighScore highScoreView = new HighScore();
            try {
                highScoreView.start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        playButton.setOnAction(e -> {
            System.out.println("Play button clicked (feature to be implemented)");
        });

        configButton.setOnAction(e -> {
            ConfigScreen configView = new ConfigScreen();
            try {
                configView.start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        exitButton.setOnAction(e -> {
            primaryStage.close();
            System.exit(0);
        });

        // Layout
        VBox root = new VBox(15, playButton, highScoreButton, configButton, exitButton);
        root.setStyle("-fx-background-color: white; -fx-padding: 30;");
        root.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(root, 400, 350);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Tetris Main Menu");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
