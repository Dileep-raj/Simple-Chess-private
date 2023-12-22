package com.drdedd.simplechess_temp.GameData;

import com.drdedd.simplechess_temp.R;

/**
 * Board color for light and dark squares
 */
public enum BoardTheme {
    GREY("Grey", R.color.theme_grey_light, R.color.theme_grey_dark),
    GREEN("Green", R.color.theme_green_light, R.color.theme_green_dark),
    DEFAULT_BROWN("Default Brown", R.color.theme_default_brown_light, R.color.theme_default_brown_dark),
    LICHESS("Lichess", R.color.theme_lichess_light, R.color.theme_lichess_dark),
    LICHESS2("Lichess 2", R.color.theme_lichess2_light, R.color.theme_lichess2_dark),
    BLUE("Blue", R.color.theme_blue_light, R.color.theme_blue_dark);
    final int lightColor, darkColor;
    public static final BoardTheme[] values = values();
    private final String themeName;

    BoardTheme(String themeName, int lightColor, int darkColor) {
        this.themeName = themeName;
        this.lightColor = lightColor;
        this.darkColor = darkColor;
    }

    public String getThemeName() {
        return themeName;
    }

    public static BoardTheme[] getValues() {
        return values;
    }

    public int getDarkColor() {
        return darkColor;
    }

    public int getLightColor() {
        return lightColor;
    }
}