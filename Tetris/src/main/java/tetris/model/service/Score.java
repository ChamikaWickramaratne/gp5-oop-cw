package tetris.model.service;

public class Score {
    public String playerName;
    public int points;
    public String gameType;
    public int boardWidth;
    public int boardHeight;
    public int level;
    public String mode;

    public Score(String playerName, int points) {
        this(playerName, points, "Human");
    }

    public Score(String playerName, int points, String gameType) {
        this(playerName, points, gameType, 10, 20, 1, "Single");
    }

    public Score(String playerName,
                 int points,
                 String gameType,
                 int boardWidth,
                 int boardHeight,
                 int level,
                 String mode) {
        this.playerName = playerName;
        this.points = points;
        this.gameType = gameType;
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.level = level;
        this.mode = mode;
    }

    // Getters for JavaFX / PropertyValueFactory
    public String getPlayerName() { return playerName; }
    public int getPoints() { return points; }
    public String getGameType() { return gameType; }
    public int getBoardWidth() { return boardWidth; }
    public int getBoardHeight() { return boardHeight; }
    public int getLevel() { return level; }
    public String getMode() { return mode; }

    // Convenience helper
    public String getBoardSizeLabel() {
        return boardWidth + "×" + boardHeight;
    }
}
