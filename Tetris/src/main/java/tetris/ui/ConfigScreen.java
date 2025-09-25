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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tetris.service.Config;
import tetris.service.ConfigService;

public class ConfigScreen extends Application {

    private final ConfigService configService = new ConfigService();
    private Config currentConfig;

    @Override
    public void start(Stage primaryStage) {
        // Load config when screen starts
        currentConfig = configService.loadConfig();
        showConfigScreen(primaryStage);
    }

    private void showConfigScreen(Stage stage) {

        Label titleLabel = new Label("Configuration");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        // Field Width
        Label fieldWidthLabel = new Label("Field Width (No of cells):");
        Slider fieldWidthSlider = createSlider(5, 15, currentConfig.fieldWidth);
        Label fieldWidthValue = new Label(Integer.toString((int) fieldWidthSlider.getValue()));
        fieldWidthSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                fieldWidthValue.setText(Integer.toString(newVal.intValue()))
        );
        HBox fieldWidthRow = new HBox(10, fieldWidthSlider, fieldWidthValue);
        fieldWidthRow.setAlignment(Pos.TOP_LEFT);
        fieldWidthValue.setPadding(new Insets(0, 0, 0, 50));

        // Field Height
        Label fieldHeightLabel = new Label("Field Height (No of cells):");
        Slider fieldHeightSlider = createSlider(15, 30, currentConfig.fieldHeight);
        Label fieldHeightValue = new Label(Integer.toString((int) fieldHeightSlider.getValue()));
        fieldHeightSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                fieldHeightValue.setText(Integer.toString(newVal.intValue()))
        );
        HBox fieldHeightRow = new HBox(10, fieldHeightSlider, fieldHeightValue);
        fieldHeightRow.setAlignment(Pos.TOP_LEFT);
        fieldHeightValue.setPadding(new Insets(0, 0, 0, 50));

        // Music
        CheckBox musicCheckBox = new CheckBox("Music");
        musicCheckBox.setSelected(currentConfig.musicOn);
        Label musicValue = new Label(currentConfig.musicOn ? "On" : "Off");
        musicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                musicValue.setText(newVal ? "On" : "Off")
        );
        HBox musicRow = new HBox(10, musicCheckBox, musicValue);
        musicRow.setAlignment(Pos.TOP_LEFT);

        // Sound Effect
        CheckBox soundEffectCheckBox = new CheckBox("Sound Effect");
        soundEffectCheckBox.setSelected(currentConfig.soundOn);
        Label soundValue = new Label(currentConfig.soundOn ? "On" : "Off");
        soundEffectCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                soundValue.setText(newVal ? "On" : "Off")
        );
        HBox soundRow = new HBox(10, soundEffectCheckBox, soundValue);
        soundRow.setAlignment(Pos.TOP_LEFT);

        // Save Button
        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            currentConfig = new Config(
                    (int) fieldWidthSlider.getValue(),
                    (int) fieldHeightSlider.getValue(),
                    musicCheckBox.isSelected(),
                    soundEffectCheckBox.isSelected()
            );
            configService.saveConfig(currentConfig);
        });

        // Back Button
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            MainMenu mainMenu = new MainMenu();
            try {
                mainMenu.start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        HBox backBox = new HBox(10, saveButton, backButton);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(20, 0, 0, 0));

        // Layout
        VBox layout = new VBox(10,
                titleBox,
                fieldWidthLabel, fieldWidthRow,
                fieldHeightLabel, fieldHeightRow,
                musicRow, soundRow,
                backBox
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
