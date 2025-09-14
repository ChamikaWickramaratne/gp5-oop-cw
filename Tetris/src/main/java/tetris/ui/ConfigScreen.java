// src/main/java/tetris/ui/ConfigScreen.java
package tetris.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tetris.config.ConfigService;
import tetris.config.TetrisConfig;

public class ConfigScreen extends Application {

    private TetrisConfig config;

    @Override
    public void start(Stage primaryStage) {
        showConfigScreen(primaryStage);
    }

    private void showConfigScreen(Stage stage) {
        // 1) Load existing config (or defaults)
        config = ConfigService.load();

        Label titleLabel = new Label("Configuration");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        // Field Width
        HBox fieldWidthRow = new HBox(10);
        fieldWidthRow.setAlignment(Pos.CENTER_LEFT);
        Label fieldWidthLabel = new Label("Field Width (No of cells):");
        fieldWidthLabel.setMinWidth(100);
        Slider fieldWidthSlider = createSlider(5, 15, config.getFieldWidth());
        Label fieldWidthValue = new Label(Integer.toString(config.getFieldWidth()));
        fieldWidthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int v = newVal.intValue();
            fieldWidthValue.setText(Integer.toString(v));
            config.setFieldWidth(v);
        });
        HBox.setHgrow(fieldWidthSlider, Priority.ALWAYS);
        fieldWidthRow.getChildren().addAll(fieldWidthLabel, fieldWidthSlider, fieldWidthValue);

        // Field Height
        HBox fieldHeightRow = new HBox(10);
        fieldHeightRow.setAlignment(Pos.CENTER_LEFT);
        Label fieldHeightLabel = new Label("Field Height (No of cells):");
        fieldHeightLabel.setMinWidth(90);
        Slider fieldHeightSlider = createSlider(15, 30, config.getFieldHeight());
        Label fieldHeightValue = new Label(Integer.toString(config.getFieldHeight()));
        fieldHeightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int v = newVal.intValue();
            fieldHeightValue.setText(Integer.toString(v));
            config.setFieldHeight(v);
        });
        HBox.setHgrow(fieldHeightSlider, Priority.ALWAYS);
        fieldHeightRow.getChildren().addAll(fieldHeightLabel, fieldHeightSlider, fieldHeightValue);

        // Game Level
        HBox fieldGameRow = new HBox(10);
        fieldGameRow.setAlignment(Pos.CENTER_LEFT);
        Label gameLevelLabel = new Label("Game Level:");
        gameLevelLabel.setMinWidth(130);
        Slider gameLevelSlider = createSlider(1, 10, config.getGameLevel());
        Label fieldGameValue = new Label(Integer.toString(config.getGameLevel()));
        gameLevelSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int v = newVal.intValue();
            fieldGameValue.setText(Integer.toString(v));
            config.setGameLevel(v);
        });
        HBox.setHgrow(gameLevelSlider, Priority.ALWAYS);
        fieldGameRow.getChildren().addAll(gameLevelLabel, gameLevelSlider, fieldGameValue);

        // Music
        CheckBox musicCheckBox = new CheckBox("Music");
        musicCheckBox.setSelected(config.isMusic());
        Label musicValue = new Label(config.isMusic() ? "On" : "Off");
        musicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            musicValue.setText(newVal ? "On" : "Off");
            config.setMusic(newVal);
        });
        HBox musicRow = new HBox(10, musicCheckBox, musicValue);
        musicRow.setAlignment(Pos.TOP_LEFT);

        // Sound Effect
        CheckBox soundEffectCheckBox = new CheckBox("Sound Effect");
        soundEffectCheckBox.setSelected(config.isSoundEffect());
        Label soundValue = new Label(config.isSoundEffect() ? "On" : "Off");
        soundEffectCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            soundValue.setText(newVal ? "On" : "Off");
            config.setSoundEffect(newVal);
        });
        HBox soundRow = new HBox(10, soundEffectCheckBox, soundValue);
        soundRow.setAlignment(Pos.TOP_LEFT);

        // AI Play
        CheckBox aiPlayCheckBox = new CheckBox("AI Play");
        aiPlayCheckBox.setSelected(config.isAiPlay());
        Label aiValue = new Label(config.isAiPlay() ? "On" : "Off");
        aiPlayCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            aiValue.setText(newVal ? "On" : "Off");
            config.setAiPlay(newVal);
        });
        HBox aiRow = new HBox(10, aiPlayCheckBox, aiValue);
        aiRow.setAlignment(Pos.TOP_LEFT);

        // Extend Mode
        CheckBox extendModeCheckBox = new CheckBox("Extend Mode");
        extendModeCheckBox.setSelected(config.isExtendMode());
        Label extendValue = new Label(config.isExtendMode() ? "On" : "Off");
        extendModeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            extendValue.setText(newVal ? "On" : "Off");
            config.setExtendMode(newVal);
        });
        HBox extendRow = new HBox(10, extendModeCheckBox, extendValue);
        extendRow.setAlignment(Pos.TOP_LEFT);

        //Back
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            // Optional auto-save on back:
            ConfigService.save(config);
            MainMenu mainMenu = new MainMenu();
            try {
                mainMenu.start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox buttonRow = new HBox(10, backButton);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(20, 0, 0, 0));

        // Layout
        VBox layout = new VBox(10,
                titleBox,
                fieldWidthRow,
                fieldHeightRow,
                fieldGameRow,
                musicRow, soundRow, aiRow, extendRow,
                buttonRow
        );
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER_LEFT);

        Scene configScene = new Scene(layout, UIConfigurations.WINDOW_WIDTH, UIConfigurations.WINDOW_HEIGHT);
        stage.setScene(configScene);
        stage.setTitle("Configuration");
        stage.show();
    }

    private Slider createSlider(int min, int max, int value) {
        Slider slider = new Slider(min, max, value);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        return slider;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
