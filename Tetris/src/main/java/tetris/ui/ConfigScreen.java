package tetris.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
            config.setMusic(newVal);
            musicValue.setText(newVal ? "On" : "Off");
        });
        HBox musicRow = new HBox(10, musicCheckBox, musicValue);
        musicRow.setAlignment(Pos.TOP_LEFT);

        // Sound Effect
        CheckBox soundEffectCheckBox = new CheckBox("Sound Effect");
        soundEffectCheckBox.setSelected(config.isSoundEffect());
        Label soundValue = new Label(config.isSoundEffect() ? "On" : "Off");

        soundEffectCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            config.setSoundEffect(newVal);
            soundValue.setText(newVal ? "On" : "Off");
        });
        HBox soundRow = new HBox(10, soundEffectCheckBox, soundValue);
        soundRow.setAlignment(Pos.TOP_LEFT);

        // Extend Mode (UI only for now)
        CheckBox extendModeCheckBox = new CheckBox("Extend Mode");
        extendModeCheckBox.setSelected(config.isExtendMode()); // default from config
        Label extendValue = new Label(extendModeCheckBox.isSelected() ? "On" : "Off");

        // Update label when changed
        extendModeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            extendValue.setText(newVal ? "On" : "Off");
        });

        // Player 1 Type
        Label player1Label = new Label("Player 1 Type:");
        RadioButton p1Human = new RadioButton("Human");
        RadioButton p1AI = new RadioButton("AI");
        RadioButton p1External = new RadioButton("External");

        ToggleGroup p1Group = new ToggleGroup();
        p1Human.setToggleGroup(p1Group);
        p1AI.setToggleGroup(p1Group);
        p1External.setToggleGroup(p1Group);

        // default selection
        p1Human.setSelected(true);

        HBox player1Row = new HBox(10, player1Label, p1Human, p1AI, p1External);
        player1Row.setAlignment(Pos.TOP_LEFT);


        // Player Two radio buttons (Human, AI, External)
        ToggleGroup player2Group = new ToggleGroup();
        RadioButton player2Human = new RadioButton("Human");
        RadioButton player2AI = new RadioButton("AI");
        RadioButton player2External = new RadioButton("External");

        player2Human.setToggleGroup(player2Group);
        player2AI.setToggleGroup(player2Group);
        player2External.setToggleGroup(player2Group);

        // Initially disabled if extend mode is false
        player2Human.disableProperty().bind(extendModeCheckBox.selectedProperty().not());
        player2AI.disableProperty().bind(extendModeCheckBox.selectedProperty().not());
        player2External.disableProperty().bind(extendModeCheckBox.selectedProperty().not());

        // Layout row for Extend Mode
        HBox extendRow = new HBox(10, extendModeCheckBox, extendValue);
        extendRow.setAlignment(Pos.TOP_LEFT);

        // Layout row for Player Two
        HBox player2Row = new HBox(10, new Label("Player 2 Type:"), player2Human, player2AI, player2External);
        player2Row.setAlignment(Pos.CENTER_LEFT);




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
                musicRow,
                soundRow,
                extendRow,
                player1Row,
                player2Row,
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
