package com.drdedd.simplechess_temp.GameData;

import android.graphics.Color;

/**
 * Board color for light and dark squares
 */
public enum BoardTheme {
    GREY(Color.rgb(233, 233, 233), Color.rgb(102, 102, 102)), GREEN(Color.rgb(238, 238, 210), Color.rgb(118, 150, 86)), BROWN(Color.rgb(241, 204, 162), Color.rgb(170, 107, 64));
    final int lightColor, darkColor;
    public static final BoardTheme values[] = values();

    BoardTheme(int lightColor, int darkColor) {
        this.lightColor = lightColor;
        this.darkColor = darkColor;
    }

    public static BoardTheme[] get() {
        return values;
    }

    public int getDarkColor() {
        return darkColor;
    }

    public int getLightColor() {
        return lightColor;
    }
}