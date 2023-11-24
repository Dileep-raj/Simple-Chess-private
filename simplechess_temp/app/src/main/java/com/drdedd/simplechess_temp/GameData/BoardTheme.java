package com.drdedd.simplechess_temp.GameData;

import com.drdedd.simplechess_temp.R;

/**
 * Board color for light and dark squares
 */
public enum BoardTheme {
    GREY(R.color.theme_grey_light, R.color.theme_grey_dark),
    GREEN(R.color.theme_green_light, R.color.theme_green_dark),
    BROWN(R.color.theme_brown_light, R.color.theme_brown_dark),
    BLUE(R.color.theme_blue_light, R.color.theme_blue_dark);
    final int lightColor, darkColor;
    public static final BoardTheme[] values = values();

    BoardTheme(int lightColor, int darkColor) {
        this.lightColor = lightColor;
        this.darkColor = darkColor;
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