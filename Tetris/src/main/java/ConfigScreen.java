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
        Label fieldWidthLabel = new Label("Field Width (No of cells):");
        Slider fieldWidthSlider = createSlider(5, 15, 10);
        Label fieldWidthValue = new Label(Integer.toString((int) fieldWidthSlider.getValue()));
        fieldWidthSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                fieldWidthValue.setText(Integer.toString(newVal.intValue()))
        );
        HBox fieldWidthRow = new HBox(10, fieldWidthSlider, fieldWidthValue);
        fieldWidthRow.setAlignment(Pos.TOP_LEFT);
        fieldWidthValue.setPadding(new Insets(0, 0, 0, 50));

        // Field Height
        Label fieldHeightLabel = new Label("Field Height (No of cells):");
        Slider fieldHeightSlider = createSlider(15, 30, 20);
        Label fieldHeightValue = new Label(Integer.toString((int) fieldHeightSlider.getValue()));
        fieldHeightSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                fieldHeightValue.setText(Integer.toString(newVal.intValue()))
        );
        HBox fieldHeightRow = new HBox(10, fieldHeightSlider, fieldHeightValue);
        fieldHeightRow.setAlignment(Pos.TOP_LEFT);
        fieldHeightValue.setPadding(new Insets(0, 0, 0, 50));

        // Game Level
        Label gameLevelLabel = new Label("Game Level:");
        Slider gameLevelSlider = createSlider(1, 10, 1);
        Label gameLevelValue = new Label(Integer.toString((int) gameLevelSlider.getValue()));
        gameLevelSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                gameLevelValue.setText(Integer.toString(newVal.intValue()))
        );
        HBox gameLevelRow = new HBox(10, gameLevelSlider, gameLevelValue);
        gameLevelRow.setAlignment(Pos.TOP_LEFT);
        gameLevelValue.setPadding(new Insets(0, 0, 0, 50));

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

        // Layout
        VBox layout = new VBox(10,
                titleBox,
                fieldWidthLabel, fieldWidthRow,
                fieldHeightLabel, fieldHeightRow,
                gameLevelLabel, gameLevelRow,
                musicRow, soundRow, aiRow, extendRow
        );
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER_LEFT);

        Scene configScene = new Scene(layout, 400, 500);
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
