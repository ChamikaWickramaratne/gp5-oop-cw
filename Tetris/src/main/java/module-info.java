module Tetris {
    requires com.fasterxml.jackson.databind;
    requires com.google.gson;
    requires java.prefs;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.media;

    opens tetris.ui to javafx.graphics, javafx.fxml, javafx.base;
    opens tetris.config to com.fasterxml.jackson.databind;
    opens tetris.service to com.google.gson;
}