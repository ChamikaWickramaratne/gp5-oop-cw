package tetris.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tetris.config.TetrisConfig;
import tetris.controller.GameplayController;

public class MainMenu extends Application {

    @Override
    public void start(Stage primaryStage) {
        double buttonWidth = 200;
        primaryStage.setMaximized(false);
        primaryStage.setFullScreen(false);
        primaryStage.setMinWidth(UIConfigurations.WINDOW_WIDTH);
        primaryStage.setMinHeight(UIConfigurations.WINDOW_HEIGHT);
        primaryStage.setWidth(UIConfigurations.WINDOW_WIDTH);
        primaryStage.setHeight(UIConfigurations.WINDOW_HEIGHT);
        primaryStage.centerOnScreen();

        Button startButton     = new Button("Start");
        Button highScoreButton = new Button("High Score");
        Button configButton    = new Button("Configurations");
        Button exitButton      = new Button("Exit");

        Button[] buttons = { startButton, highScoreButton, configButton, exitButton };
        for (Button btn : buttons) {
            btn.setPrefWidth(buttonWidth);
            btn.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");
        }

        startButton.setOnAction(e -> {
            try {
                TetrisConfig cfg = TetrisConfig.getInstance();
                boolean extend = cfg.isExtendMode();
                if (extend) {
                    new TwoPlayerBoard().start(primaryStage);
                } else {
                    GameplayController controller = new GameplayController();
                    controller.start(primaryStage);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        highScoreButton.setOnAction(e -> {
            try { new HighScore().start(primaryStage); }
            catch (Exception ex) { ex.printStackTrace(); }
        });

        configButton.setOnAction(e -> {
            try { new ConfigScreen().start(primaryStage); }
            catch (Exception ex) { ex.printStackTrace(); }
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
                if (response == yes) Platform.exit();
            });
        });

        VBox root = new VBox(15, startButton, highScoreButton, configButton, exitButton);
        root.setStyle("-fx-background-color: white; -fx-padding: 30;");
        root.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(root, UIConfigurations.WINDOW_HEIGHT, UIConfigurations.WINDOW_WIDTH);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Tetris Main Menu");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
