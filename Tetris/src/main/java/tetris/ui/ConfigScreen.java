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

public class ConfigScreen extends Application {

    @Override
    public void start(Stage primaryStage) {
        showConfigScreen(primaryStage);
    }

    private void showConfigScreen(Stage stage) {

        Label titleLabel = new Label("Configuration");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        // Field Width
        HBox fieldWidthRow = new HBox(10); // 10px spacing
        fieldWidthRow.setAlignment(Pos.CENTER_LEFT);

        Label fieldWidthLabel = new Label("Field Width (No of cells):");
        fieldWidthLabel.setMinWidth(100);   // keep labels aligned
        Slider fieldWidthSlider = createSlider(5, 15, 10);
        Label fieldWidthValue = new Label("10");

        fieldWidthSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                fieldWidthValue.setText(Integer.toString(newVal.intValue()))
        );

        // allow slider to stretch
        HBox.setHgrow(fieldWidthSlider, Priority.ALWAYS);

        fieldWidthRow.getChildren().addAll(fieldWidthLabel, fieldWidthSlider, fieldWidthValue);

        // Field Height
        HBox fieldHeightRow = new HBox(10);
        fieldHeightRow.setAlignment(Pos.CENTER_LEFT);

        Label fieldHeightLabel = new Label("Field Height (No of cells):");
        fieldHeightLabel.setMinWidth(90);  // same width as fieldWidthLabel
        Slider fieldHeightSlider = createSlider(15, 30, 20);
        Label fieldHeightValue = new Label("20");

        fieldHeightSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                fieldHeightValue.setText(Integer.toString(newVal.intValue()))
        );

        HBox.setHgrow(fieldHeightSlider, Priority.ALWAYS);

        fieldHeightRow.getChildren().addAll(fieldHeightLabel, fieldHeightSlider, fieldHeightValue);

        // Game Level
        HBox fieldGameRow = new HBox(10);
        fieldGameRow.setAlignment(Pos.CENTER_LEFT);

        Label gameLevelLabel = new Label("Game Level:");
        gameLevelLabel.setMinWidth(130); // match width with Field Width/Height labels
        Slider gameLevelSlider = createSlider(1, 10, 1);
        Label fieldGameValue = new Label("1");

        gameLevelSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                fieldGameValue.setText(Integer.toString(newVal.intValue()))
        );
        HBox.setHgrow(gameLevelSlider, Priority.ALWAYS);

        fieldGameRow.getChildren().addAll(gameLevelLabel, gameLevelSlider, fieldGameValue);


        // Music
        CheckBox musicCheckBox = new CheckBox("Music");
        musicCheckBox.setSelected(true);
        Label musicValue = new Label("On");
        musicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                musicValue.setText(newVal ? "On" : "Off")
        );
        HBox musicRow = new HBox(10, musicCheckBox, musicValue);
        musicRow.setAlignment(Pos.TOP_LEFT);

        // Sound Effect
        CheckBox soundEffectCheckBox = new CheckBox("Sound Effect");
        soundEffectCheckBox.setSelected(true);
        Label soundValue = new Label("On");
        soundEffectCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                soundValue.setText(newVal ? "On" : "Off")
        );
        HBox soundRow = new HBox(10, soundEffectCheckBox, soundValue);
        soundRow.setAlignment(Pos.TOP_LEFT);

        // AI Play
        CheckBox aiPlayCheckBox = new CheckBox("AI Play");
        Label aiValue = new Label("Off");
        aiPlayCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                aiValue.setText(newVal ? "On" : "Off")
        );
        HBox aiRow = new HBox(10, aiPlayCheckBox, aiValue);
        aiRow.setAlignment(Pos.TOP_LEFT);

        // Extend Mode
        CheckBox extendModeCheckBox = new CheckBox("Extend Mode");
        Label extendValue = new Label("Off");
        extendModeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                extendValue.setText(newVal ? "On" : "Off")
        );
        HBox extendRow = new HBox(10, extendModeCheckBox, extendValue);
        extendRow.setAlignment(Pos.TOP_LEFT);

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
        HBox backBox = new HBox(backButton);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(20, 0, 0, 0));

        // Layout
        VBox layout = new VBox(10,
                titleBox,
                fieldWidthRow,
                fieldHeightRow,
                fieldGameRow,
                musicRow, soundRow, aiRow, extendRow,
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
