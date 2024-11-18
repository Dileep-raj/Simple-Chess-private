package com.drdedd.simplichess.fragments.settings;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.drdedd.simplichess.data.DataManager;
import com.drdedd.simplichess.game.BoardModel;
import com.drdedd.simplichess.game.gameData.BoardTheme;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.pieces.Pawn;
import com.drdedd.simplichess.game.pieces.Piece;
import com.drdedd.simplichess.interfaces.GameLogicInterface;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SettingsViewModel extends ViewModel implements GameLogicInterface {
    private boolean fullScreen, cheatMode, invertBlackSVGs, timer, vibration, animation, sound, restartActivity, backgroundImage;
    private String whiteName, blackName;
    private int minutes, seconds;
    private BoardTheme boardTheme;
    private DataManager dataManager;
    private BoardModel boardModel;
    private HashMap<Piece, HashSet<Integer>> legalMoves;

    public void initializeData(Context context) {
        boardModel = BoardModel.parseFEN("3k4/8/3KQ3/8/8/8/8/8 w - - 0 1", context);
        if (boardModel == null) boardModel = new BoardModel(context, true);
        Piece piece = boardModel.pieceAt(5, 4);
        Log.d("TestFragment", "initializeData: Piece: " + piece);
        legalMoves = new HashMap<>(Map.of(piece, piece.getPossibleMoves(this)));
        dataManager = new DataManager(context);
        fullScreen = dataManager.getBoolean(DataManager.FULL_SCREEN);
        cheatMode = dataManager.getBoolean(DataManager.CHEAT_MODE);
        invertBlackSVGs = dataManager.getBoolean(DataManager.INVERT_BLACK_PIECES);
        timer = dataManager.getBoolean(DataManager.TIMER);
        vibration = dataManager.getBoolean(DataManager.VIBRATION);
        sound = dataManager.getBoolean(DataManager.SOUND);
        animation = dataManager.getBoolean(DataManager.ANIMATION);
        minutes = dataManager.getInt(DataManager.MINUTES);
        seconds = dataManager.getInt(DataManager.SECONDS);
        whiteName = dataManager.getString(DataManager.WHITE);
        blackName = dataManager.getString(DataManager.BLACK);
        boardTheme = dataManager.getBoardTheme();
        backgroundImage = dataManager.getBoolean(DataManager.USE_BOARD_IMAGE);
        restartActivity = false;
    }

    public void updateSettings() {
        dataManager.setString(DataManager.WHITE,whiteName);
        dataManager.setString(DataManager.BLACK,blackName);
        dataManager.setBoolean(DataManager.FULL_SCREEN, fullScreen);
        dataManager.setBoolean(DataManager.CHEAT_MODE, cheatMode);
        dataManager.setBoolean(DataManager.INVERT_BLACK_PIECES, invertBlackSVGs);
        dataManager.setBoolean(DataManager.TIMER, timer);
        dataManager.setBoolean(DataManager.VIBRATION, vibration);
        dataManager.setBoolean(DataManager.SOUND, sound);
        dataManager.setBoolean(DataManager.ANIMATION, animation);
        dataManager.setInt(DataManager.MINUTES, minutes);
        dataManager.setInt(DataManager.SECONDS, seconds);
        dataManager.setBoolean(DataManager.USE_BOARD_IMAGE, backgroundImage);
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
        if (this.fullScreen != fullScreen) restartActivity = true;
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

    public boolean getSound() {
        return sound;
    }

    public void setSound(boolean sound) {
        this.sound = sound;
    }

    public boolean isBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(boolean backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public boolean isRestartActivity() {
        return restartActivity;
    }

    @Override
    public Piece pieceAt(int row, int col) {
        return boardModel.pieceAt(row, col);
    }

    @Override
    public boolean move(int fromRow, int fromCol, int toRow, int toCol) {
        return false;
    }

    @Override
    public boolean capturePiece(Piece piece) {
        return false;
    }

    @Override
    public void promote(Pawn pawn, int row, int col, int fromRow, int fromCol) {

    }

    @Override
    public void terminateByTimeOut(Player player) {

    }

    @Override
    public BoardModel getBoardModel() {
        return boardModel;
    }

    @Override
    public HashMap<Piece, HashSet<Integer>> getLegalMoves() {
        return legalMoves;
    }

    @Override
    public boolean isWhiteToPlay() {
        return true;
    }

    @Override
    public boolean isGameTerminated() {
        return false;
    }

    @Override
    public boolean isPieceToPlay(Piece piece) {
        return false;
    }
}