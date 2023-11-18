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

import java.util.HashMap;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private DataManager dataManager;
    private EditText whiteName, blackName;
    private SwitchCompat fullScreenToggle;
    private Spinner themeSpinnerMenu;
    private final BoardTheme[] themes = BoardTheme.getValues();
    boolean fullScreen;
    private String[] items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Settings");
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_settings);

        dataManager = new DataManager(this);
        fullScreen = dataManager.isFullScreen();

        if (fullScreen)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        items = new String[themes.length];
        int i = 0;
        for (BoardTheme theme : themes)
            items[i++] = theme.toString();

        themeSpinnerMenu = findViewById(R.id.themeSpinnerMenu);
        whiteName = findViewById(R.id.whiteName);
        blackName = findViewById(R.id.blackName);
        fullScreenToggle = findViewById(R.id.fullScreenToggle);

        initialize();

        fullScreenToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> fullScreen = isChecked);
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

    public void initialize() {

        fullScreenToggle.setChecked(fullScreen);
        whiteName.setText(dataManager.getWhite());
        blackName.setText(dataManager.getBlack());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        themeSpinnerMenu.setAdapter(adapter);
        themeSpinnerMenu.setSelection(dataManager.getBoardTheme().ordinal());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataManager.setFullScreen(fullScreen);
        dataManager.saveWhiteBlack(whiteName.getText().toString(), blackName.getText().toString());
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