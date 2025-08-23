package tetris;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

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

    @Override
    public void start(Stage stage) {
        // Title
        Label titleLabel = new Label("High Score");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);

        // Table
        TableView<ScoreEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ScoreEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<ScoreEntry, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setStyle("-fx-alignment: CENTER;"); // Center-align score column

        table.getColumns().addAll(nameCol, scoreCol);

        // Dummy data
        table.getItems().addAll(
                new ScoreEntry("Tony Stark", 9840),
                new ScoreEntry("Steve Rogers", 8765),
                new ScoreEntry("Natasha Romanoff", 8432),
                new ScoreEntry("Bruce Banner", 7910),
                new ScoreEntry("Peter Parker", 6823),
                new ScoreEntry("Wanda Maximoff", 6391),
                new ScoreEntry("Stephen Strange", 5580),
                new ScoreEntry("Carol Danvers", 4729),
                new ScoreEntry("Sam Wilson", 3665),
                new ScoreEntry("Scott Lang", 2584)
        );

        // Remove extra row by resizing table to fit content
        table.setFixedCellSize(25);
        table.prefHeightProperty().bind(
                table.fixedCellSizeProperty().multiply(table.getItems().size()).add(30)
        );
        table.setPlaceholder(new Label("")); // No placeholder row

        // Back Button
        Button backButton = new Button("Back");
        HBox backBox = new HBox(backButton);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(10, 0, 0, 0));

        // Back Button Functionality
        backButton.setOnAction(e -> {
            MainMenu mainView = new MainMenu();
            try {
                mainView.start(stage);  // Navigate back to Main menu
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        // Author
        Label authorLabel = new Label("Version : v2.0.0");
        HBox authorBox = new HBox(authorLabel);
        authorBox.setAlignment(Pos.CENTER);
        authorBox.setPadding(new Insets(10, 0, 0, 0));

        // Main layout
        VBox root = new VBox(15, titleBox, table, backBox, authorBox);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(root, 400, 500);
        stage.setTitle("Tetris");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
