package com.drdedd.simplichess.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.databinding.FragmentTestBinding;
import com.drdedd.simplichess.game.GameLogic;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.interfaces.GameUI;
import com.drdedd.simplichess.misc.MiscMethods;
import com.drdedd.simplichess.views.CompactBoard;

public class TestFragment extends Fragment implements GameUI {
    private static final String TAG = "TestFragment";
    private FragmentTestBinding binding;
    private GameLogic gameLogic;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.hide();
        }
//        binding.btnTest.setOnClickListener(v -> test());
        binding.uciInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                test(String.valueOf(s));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.resetBoard.setOnClickListener(v -> gameLogic.reset());
        binding.copyPGN.setOnClickListener(v -> MiscMethods.shareContent(requireContext(), "PGN", gameLogic.getPGN().toString()));
        CompactBoard compactBoard = binding.compactBoard;
        gameLogic = new GameLogic(this, requireContext(), compactBoard.getBoard(), true);
        gameLogic.setBotPlayer(Player.BLACK);
        compactBoard.setToggleSizeButton(binding.shrinkExpandButton, R.drawable.ic_shrink, R.drawable.ic_expand);
        compactBoard.getBoard().setAnalysis(true);
    }

    @SuppressLint("SetTextI18n")
    private void test(String s) {
        if (s.length() < 4) return;
        binding.compactBoard.getBoard().addArrow(s.substring(0, 4));
    }

    @Override
    public void updateViews() {
    }

    @Override
    public void terminateGame(String termination) {
        Toast.makeText(requireContext(), termination, Toast.LENGTH_LONG).show();
        Toast.makeText(requireContext(), "Game terminated\nReset to continue", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean saveProgress() {
        return false;
    }

    public static String getTAG() {
        return TAG;
    }
}