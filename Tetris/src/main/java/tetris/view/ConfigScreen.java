package tetris.view;

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
import tetris.model.PlayerType;
import tetris.config.TetrisConfig;

public class ConfigScreen extends Application {

    private TetrisConfig config;

    @Override
    public void start(Stage primaryStage) {
        showConfigScreen(primaryStage);
    }

    private void showConfigScreen(Stage stage) {
        config = ConfigService.load();

        Label titleLabel = new Label("Configuration");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        HBox fieldWidthRow = new HBox(10);
        fieldWidthRow.setAlignment(Pos.CENTER_LEFT);
        Label fieldWidthLabel = new Label("Field Width (No of cells):");
        fieldWidthLabel.setMinWidth(150);
        Slider fieldWidthSlider = createSlider(5, 15, config.getFieldWidth());
        Label fieldWidthValue = new Label(Integer.toString(config.getFieldWidth()));
        fieldWidthSlider.valueProperty().addListener((obs, o, n) -> {
            int v = n.intValue();
            fieldWidthValue.setText(Integer.toString(v));
            config.setFieldWidth(v);
        });
        HBox.setHgrow(fieldWidthSlider, Priority.ALWAYS);
        fieldWidthRow.getChildren().addAll(fieldWidthLabel, fieldWidthSlider, fieldWidthValue);

        HBox fieldHeightRow = new HBox(10);
        fieldHeightRow.setAlignment(Pos.CENTER_LEFT);
        Label fieldHeightLabel = new Label("Field Height (No of cells):");
        fieldHeightLabel.setMinWidth(150);
        Slider fieldHeightSlider = createSlider(15, 30, config.getFieldHeight());
        Label fieldHeightValue = new Label(Integer.toString(config.getFieldHeight()));
        fieldHeightSlider.valueProperty().addListener((obs, o, n) -> {
            int v = n.intValue();
            fieldHeightValue.setText(Integer.toString(v));
            config.setFieldHeight(v);
        });
        HBox.setHgrow(fieldHeightSlider, Priority.ALWAYS);
        fieldHeightRow.getChildren().addAll(fieldHeightLabel, fieldHeightSlider, fieldHeightValue);

        HBox fieldGameRow = new HBox(10);
        fieldGameRow.setAlignment(Pos.CENTER_LEFT);
        Label gameLevelLabel = new Label("Game Level:");
        gameLevelLabel.setMinWidth(150);
        Slider gameLevelSlider = createSlider(1, 10, config.getGameLevel());
        Label fieldGameValue = new Label(Integer.toString(config.getGameLevel()));
        gameLevelSlider.valueProperty().addListener((obs, o, n) -> {
            int v = n.intValue();
            fieldGameValue.setText(Integer.toString(v));
            config.setGameLevel(v);
        });
        HBox.setHgrow(gameLevelSlider, Priority.ALWAYS);
        fieldGameRow.getChildren().addAll(gameLevelLabel, gameLevelSlider, fieldGameValue);

        CheckBox musicCheckBox = new CheckBox("Music");
        musicCheckBox.setSelected(config.isMusic());
        Label musicValue = new Label(config.isMusic() ? "On" : "Off");
        musicCheckBox.selectedProperty().addListener((obs, o, n) -> {
            config.setMusic(n);
            musicValue.setText(n ? "On" : "Off");
        });
        HBox musicRow = new HBox(10, musicCheckBox, musicValue);
        musicRow.setAlignment(Pos.TOP_LEFT);

        CheckBox soundEffectCheckBox = new CheckBox("Sound Effect");
        soundEffectCheckBox.setSelected(config.isSoundEffect());
        Label soundValue = new Label(config.isSoundEffect() ? "On" : "Off");
        soundEffectCheckBox.selectedProperty().addListener((obs, o, n) -> {
            config.setSoundEffect(n);
            soundValue.setText(n ? "On" : "Off");
        });
        HBox soundRow = new HBox(10, soundEffectCheckBox, soundValue);
        soundRow.setAlignment(Pos.TOP_LEFT);

        CheckBox extendModeCheckBox = new CheckBox("Extend Mode");
        extendModeCheckBox.setSelected(config.isExtendMode());
        Label extendValue = new Label(config.isExtendMode() ? "On" : "Off");
        extendModeCheckBox.selectedProperty().addListener((obs, o, n) -> {
            extendValue.setText(n ? "On" : "Off");
            config.setExtendMode(n);
        });
        HBox extendRow = new HBox(10, extendModeCheckBox, extendValue);
        extendRow.setAlignment(Pos.TOP_LEFT);

        HBox p1Row = new HBox(10);
        p1Row.setAlignment(Pos.CENTER_LEFT);
        Label p1Label = new Label("Player 1:");
        p1Label.setMinWidth(150);

        ToggleGroup p1Group = new ToggleGroup();
        RadioButton p1Human   = new RadioButton("Human");
        RadioButton p1AI      = new RadioButton("AI");
        RadioButton p1External= new RadioButton("External");
        p1Human.setToggleGroup(p1Group);
        p1AI.setToggleGroup(p1Group);
        p1External.setToggleGroup(p1Group);

        switch (config.getPlayer1Type()) {
            case HUMAN -> p1Human.setSelected(true);
            case AI -> p1AI.setSelected(true);
            case EXTERNAL -> p1External.setSelected(true);
        }

        p1Group.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            RadioButton rb = (RadioButton) n;
            config.setPlayer1Type(mapTextToType(rb.getText()));
        });

        p1Row.getChildren().addAll(p1Label, p1Human, p1AI, p1External);

        HBox p2Row = new HBox(10);
        p2Row.setAlignment(Pos.CENTER_LEFT);
        Label p2Label = new Label("Player 2:");
        p2Label.setMinWidth(150);

        ToggleGroup p2Group = new ToggleGroup();
        RadioButton p2Human   = new RadioButton("Human");
        RadioButton p2AI      = new RadioButton("AI");
        RadioButton p2External= new RadioButton("External");
        p2Human.setToggleGroup(p2Group);
        p2AI.setToggleGroup(p2Group);
        p2External.setToggleGroup(p2Group);

        switch (config.getPlayer2Type()) {
            case HUMAN -> p2Human.setSelected(true);
            case AI -> p2AI.setSelected(true);
            case EXTERNAL -> p2External.setSelected(true);
        }

        p2Group.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            RadioButton rb = (RadioButton) n;
            config.setPlayer2Type(mapTextToType(rb.getText()));
        });

        p2Row.disableProperty().bind(extendModeCheckBox.selectedProperty().not());

        p2Row.getChildren().addAll(p2Label, p2Human, p2AI, p2External);

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            ConfigService.save(config); // autosave
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

        VBox layout = new VBox(10,
                titleBox,
                fieldWidthRow,
                fieldHeightRow,
                fieldGameRow,
                musicRow, soundRow, extendRow,
                p1Row, p2Row,
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

    private PlayerType mapTextToType(String text) {
        String t = text.trim().toUpperCase();
        return switch (t) {
            case "HUMAN" -> PlayerType.HUMAN;
            case "AI" -> PlayerType.AI;
            case "EXTERNAL" -> PlayerType.EXTERNAL;
            default -> PlayerType.HUMAN;
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
