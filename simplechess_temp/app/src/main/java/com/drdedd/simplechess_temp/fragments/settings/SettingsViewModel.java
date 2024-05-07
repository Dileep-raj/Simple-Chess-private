package com.drdedd.simplechess_temp.fragments.settings;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import com.drdedd.simplechess_temp.GameData.BoardTheme;
import com.drdedd.simplechess_temp.GameData.DataManager;

public class SettingsViewModel extends ViewModel {
    private boolean fullScreen, cheatMode, invertBlackSVGs, timer, vibration, animation;
    private String whiteName, blackName;
    private int minutes, seconds;
    private BoardTheme boardTheme;
    private DataManager dataManager;

    public void initializeData(Context context) {
        dataManager = new DataManager(context);
        fullScreen = dataManager.isFullScreen();
        cheatMode = dataManager.cheatModeEnabled();
        invertBlackSVGs = dataManager.invertBlackSVGEnabled();
        timer = dataManager.isTimerEnabled();
        vibration = dataManager.getVibration();
        animation = dataManager.getAnimation();
        minutes = dataManager.getTimerMinutes();
        seconds = dataManager.getTimerSeconds();
        whiteName = dataManager.getWhite();
        blackName = dataManager.getBlack();
        boardTheme = dataManager.getBoardTheme();
    }

    public void updateSettings() {
        dataManager.setFullScreen(fullScreen);
        dataManager.saveWhiteBlack(whiteName, blackName);
        dataManager.setCheatMode(cheatMode);
        dataManager.setInvertBlackSVG(invertBlackSVGs);
        dataManager.setTimerEnabled(timer);
        dataManager.setVibration(vibration);
        dataManager.setAnimation(animation);
        dataManager.setTimerMinutesSeconds(minutes, seconds);
        dataManager.setBoardTheme(boardTheme);
    }

    public BoardTheme getBoardTheme() {
        return boardTheme;
    }

    public void setBoardTheme(BoardTheme boardTheme) {
        this.boardTheme = boardTheme;
    }

    public String getWhiteName() {
        return whiteName;
    }

    public void setWhiteName(String whiteName) {
        this.whiteName = whiteName;
    }

    public String getBlackName() {
        return blackName;
    }

    public void setBlackName(String blackName) {
        this.blackName = blackName;
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public boolean isCheatMode() {
        return cheatMode;
    }

    public boolean isInvertBlackSVGs() {
        return invertBlackSVGs;
    }

    public boolean isTimer() {
        return timer;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setFullScreen(boolean fullScreen) {
        this.fullScreen = fullScreen;
    }

    public void setCheatMode(boolean cheatMode) {
        this.cheatMode = cheatMode;
    }

    public void setInvertBlackSVGs(boolean invertBlackSVGs) {
        this.invertBlackSVGs = invertBlackSVGs;
    }

    public void setTimer(boolean timer) {
        this.timer = timer;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public boolean getVibration() {
        return vibration;
    }

    public void setVibration(boolean vibration) {
        this.vibration = vibration;
    }

    public boolean getAnimation() {
        return animation;
    }

    public void setAnimation(boolean animation) {
        this.animation = animation;
    }
}