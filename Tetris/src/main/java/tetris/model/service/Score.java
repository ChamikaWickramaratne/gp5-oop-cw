package tetris.model.service;

public class Score {
    public String playerName;
    public int points;
    public String gameType; // NEW FIELD

    public Score(String playerName, int points) {
        this(playerName, points, "Human"); // default fallback
    }

    public Score(String playerName, int points, String gameType) {
        this.playerName = playerName;
        this.points = points;
        this.gameType = gameType;
    }

    public String getPlayerName() { return playerName; }
    public int getPoints() { return points; }
    public String getGameType() { return gameType; }
}
