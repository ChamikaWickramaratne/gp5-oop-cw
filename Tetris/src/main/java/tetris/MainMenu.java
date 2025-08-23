package tetris;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

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

        // Gameplay actions
        playButton.setOnAction(e -> {
            Gameplay gameplayView = new Gameplay();
            try {
                gameplayView.start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(primaryStage);
            alert.setTitle("Confirm Exit");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to exit?");

            ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
            ButtonType no  = new ButtonType("No",  ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yes, no);

            alert.showAndWait().ifPresent(response -> {
                if (response == yes) {
                    Platform.exit();   // graceful shutdown
                }
                // else: do nothing (return to main screen)
            });
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
