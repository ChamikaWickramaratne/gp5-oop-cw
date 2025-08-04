import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Pos;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.net.URL;

public class SplashScreen extends Application {

    @Override
    public void start(Stage splashStage) {
        splashStage.initStyle(StageStyle.UNDECORATED);

        // Load splash image
        URL imageUrl = getClass().getClassLoader().getResource("pic3.png");
        if (imageUrl == null) {
            throw new IllegalStateException("❌ pic3.png not found in resources!");
        }

        ImageView splashImage = new ImageView(new Image(imageUrl.toExternalForm()));
        splashImage.setPreserveRatio(false);
        splashImage.setFitWidth(400);
        splashImage.setFitHeight(350);

        // Create animated loading label
        Label loadingLabel = new Label("Loading");
        loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
        animateLoadingText(loadingLabel);

        VBox content = new VBox(splashImage, loadingLabel);
        content.setAlignment(Pos.CENTER);
        content.setSpacing(10);

        StackPane splashLayout = new StackPane(content);
        splashLayout.setStyle("-fx-background-color: black;");

        Scene splashScene = new Scene(splashLayout, 400, 400);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        splashStage.show();

        // Delay then load MainMenu
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(4000); // 4 seconds delay
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    splashStage.close();
                    try {
                        new MainMenu().start(new Stage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        };

        new Thread(loadTask).start();
    }

    private void animateLoadingText(Label label) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e -> label.setText("Loading.")),
                new KeyFrame(Duration.seconds(1.0), e -> label.setText("Loading..")),
                new KeyFrame(Duration.seconds(1.5), e -> label.setText("Loading...")),
                new KeyFrame(Duration.seconds(2.0), e -> label.setText("Loading"))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public static void main(String[] args) {
        launch(args); // Entry point
    }
}
