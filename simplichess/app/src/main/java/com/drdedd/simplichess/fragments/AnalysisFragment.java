package com.drdedd.simplichess.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drdedd.simplichess.game.GameLogic;
import com.drdedd.simplichess.databinding.FragmentAnalysisBinding;
import com.drdedd.simplichess.interfaces.GameFragmentInterface;

public class AnalysisFragment extends Fragment implements GameFragmentInterface {

    private FragmentAnalysisBinding binding;
    private GameLogic gameLogic;
    private boolean isPuzzle;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAnalysisBinding.inflate(inflater, container, false);
        gameLogic = new GameLogic(this, requireContext(), binding.analysisBoard, true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //TODO implement game analysis features
    }

    @Override
    public void updateViews() {
    }

    @Override
    public void terminateGame(String termination) {
    }

    @Override
    public boolean saveProgress() {
        return false;
    }
}