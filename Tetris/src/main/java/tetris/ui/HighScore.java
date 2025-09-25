package tetris.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tetris.service.HighScoreManager;
import tetris.service.Score;

import java.util.List;

public class HighScore extends Application {

    public static class ScoreEntry {
        private final String name;
        private final int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }
    }

    private final HighScoreManager manager = new HighScoreManager();
    private TableView<ScoreEntry> table;

    @Override
    public void start(Stage stage) {
        // Title
        Label titleLabel = new Label("High Score");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);

        // Table
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ScoreEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<ScoreEntry, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(nameCol, scoreCol);

        refreshTable(); // load existing scores

        // Clear High Scores Button
        Button clearButton = new Button("Clear High Scores");
        clearButton.setOnAction(e -> {
            manager.clear();       // wipe scores.json
            refreshTable();        // refresh UI
        });

        // Back Button
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            MainMenu mainView = new MainMenu();
            try {
                mainView.start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox buttonRow = new HBox(15, clearButton, backButton);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(10, 0, 0, 0));

        // Author / Version
        Label authorLabel = new Label("Version : v2.0.0");
        HBox authorBox = new HBox(authorLabel);
        authorBox.setAlignment(Pos.CENTER);
        authorBox.setPadding(new Insets(10, 0, 0, 0));

        // Main layout
        VBox root = new VBox(15, titleBox, table, buttonRow, authorBox);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(root, UIConfigurations.WINDOW_WIDTH, UIConfigurations.WINDOW_HEIGHT);
        stage.setTitle("Tetris");
        stage.setScene(scene);
        stage.show();
    }

    private void refreshTable() {
        table.getItems().clear();
        List<Score> scores = manager.loadScores();
        for (Score s : scores) {
            table.getItems().add(new ScoreEntry(s.playerName, s.points));
        }
        table.setFixedCellSize(25);
        table.prefHeightProperty().bind(
                table.fixedCellSizeProperty().multiply(table.getItems().size()).add(30)
        );
        table.setPlaceholder(new Label(""));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
