package com.drdedd.simplechess_temp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.R;
import com.drdedd.simplechess_temp.data.GameStatistics;

import java.util.ArrayList;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private NavController navController;
    private static GameStatistics gameStatistics;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        DataManager dataManager = new DataManager(requireContext());

        Button btn_continue = view.findViewById(R.id.btn_continue_game);

        btn_continue.setOnClickListener(v -> startGame(false));
        view.findViewById(R.id.btn_new_game).setOnClickListener(v -> startGame(true));
        view.findViewById(R.id.btn_exit_app).setOnClickListener(v -> exit_app());
        view.findViewById(R.id.btn_settings).setOnClickListener(v -> navController.navigate(R.id.nav_settings));
//        view.findViewById(R.id.btn_open_test).setOnClickListener(v -> navController.navigate(R.id.nav_test));

        gameStatistics = new GameStatistics(requireContext());
        long start = System.nanoTime();
        Object boardObject = dataManager.readObject(DataManager.boardFile), PGNObject = dataManager.readObject(DataManager.PGNFile), stackObject = dataManager.readObject(DataManager.stackFile);
        long end = System.nanoTime();
        if (boardObject == null || PGNObject == null || stackObject == null)
            btn_continue.setVisibility(View.GONE);
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