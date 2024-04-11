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

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    public static final String NEW_GAME_KEY = "NewGame";
    private NavController navController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DataManager dataManager = new DataManager(getContext());

        Button btn_continue = view.findViewById(R.id.btn_continue_game);

        btn_continue.setOnClickListener(v -> startGame(false));
        view.findViewById(R.id.btn_new_game).setOnClickListener(v -> startGame(true));
        view.findViewById(R.id.btn_exit_app).setOnClickListener(v -> exit_app());
        view.findViewById(R.id.btn_settings).setOnClickListener(v -> navController.navigate(R.id.nav_settings));
//        view.findViewById(R.id.btn_open_test).setOnClickListener(v -> navController.navigate(R.id.nav_test));

        if (dataManager.readObject(DataManager.boardFile) == null || dataManager.readObject(DataManager.PGNFile) == null || dataManager.readObject(DataManager.stackFile) == null)
            btn_continue.setVisibility(View.GONE);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        navController = Navigation.findNavController(requireActivity(), R.id.main_fragment);
    }

    public void startGame(boolean newGame) {
        Bundle args = new Bundle();
        args.putBoolean(NEW_GAME_KEY, newGame);
        navController.navigate(R.id.nav_game, args);
        Log.d(TAG, "startGame: Game started");
    }

    public void exit_app() {
        requireActivity().finishAffinity();
    }

    public static String getTAG() {
        return TAG;
    }
}