package com.drdedd.simplichess.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.data.DataManager;
import com.drdedd.simplichess.data.GameStatistics;
import com.drdedd.simplichess.databinding.FragmentHomeBinding;
import com.drdedd.simplichess.dialogs.SelectGameModeDialog;
import com.drdedd.simplichess.misc.Constants;

import java.util.ArrayList;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private NavController navController;
    private static GameStatistics gameStatistics;
    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
//        binding.btnOpenTest.setVisibility(View.GONE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.show();
        }
        navController = Navigation.findNavController(view);
        DataManager dataManager = new DataManager(requireContext());

        Button btn_continue = binding.btnContinueGame;

        btn_continue.setOnClickListener(v -> startGame(false));
        binding.btnNewGame.setOnClickListener(v -> startGame(true));
        binding.btnExitApp.setOnClickListener(v -> exit_app());
        binding.btnSettings.setOnClickListener(v -> navController.navigate(R.id.nav_settings));
        binding.btnOpenTest.setOnClickListener(v -> navController.navigate(R.id.nav_test));

        gameStatistics = new GameStatistics(requireContext());
        long start = System.nanoTime();
        Object boardObject = dataManager.readObject(DataManager.BOARD_FILE), PGNObject = dataManager.readObject(DataManager.PGN_FILE), stackObject = dataManager.readObject(DataManager.STACK_FILE);
        long end = System.nanoTime();
        if (boardObject == null || PGNObject == null || stackObject == null) {
            if (dataManager.deleteGameFiles())
                Toast.makeText(requireContext(), "Couldn't load previous game", Toast.LENGTH_SHORT).show();
            btn_continue.setVisibility(View.GONE);
        }
        printTime(TAG, "reading game objects", end - start, -1);
        ArrayList<String> names = gameStatistics.getNames();
        Log.i(TAG, "onViewCreated: Names of records: " + names.toString());
    }

    public void startGame(boolean newGame) {
        Bundle args = new Bundle();
        args.putBoolean(Constants.NEW_GAME_KEY, newGame);
        if (newGame) {
            SelectGameModeDialog dialog = new SelectGameModeDialog(requireContext());
            dialog.setOnDismissListener(d -> {
                if (dialog.isStart()) {
                    args.putBoolean(Constants.SINGLE_PLAYER, dialog.isSinglePlayer());
                    if (dialog.isSinglePlayer())
                        args.putBoolean(Constants.PLAY_AS_WHITE, dialog.isPlayAsWhite());
                    navController.navigate(R.id.nav_game, args);
                    Log.d(TAG, "startGame: Game started");
                }
            });
            dialog.show();
        } else {
            args.putBoolean(Constants.PLAY_AS_WHITE, true);
            navController.navigate(R.id.nav_game, args);
            Log.d(TAG, "startGame: Game started");
        }
    }

    public void exit_app() {
        requireActivity().finishAffinity();
    }

    public static String getTAG() {
        return TAG;
    }

    public static void printTime(String TAG, String message, long time, int size) {
//        String s = String.valueOf(time), s1 = String.valueOf(size);
//        String decor = "=".repeat(message.length() + 47 + s.length() + TAG.length() + s1.length());
        int sec = (int) (time / 1000000000);
        int min = sec / 60;
//        Log.i("TimeCalculated", String.format(Locale.ENGLISH, "%s: Time taken for %s: %,3d ns, Size:%d", TAG, message, time, size));
        Log.i("TimeCalculated", String.format(Locale.ENGLISH, "%s: Time taken for %s : %02dm %02ds %01dms (%d ns) Size: %d", TAG, message, min, sec % 60, time / 1000000, time, size));
        gameStatistics.addRecord(message, time, size);
    }
}