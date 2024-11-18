package com.drdedd.simplichess.fragments;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.drdedd.simplichess.databinding.FragmentOpeningsBinding;
import com.drdedd.simplichess.game.GameLogic;
import com.drdedd.simplichess.game.Openings;
import com.drdedd.simplichess.game.gameData.Annotation;
import com.drdedd.simplichess.game.pgn.PGN;
import com.drdedd.simplichess.interfaces.GameFragmentInterface;
import com.drdedd.simplichess.interfaces.PGNRecyclerViewInterface;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class OpeningsFragment extends Fragment implements GameFragmentInterface, PGNRecyclerViewInterface {
    private FragmentOpeningsBinding binding;
    private Openings openings;
    private GameLogic gameLogic;
    private RecyclerView recyclerView;
    private PGN.PGNRecyclerViewAdapter adapter;
    private LinkedHashMap<Integer, Annotation> annotationMap;
    private int position;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOpeningsBinding.inflate(inflater, container, false);
        openings = Openings.getInstance(requireContext());
        annotationMap = new LinkedHashMap<>();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayList<Pair<String, ArrayList<String>>> allOpenings = openings.getAllOpenings();
        gameLogic = new GameLogic(this, requireContext(), binding.openingsBoard.getBoard(), true);
//        binding.moveNext.setOnClickListener(v ->);
    }

    private void resetBoard() {
        gameLogic.reset();
    }

    @Override
    public void updateViews() {
        binding.openingsBoard.getBoard().setViewOnly(position != gameLogic.getPGN().getPlyCount());
        String result = openings.searchOpening(gameLogic.getPGN().getUCIMoves());
        String[] split = result.split(Openings.separator);
        int pos = Integer.parseInt(split[0]);
        if (pos != -1 && split.length == 3) {
            binding.openingsBoard.setOpening(split[1], split[2]);
            gameLogic.getPGN().setLastBookMoveNo(pos);
            binding.openingsBoard.getBoard().annotation = Annotation.BOOK.getResID();
            annotationMap.put(pos, Annotation.BOOK);
        }
    }

    private void updateAdapter() {
        adapter = new PGN.PGNRecyclerViewAdapter(requireContext(), gameLogic.getPGN(), this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void terminateGame(String termination) {
    }

    @Override
    public boolean saveProgress() {
        return false;
    }

    @Override
    public void jumpToMove(int position) {
        this.position = position;
        updateViews();
    }

    private void moveNext() {
        position++;
    }

    private void movePrevious() {
        position--;
    }

    @Override
    public int getPosition() {
        return position;
    }
}