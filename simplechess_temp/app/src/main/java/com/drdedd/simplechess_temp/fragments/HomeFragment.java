package com.drdedd.simplechess_temp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.R;
import com.drdedd.simplechess_temp.data.GameStatistics;
import com.drdedd.simplechess_temp.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private NavController navController;
    private static GameStatistics gameStatistics;
    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        DataManager dataManager = new DataManager(requireContext());

        Button btn_continue = binding.btnContinueGame;

        btn_continue.setOnClickListener(v -> startGame(false));
        binding.btnNewGame.setOnClickListener(v -> startGame(true));
        binding.btnExitApp.setOnClickListener(v -> exit_app());
        binding.btnSettings.setOnClickListener(v -> navController.navigate(R.id.nav_settings));
//        binding.btnOpenTest.setOnClickListener(v -> navController.navigate(R.id.nav_test));

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
        args.putBoolean(GameFragment.NEW_GAME_KEY, newGame);
        navController.navigate(R.id.nav_game, args);
        Log.d(TAG, "startGame: Game started");
    }

    public void exit_app() {
        requireActivity().finishAffinity();
    }

    public static String getTAG() {
        return TAG;
    }

    public static void printTime(String TAG, String message, long time, int size) {
        Log.i("TimeCalculated", String.format(Locale.ENGLISH, "%s: Time taken for %s: %,3d ns, Size:%d", TAG, message, time, size));
        gameStatistics.addRecord(message, time, size);
    }
}