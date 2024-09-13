package com.drdedd.simplichess.game.gameData;

import com.drdedd.simplichess.R;

/**
 * Board color for light and dark squares
 */
public enum BoardTheme {
    /**
     * Default color of board
     */
    DEFAULT("Default", R.color.theme_default_light, R.color.theme_default_dark),
    /**
     * Black and white board theme
     */
    GREY("Grey", R.color.theme_grey_light, R.color.theme_grey_dark),
    /**
     * Green theme like in chess.com
     */
    GREEN("Green", R.color.theme_green_light, R.color.theme_green_dark),
    /**
     * Vibrant brown theme
     */
    BROWN("Brown", R.color.theme_brown_light, R.color.theme_brown_dark),
    /**
     * Lichess board theme
     */
    LICHESS("Lichess", R.color.theme_lichess_light, R.color.theme_lichess_dark),
    /**
     * Blue board theme
     */
    BLUE("Blue", R.color.theme_blue_light, R.color.theme_blue_dark);
    final int lightColor, darkColor;
    private static final BoardTheme[] values = values();
    private final String themeName;

    BoardTheme(String themeName, int lightColor, int darkColor) {
        this.themeName = themeName;
        this.lightColor = lightColor;
        this.darkColor = darkColor;
    }

    /**
     * Name of the Theme
     */
    public String getThemeName() {
        return themeName;
    }

    /**
     * Array of values of BoardThemes
     */
    public static BoardTheme[] getValues() {
        return values;
    }

    /**
     * Dark color resource id
     */
    public int getDarkColor() {
        return darkColor;
    }

    /**
     * Light color resource id
     */
    public int getLightColor() {
        return lightColor;
    }
}