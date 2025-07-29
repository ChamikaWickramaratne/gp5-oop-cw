import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create splash stage
        Stage splashStage = new Stage(StageStyle.UNDECORATED);

        // Load splash image
        URL imageUrl = getClass().getClassLoader().getResource("pic2.png");
        if (imageUrl == null) {
            throw new IllegalStateException("❌ pic2.png not found in resources!");
        }

        ImageView splashImage = new ImageView(new Image(imageUrl.toExternalForm()));
        splashImage.setPreserveRatio(false); // Stretch to fit
        splashImage.setFitWidth(400);        // Match scene size
        splashImage.setFitHeight(400);

        StackPane splashLayout = new StackPane(splashImage);
        Scene splashScene = new Scene(splashLayout, 400, 400);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        splashStage.show();

        // Simulated loading for 8 seconds
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(8000);  // ⏱ 8 seconds
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    splashStage.close();
                    showMainStage(primaryStage);
                });
            }
        };

        new Thread(loadTask).start();
    }

    // Replace this with your actual main menu code
    private void showMainStage(Stage primaryStage) {
        StackPane layout = new StackPane();
        layout.getChildren().add(new javafx.scene.control.Label("Welcome to Tetris Main Menu!"));
        Scene mainScene = new Scene(layout, 600, 400);
        primaryStage.setTitle("Tetris");
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
