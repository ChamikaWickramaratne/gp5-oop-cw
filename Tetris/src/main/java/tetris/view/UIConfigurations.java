package tetris.view;

/**
 * Centralized configuration class for UI settings.
 *
 * This class defines reusable constants for the application's
 * UI such as window dimensions, default colors, and font settings.
 *
 * Using this class allows all screens (MainMenu, Gameplay, HighScore, etc.)
 * to share consistent layout and style configurations from a single location.
 */
public class UIConfigurations {

    /**
     * The default width of all application windows (in pixels).
     */
    public static final int WINDOW_WIDTH = 500;

    /**
     * The default height of all application windows (in pixels).
     */
    public static final int WINDOW_HEIGHT = 500;

    // In the future, extend this class with additional UI configurations:
    // public static final Color BACKGROUND_COLOR = Color.web("#1e1e1e");
    // public static final String DEFAULT_FONT = "Arial";
    // public static final int DEFAULT_FONT_SIZE = 16;
}

