package tetris.view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import tetris.config.ConfigService;
import tetris.config.TetrisConfig;
import tetris.model.PlayerType;
import tetris.controller.GamePane;
import tetris.controller.PlayerFactory;
import java.net.URL;

public class TwoPlayerBoard extends Application {

    private final String externalHost = "localhost";
    private final int    externalPort = 3000;
    private MediaPlayer musicPlayer;
    private MediaPlayer beepPlayer;
    private boolean leftOver=false, rightOver=false;
    private boolean leftSaved=false, rightSaved=false;

    @Override
    public void start(Stage stage) {
        TetrisConfig cfg = ConfigService.load();
        GamePane left  = new GamePane();
        GamePane right = new GamePane();
        long seed = System.currentTimeMillis();
        left.setSeed(seed);
        right.setSeed(seed);
        configureSide(left,  cfg.getPlayer1Type(), stage);
        configureSide(right, cfg.getPlayer2Type(), stage);
        final boolean leftHuman  = (cfg.getPlayer1Type() == PlayerType.HUMAN);
        final boolean rightHuman = (cfg.getPlayer2Type() == PlayerType.HUMAN);

        Button back = new Button("Back");
        back.setOnAction(e -> {
            left.pauseForMenu();
            right.pauseForMenu();

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(stage);
            alert.setTitle("Leave Game?");
            alert.setHeaderText("Exit to Main Menu");
            alert.setContentText("Your current two-player game will be lost. Are you sure?");

            ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
            ButtonType no  = new ButtonType("No",  ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yes, no);

            alert.showAndWait().ifPresent(response -> {
                if (response == yes) {
                    left.dispose(); right.dispose();
                    try {
                        stage.setWidth(UIConfigurations.WINDOW_WIDTH);
                        stage.setHeight(UIConfigurations.WINDOW_HEIGHT);
                        stage.centerOnScreen();
                        stopAndDisposeMusic();
                        resetStageForMenu(stage);
                        new MainMenu().start(stage);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    left.resumeFromMenu();
                    right.resumeFromMenu();
                    javafx.application.Platform.runLater(() -> stage.getScene().getRoot().requestFocus());
                }
            });
        });

        HBox boards = new HBox(16, left, right);
        boards.setPadding(new Insets(12));
        boards.setAlignment(Pos.CENTER);

        HBox bottomBar = new HBox(back);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(12, 12, 16, 12));

        BorderPane root = new BorderPane();
        root.setCenter(boards);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Tetris — Two Player");
        back.setFocusTraversable(false);
        root.setFocusTraversable(true);

        root.applyCss();
        root.layout();
        stage.sizeToScene();
        stage.centerOnScreen();
        if (cfg.isMusic()) {
            initMusicPlayerIfNeeded();
            if (musicPlayer != null) {
                musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                musicPlayer.play();
            }
        }
        stage.show();
        javafx.application.Platform.runLater(root::requestFocus);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case A -> { if (leftHuman)  left.tryMoveLeft(); }
                case D -> { if (leftHuman)  left.tryMoveRight(); }
                case W -> { if (leftHuman)  left.tryRotate(); }
                case S -> { if (leftHuman)  left.boost(true); }
                case LEFT  -> { if (rightHuman) right.tryMoveLeft(); }
                case RIGHT -> { if (rightHuman) right.tryMoveRight(); }
                case UP    -> { if (rightHuman) right.tryRotate(); }
                case DOWN  -> { if (rightHuman) right.boost(true); }

                case P -> { left.pauseToggle(); right.pauseToggle(); }
                case M -> toggleMusic();
                case N -> toggleSound();
            }
        });
        scene.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case S    -> { if (leftHuman)  left.boost(false); }
                case DOWN -> { if (rightHuman) right.boost(false); }
            }
        });

        stage.setOnCloseRequest(ev -> { left.dispose(); right.dispose(); stopAndDisposeMusic(); });

        GamePane.GameOverWatcher watcher = new GamePane.GameOverWatcher() {
            @Override public void onHighScoreDialogShown(GamePane who) {
                if (who == left) right.pause(); else left.pause();
                if (musicPlayer != null) { try { musicPlayer.pause(); } catch (Exception ignore) {} }
            }

            @Override public void onHighScoreDialogClosed(GamePane who) {
                if (who == left) right.resume(); else left.resume();
                if (TetrisConfig.getInstance().isMusic() && musicPlayer != null) {
                    try { musicPlayer.play(); } catch (Exception ignore) {}
                }
            }

            @Override public void onGameOverReached(GamePane who) {
                if (who == left) leftOver = true; else rightOver = true;
                maybeShowHighScores(stage);
            }

            @Override public void onScoreSaved(GamePane who) {
                if (who == left) leftSaved = true; else rightSaved = true;
                maybeShowHighScores(stage);
            }

            private void maybeShowHighScores(Stage stage) {
                if (leftOver && rightOver && leftSaved && rightSaved) {
                    stopAndDisposeMusic();
                    resetStageForMenu(stage);
                    try { new HighScore().start(stage); }
                    catch (Exception e) { e.printStackTrace(); }
                }
            }
        };

        left.setGameOverWatcher(watcher);
        right.setGameOverWatcher(watcher);


        left.setGameOverWatcher(watcher);
        right.setGameOverWatcher(watcher);

        left.startGame();
        right.startGame();
    }

    private void configureSide(GamePane pane, PlayerType type, Stage stage) {
        try {
            PlayerFactory.configureForType(pane, type, externalHost, externalPort);
            if (type == PlayerType.AI) pane.boost(true);
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(stage);
            alert.setTitle("Player Setup");
            alert.setHeaderText("Could not enable " + type + " for this side");
            alert.setContentText(ex.getClass().getSimpleName() +
                    (ex.getMessage() != null ? (": " + ex.getMessage()) : ""));
            alert.showAndWait();
        }
    }

    private void toggleMusic() {
        TetrisConfig config = TetrisConfig.getInstance();
        boolean newVal = !config.isMusic();
        config.setMusic(newVal);
        ConfigService.save(config);
        if (newVal) {
            initMusicPlayerIfNeeded();
            if (musicPlayer != null) {
                musicPlayer.setOnReady(() -> musicPlayer.play());
                try { musicPlayer.play(); } catch (Exception ignore) { }
            }
        } else {
            if (musicPlayer != null) musicPlayer.pause();
        }
    }

    private void toggleSound() {
        TetrisConfig config = TetrisConfig.getInstance();
        boolean newVal = !config.isSoundEffect();
        config.setSoundEffect(newVal);
        ConfigService.save(config);
    }

    private void initMusicPlayerIfNeeded() {
        if (musicPlayer != null) return;
        URL musicUrl = getClass().getResource("/sounds/background.mp3");
        if (musicUrl == null) {
            System.err.println("background.mp3 not found under /sounds");
            return;
        }
        Media bg = new Media(musicUrl.toExternalForm());
        musicPlayer = new MediaPlayer(bg);
        musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
    }

    private void stopAndDisposeMusic() {
        if (musicPlayer != null) {
            try { musicPlayer.stop(); } catch (Exception ignored) {}
            try { musicPlayer.dispose(); } catch (Exception ignored) {}
            musicPlayer = null;
        }
    }

    private void resetStageForMenu(Stage stage) {
        stage.setMaximized(false);
        stage.setFullScreen(false);
        stage.setMinWidth(UIConfigurations.WINDOW_WIDTH);
        stage.setMinHeight(UIConfigurations.WINDOW_HEIGHT);
        stage.setWidth(UIConfigurations.WINDOW_WIDTH);
        stage.setHeight(UIConfigurations.WINDOW_HEIGHT);
        stage.centerOnScreen();
    }

    public static void main(String[] args) { launch(args); }
}
