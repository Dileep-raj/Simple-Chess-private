package com.drdedd.simplichess.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.drdedd.simplichess.game.BoardModel;
import com.drdedd.simplichess.game.gameData.BoardTheme;
import com.drdedd.simplichess.game.pgn.PGN;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * A class to store and retrieve the game data using files or <code>SharedPreferences</code>
 */
public class DataManager {
    public static final String TAG = "DataManager", BOARD_FILE = "boardFile", PGN_FILE = "PGNFile", STACK_FILE = "stackFile", FENS_LIST_FILE = "FENsListFile";
    public static final String BOARD_THEME = "BoardTheme", WHITE = "white", BLACK = "black", FULL_SCREEN = "fullScreen", CHEAT_MODE = "cheatMode", INVERT_BLACK_PIECES = "invertBlackSVG";
    public static final String TIMER = "timer", MINUTES = "minutes", SECONDS = "seconds", WHITE_TIME_LEFT = "whiteTimeLeft", BLACK_TIME_LEFT = "blackTimeLeft", VIBRATION = "vibration";
    public static final String ANIMATION = "animation", SOUND = "sound", USE_BOARD_IMAGE = "useBoardImage";
    public final String savedGameDir;
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final HashMap<String, BoardTheme> themesMap = new HashMap<>();

    public DataManager(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
        BoardTheme[] themes = BoardTheme.getValues();
        for (BoardTheme theme : themes)
            themesMap.put(theme.toString(), theme);
        savedGameDir = context.getFilesDir() + File.separator + "SavedGames" + File.separator;
    }

    /**
     * Read serialized object from the file
     *
     * @param fileName Name of the file
     * @return <code>Object|null</code>
     */
    public @Nullable Object readObject(String fileName) {
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(context.getFilesDir(), fileName));
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object obj = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
            return obj;
        } catch (FileNotFoundException e) {
            Log.d(TAG, fileName + " not found" + "\n" + e);
        } catch (IOException e) {
            Log.d(TAG, "Couldn't load " + fileName + "\n" + e);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, fileName + " corrupted " + "\n" + e);
        }
        return null;
    }

    /**
     * Save serialized object into a file in storage
     *
     * @param fileName Name of the file
     * @param obj      Object to be saved
     */
    public void saveObject(String fileName, Object obj) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(obj);
//            Log.d(TAG, fileName + " saved successfully");
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
            Log.d(TAG, fileName + " not found" + "\n" + e);
        } catch (IOException e) {
            Toast.makeText(context, "Error while saving " + fileName, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Error while saving " + fileName + "\n" + e);
        }
    }

    /**
     * @param pgn  PGN to be saved
     * @param name Name of the PGN file
     * @return <code>true|false</code> - File save result
     */
    public boolean savePGN(PGN pgn, String name) {
        try {
            File dir = new File(savedGameDir);
            if (!dir.isDirectory() && dir.mkdir())
                Log.d(TAG, String.format("saveGame: Directory %s created", savedGameDir));

            File file = new File(savedGameDir, name);
            boolean result = file.createNewFile();

            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                fileOutputStream.write(pgn.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (result) Log.d(TAG, String.format("saveGame: Game saving in %s", savedGameDir));
            return result;
        } catch (Exception e) {
            Log.e(TAG, "saveGame: Error while saving game", e);
            return false;
        }
    }

    /**
     * @return List of games in PGN library
     */
    public ArrayList<String> savedGames() {
        ArrayList<String> savedGames = new ArrayList<>();
        File filesDir = new File(savedGameDir);
        File[] files = filesDir.listFiles();
        if (files != null) for (File file : files) {
            savedGames.add(file.getName());

        }
        return savedGames;
    }

    /**
     * Deletes game object file
     *
     * @param fileName Name of the file to be deleted
     * @return <code>true|false</code> - Deletion result
     */
    private boolean deleteFile(String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        if (file.exists()) return file.delete();
        Log.d(TAG, "deleteFile: File does not exist");
        return false;
    }

    /**
     * Deletes all game files
     *
     * @return <code>true|false</code> - Deletion result
     */
    public boolean deleteGameFiles() {
        return deleteFile(BOARD_FILE) && deleteFile(STACK_FILE) && deleteFile(PGN_FILE) && deleteFile(FENS_LIST_FILE);
    }

    /**
     * Delete game from PGN library
     *
     * @param fileName Name of the saved game
     * @return <code>true|false</code> - Deletion result
     */
    public boolean deleteGame(String fileName) {
        File file = new File(savedGameDir, fileName);
        if (file.exists()) return file.delete();
        return false;
    }

    /**
     * Retrieve saved board theme
     *
     * @return <code>BoardTheme</code> - Light and dark color scheme for board
     */
    public BoardTheme getBoardTheme() {
        BoardTheme boardTheme;
        try {
            boardTheme = themesMap.get(sharedPreferences.getString(BOARD_THEME, "DEFAULT"));
            if (boardTheme == null) boardTheme = BoardTheme.DEFAULT;
        } catch (Exception e) {
            Log.d(TAG, "getBoardTheme: Error in finding BoardTheme");
            return BoardTheme.DEFAULT;
        }
        return boardTheme;
    }

    /**
     * Saves the board theme
     *
     * @param boardTheme <code>BoardTheme</code> to be saved
     */
    public void setBoardTheme(@NonNull BoardTheme boardTheme) {
        editor.putString(BOARD_THEME, boardTheme.toString());
        editor.commit();
    }

    /**
     * Serializes and saves game objects
     *
     * @param boardModel      BoardModel object
     * @param pgn             PGN object
     * @param boardModelStack BoardModel stack object
     * @param FENs            FENs stack object
     */
    public void saveData(BoardModel boardModel, PGN pgn, Stack<BoardModel> boardModelStack, Stack<String> FENs) {
        saveObject(DataManager.BOARD_FILE, boardModel);
        saveObject(DataManager.PGN_FILE, pgn);
        saveObject(DataManager.STACK_FILE, boardModelStack);
        saveObject(DataManager.FENS_LIST_FILE, FENs);
        editor.commit();
    }

    public void setInt(String label, int i) {
        editor.putInt(label, i);
        editor.commit();
    }

    public void setLong(String label, long l) {
        editor.putLong(label, l);
        editor.commit();
    }

    public void setBoolean(String label, boolean b) {
        editor.putBoolean(label, b);
        editor.commit();
    }

    public void setString(String label, String s) {
        editor.putString(label, s);
        editor.commit();
    }

    public boolean getBoolean(String label) {
        return sharedPreferences.getBoolean(label, false);
    }

    public String getString(String label) {
        return sharedPreferences.getString(label, null);
    }

    public int getInt(String label) {
        return sharedPreferences.getInt(label, Integer.MIN_VALUE);
    }

    public long getLong(String label) {
        return sharedPreferences.getLong(label, Long.MIN_VALUE);
    }
}