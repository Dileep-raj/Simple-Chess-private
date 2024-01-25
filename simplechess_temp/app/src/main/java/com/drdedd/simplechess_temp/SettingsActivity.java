package com.drdedd.simplechess_temp;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NavUtils;

import com.drdedd.simplechess_temp.GameData.BoardTheme;
import com.drdedd.simplechess_temp.GameData.DataManager;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private DataManager dataManager;
    private EditText whiteName, blackName, minutesInput, secondsInput;
    private SwitchCompat fullScreenToggle, cheatToggle, invertBlackSVGToggle, timerToggle;
    private LinearLayout timerInputLayout;
    private Spinner themeSpinnerMenu;
    private final BoardTheme[] themes = BoardTheme.getValues();
    private boolean fullScreen, cheatMode, invertBlackSVGs, timer;
    private int minutes, seconds;
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
        timer = dataManager.isTimerEnabled();
        minutes = dataManager.getTimerMinutes();
        seconds = dataManager.getTimerSeconds();

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

        timerToggle = findViewById(R.id.timerToggle);
        timerInputLayout = findViewById(R.id.timerInputLayout);
        minutesInput = findViewById(R.id.minutesInput);
        minutesInput.setFilters(new InputFilter[]{new InputFilterMinMax(1, 30), new InputFilter.LengthFilter(2)});
        secondsInput = findViewById(R.id.secondsInput);
        secondsInput.setFilters(new InputFilter[]{new InputFilterMinMax(0, 59), new InputFilter.LengthFilter(2)});

        initialize();

        fullScreenToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> fullScreen = isChecked);
        cheatToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> cheatMode = isChecked);
        invertBlackSVGToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> invertBlackSVGs = isChecked);
        timerToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) timerInputLayout.setVisibility(View.VISIBLE);
            else timerInputLayout.setVisibility(View.GONE);
            timer = isChecked;
        });

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
        timerToggle.setChecked(timer);
        minutesInput.setText(String.valueOf(minutes));
        secondsInput.setText(String.valueOf(seconds));
        if (!timerToggle.isChecked()) timerInputLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataManager.setFullScreen(fullScreen);
        String white = whiteName.getText().toString().trim();
        if (white.equals("")) white = "White";
        String black = blackName.getText().toString().trim();
        if (black.equals("")) black = "Black";

        try {
            minutes = Integer.parseInt(minutesInput.getText().toString());
        } catch (NumberFormatException numberFormatException) {
            minutes = 10;
        }
        try {
            seconds = Integer.parseInt(secondsInput.getText().toString());
        } catch (NumberFormatException numberFormatException) {
            seconds = 0;
        }

        dataManager.saveWhiteBlack(white, black);
        dataManager.setCheatMode(cheatMode);
        dataManager.setInvertBlackSVG(invertBlackSVGs);
        dataManager.setTimerEnabled(timer);
        dataManager.setTimerMinutesSeconds(minutes, seconds);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static class InputFilterMinMax implements InputFilter {
        private final int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());
                if (isInRange(min, max, input)) return null;
            } catch (NumberFormatException nfe) {
                Log.d("InputFilterError", "Error:\n" + nfe);
            }
            return "";
        }

        private boolean isInRange(int min, int max, int value) {
            return max > min ? value >= min && value <= max : value >= max && value <= min;
        }
    }
}