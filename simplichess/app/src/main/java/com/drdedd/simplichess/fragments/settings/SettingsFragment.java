package com.drdedd.simplichess.fragments.settings;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.drdedd.simplichess.databinding.FragmentSettingsBinding;
import com.drdedd.simplichess.game.gameData.BoardTheme;
import com.drdedd.simplichess.views.ChessBoard;

public class SettingsFragment extends Fragment {

    private final static String TAG = "SettingsFragment";
    private FragmentSettingsBinding binding;
    private ChessBoard previewBoard;
    private EditText whiteName, blackName, minutesInput, secondsInput;
    private SwitchCompat fullScreenToggle, cheatToggle, invertBlackSVGToggle, timerToggle, vibrationToggle, animationToggle, soundToggle, backgroundImageToggle;
    private LinearLayout timerInputLayout;
    private Spinner themeSpinnerMenu;
    private final BoardTheme[] themes = BoardTheme.getValues();
    private String[] items;
    private SettingsViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        whiteName = binding.whiteName;
        blackName = binding.blackName;
        minutesInput = binding.minutesInput;
        secondsInput = binding.secondsInput;
        fullScreenToggle = binding.fullScreenToggle;
        cheatToggle = binding.cheatToggle;
        invertBlackSVGToggle = binding.invertBlackSVGToggle;
        timerToggle = binding.timerToggle;
        vibrationToggle = binding.vibrationToggle;
        animationToggle = binding.animationToggle;
        soundToggle = binding.soundToggle;
        backgroundImageToggle = binding.useBoardBackground;
        previewBoard = binding.previewBoard;

        timerInputLayout = binding.timerInputLayout;
        themeSpinnerMenu = binding.themeSpinnerMenu;

        items = new String[themes.length];
        int i = 0;
        for (BoardTheme theme : themes) items[i++] = theme.toString();

        timerToggle.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) timerInputLayout.setVisibility(View.VISIBLE);
            else timerInputLayout.setVisibility(View.GONE);
            viewModel.setTimer(isChecked);
        });

        themeSpinnerMenu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                viewModel.setBoardTheme(themes[position]);
                updatePreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        fullScreenToggle.setOnCheckedChangeListener((button, b) -> viewModel.setFullScreen(b));
        cheatToggle.setOnCheckedChangeListener((button, b) -> viewModel.setCheatMode(b));
        invertBlackSVGToggle.setOnCheckedChangeListener((button, b) -> {
            viewModel.setInvertBlackSVGs(b);
            updatePreview();
        });
        vibrationToggle.setOnCheckedChangeListener((button, b) -> viewModel.setVibration(b));
        animationToggle.setOnCheckedChangeListener((button, b) -> viewModel.setAnimation(b));
        soundToggle.setOnCheckedChangeListener((button, b) -> viewModel.setSound(b));
        backgroundImageToggle.setOnCheckedChangeListener((button, b) -> {
            viewModel.setBackgroundImage(b);
            updatePreview();
        });

        return binding.getRoot();
    }

    private void updatePreview() {
        previewBoard.setTheme(viewModel.getBoardTheme());
        previewBoard.setInvertBlackPieces(viewModel.isInvertBlackSVGs());
        previewBoard.setBoardImage(viewModel.isBackgroundImage());
        previewBoard.invalidate();

        binding.ThemeMenuLayout.setVisibility(viewModel.isBackgroundImage() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        minutesInput.setFilters(new InputFilter[]{new InputFilterMinMax(1, 30), new InputFilter.LengthFilter(2)});
        secondsInput.setFilters(new InputFilter[]{new InputFilterMinMax(0, 59), new InputFilter.LengthFilter(2)});
        viewModel.initializeData(requireContext());
        initializeData();
    }

    private void initializeData() {
        previewBoard.setData(viewModel, true);
        previewBoard.setSelection(5, 4);
        previewBoard.setAllLegalMoves(viewModel.getLegalMoves());
        fullScreenToggle.setChecked(viewModel.isFullScreen());
        whiteName.setText(viewModel.getWhiteName());
        blackName.setText(viewModel.getBlackName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, items);
        themeSpinnerMenu.setAdapter(adapter);
        themeSpinnerMenu.setSelection(viewModel.getBoardTheme().ordinal());
        cheatToggle.setChecked(viewModel.isCheatMode());
        invertBlackSVGToggle.setChecked(viewModel.isInvertBlackSVGs());
        timerToggle.setChecked(viewModel.isTimer());
        vibrationToggle.setChecked(viewModel.getVibration());
        animationToggle.setChecked(viewModel.getAnimation());
        soundToggle.setChecked(viewModel.getSound());
        backgroundImageToggle.setChecked(viewModel.isBackgroundImage());

        minutesInput.setText(String.valueOf(viewModel.getMinutes()));
        secondsInput.setText(String.valueOf(viewModel.getSeconds()));
        if (!timerToggle.isChecked()) timerInputLayout.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        int minutes, seconds;
        String white = whiteName.getText().toString().trim();
        if (white.isEmpty()) white = "White";
        String black = blackName.getText().toString().trim();
        if (black.isEmpty()) black = "Black";

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
        viewModel.setWhiteName(white);
        viewModel.setBlackName(black);
        viewModel.setMinutes(minutes);
        viewModel.setSeconds(seconds);
        viewModel.updateSettings();

        if (viewModel.isRestartActivity()) {
            requireActivity().finish();
            startActivity(requireActivity().getIntent());
        }
    }

    public static String getTAG() {
        return TAG;
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
                Log.e("InputFilterError", "Error in filter:\n", nfe);
            }
            return "";
        }

        private boolean isInRange(int min, int max, int value) {
            return max > min ? value >= min && value <= max : value >= max && value <= min;
        }
    }
}