package com.drdedd.simplechess_temp;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NavUtils;

import com.drdedd.simplechess_temp.GameData.BoardTheme;
import com.drdedd.simplechess_temp.GameData.DataManager;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private DataManager dataManager;
    private EditText whiteName, blackName;
    private SwitchCompat fullScreenToggle, cheatToggle, invertBlackSVGToggle, computeLegalMovesToggle;
    private Spinner themeSpinnerMenu;
    private final BoardTheme[] themes = BoardTheme.getValues();
    private boolean fullScreen, cheatMode, invertBlackSVGs, computeLegalMoves;
    private String[] items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Settings");
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_settings);

        dataManager = new DataManager(this);
        fullScreen = dataManager.isFullScreen();
        cheatMode = dataManager.cheatModeEnabled();
        invertBlackSVGs = dataManager.invertBlackSVGEnabled();
        computeLegalMoves = dataManager.computeLegalMovesEnabled();

        if (fullScreen)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        items = new String[themes.length];
        int i = 0;
        for (BoardTheme theme : themes)
            items[i++] = theme.getThemeName();

        themeSpinnerMenu = findViewById(R.id.themeSpinnerMenu);
        whiteName = findViewById(R.id.whiteName);
        blackName = findViewById(R.id.blackName);
        fullScreenToggle = findViewById(R.id.fullScreenToggle);
        cheatToggle = findViewById(R.id.cheatToggle);
        invertBlackSVGToggle = findViewById(R.id.invertBlackSVGToggle);
        computeLegalMovesToggle = findViewById(R.id.computeLegalMovesToggle);

        initialize();

        fullScreenToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> fullScreen = isChecked);
        cheatToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> cheatMode = isChecked);
        invertBlackSVGToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> invertBlackSVGs = isChecked);
        computeLegalMovesToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> computeLegalMoves = isChecked);
        themeSpinnerMenu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                dataManager.setBoardTheme(Objects.requireNonNull(themes[position]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void initialize() {
        fullScreenToggle.setChecked(fullScreen);
        whiteName.setText(dataManager.getWhite());
        blackName.setText(dataManager.getBlack());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        themeSpinnerMenu.setAdapter(adapter);
        themeSpinnerMenu.setSelection(dataManager.getBoardTheme().ordinal());
        cheatToggle.setChecked(cheatMode);
        invertBlackSVGToggle.setChecked(invertBlackSVGs);
        computeLegalMovesToggle.setChecked(computeLegalMoves);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataManager.setFullScreen(fullScreen);
        String white = whiteName.getText().toString().trim();
        if (white.equals("")) white = "White";
        String black = blackName.getText().toString().trim();
        if (black.equals("")) black = "Black";
        dataManager.saveWhiteBlack(white, black);
        dataManager.setCheatMode(cheatMode);
        dataManager.setInvertBlackSVG(invertBlackSVGs);
        dataManager.setComputeLegalMoves(computeLegalMoves);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}