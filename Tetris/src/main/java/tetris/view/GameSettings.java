package tetris.view;

import java.util.prefs.Preferences;

public class GameSettings {
    public static boolean MUSIC_ON = true;
    public static boolean SOUND_ON = true;

    private static final Preferences prefs = Preferences.userNodeForPackage(GameSettings.class);

    static {
        // Load saved values at startup
        MUSIC_ON = prefs.getBoolean("musicOn", true);
        SOUND_ON = prefs.getBoolean("soundOn", true);
    }

    // Save when changed
    public static void save() {
        prefs.putBoolean("musicOn", MUSIC_ON);
        prefs.putBoolean("soundOn", SOUND_ON);
    }
}
