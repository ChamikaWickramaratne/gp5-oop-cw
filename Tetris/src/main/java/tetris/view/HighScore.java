package tetris.view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tetris.model.service.HighScoreManager;
import tetris.model.service.Score;

import java.util.List;

public class HighScore extends Application {

    public static class ScoreEntry {
        private final String name;
        private final int score;
        private final String gameType;
        private final String boardSize; // NEW
        private final int level;        // NEW
        private final String mode;      // NEW

        public ScoreEntry(String name, int score, String gameType, String boardSize, int level, String mode) {
            this.name = name;
            this.score = score;
            this.gameType = gameType;
            this.boardSize = boardSize;
            this.level = level;
            this.mode = mode;
        }

        public String getName() { return name; }
        public int getScore() { return score; }
        public String getGameType() { return gameType; }
        public String getBoardSize() { return boardSize; }
        public int getLevel() { return level; }
        public String getMode() { return mode; }
    }

    private final HighScoreManager manager = new HighScoreManager();
    private TableView<ScoreEntry> table;

    @Override
    public void start(Stage stage) {
        Label titleLabel = new Label("High Scores");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<ScoreEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<ScoreEntry, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ScoreEntry, String> typeCol = new TableColumn<>("Game Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("gameType"));
        typeCol.setStyle("-fx-alignment: CENTER;");

        // NEW columns
        TableColumn<ScoreEntry, String> boardCol = new TableColumn<>("Board");
        boardCol.setCellValueFactory(new PropertyValueFactory<>("boardSize"));
        boardCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ScoreEntry, Integer> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<>("level"));
        levelCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ScoreEntry, String> modeCol = new TableColumn<>("Mode");
        modeCol.setCellValueFactory(new PropertyValueFactory<>("mode"));
        modeCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(nameCol, scoreCol, typeCol, boardCol, levelCol, modeCol);
        refreshTable();

        Button clearButton = new Button("Clear High Scores");
        clearButton.setOnAction(e -> {
            manager.clear();
            refreshTable();
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            try {
                new MainMenu().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox buttonRow = new HBox(15, clearButton, backButton);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(10, 0, 0, 0));

        Label authorLabel = new Label("Version : v2.0.0");
        HBox authorBox = new HBox(authorLabel);
        authorBox.setAlignment(Pos.CENTER);
        authorBox.setPadding(new Insets(10, 0, 0, 0));

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
            // Back-compat defaults if old entries lack fields
            int bw = (s.getBoardWidth() > 0) ? s.getBoardWidth() : 10;
            int bh = (s.getBoardHeight() > 0) ? s.getBoardHeight() : 20;
            int lvl = (s.getLevel() > 0) ? s.getLevel() : 1;
            String mode = (s.getMode() != null && !s.getMode().isBlank()) ? s.getMode() : "Single";
            String boardSize = bw + "×" + bh;

            table.getItems().add(new ScoreEntry(
                    s.getPlayerName(),
                    s.getPoints(),
                    s.getGameType() != null ? s.getGameType() : "Human",
                    boardSize,
                    lvl,
                    mode
            ));
        }

        table.setFixedCellSize(25);
        table.prefHeightProperty().bind(
                table.fixedCellSizeProperty().multiply(Math.max(table.getItems().size(), 1)).add(30)
        );
        table.setPlaceholder(new Label(""));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
