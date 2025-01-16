package com.drdedd.simplichess.fragments;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.databinding.FragmentOpeningsBinding;
import com.drdedd.simplichess.game.GameLogic;
import com.drdedd.simplichess.game.Openings;
import com.drdedd.simplichess.game.gameData.Annotation;
import com.drdedd.simplichess.game.pgn.PGN;
import com.drdedd.simplichess.interfaces.GameUI;
import com.drdedd.simplichess.interfaces.PGNRecyclerViewInterface;
import com.drdedd.simplichess.misc.MiscMethods;
import com.drdedd.simplichess.views.CompactBoard;

import java.util.ArrayList;
import java.util.List;

public class OpeningsFragment extends Fragment implements GameUI, PGNRecyclerViewInterface {
    private static final String TAG = "OpeningsFragment";
    private FragmentOpeningsBinding binding;
    private Openings openings;
    private GameLogic gameLogic;
    private RecyclerView openingMoves;
    private CompactBoard openingsBoard;
    private ArrayList<Pair<String, ArrayList<String>>> allOpenings;
    private LinearLayoutManager linearLayoutManager;
    private int position, previousPosition;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOpeningsBinding.inflate(inflater, container, false);

        openings = Openings.getInstance(requireContext());
        allOpenings = openings.getAllOpenings();

        openingsBoard = binding.openingsBoard;
        openingMoves = binding.openingMoves;

        binding.resetBoard.setOnClickListener(v -> reset());
        binding.undoMove.setOnClickListener(v -> undoMove());
        binding.moveNext.setOnClickListener(v -> moveNext());
        binding.movePrevious.setOnClickListener(v -> movePrevious());
        binding.moveNext.setOnLongClickListener(v -> moveLast());
        binding.movePrevious.setOnLongClickListener(v -> moveFirst());

        gameLogic = new GameLogic(this, requireContext(), openingsBoard.getBoard(), true);

        ArrayList<String> openingNames = new ArrayList<>(List.of(" -- Select an opening -- "));
        for (Pair<String, ArrayList<String>> pair : allOpenings) openingNames.add(pair.first);
        linearLayoutManager = new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false);
        linearLayoutManager.setStackFromEnd(true);
        binding.openingsList.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, openingNames));
        binding.openingsList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                loadOpening(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                adapterView.setSelection(0);
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reset();
    }

    public void reset() {
        gameLogic.reset();
        position = 0;
        openingsBoard.setOpening("", "");
        updateAdapter();
    }

    private void undoMove() {
        gameLogic.undoLastMove();
        previousPosition = position;
        position = gameLogic.getFENs().size() - 1;
    }

    private void loadOpening(int i) {
        if (i == 0) return;
        Log.d(TAG, "loadOpening: Loading " + allOpenings.get(i - 1).first);
        ArrayList<String> moves = allOpenings.get(i - 1).second;
        gameLogic.reset();
        for (String move : moves) {
            Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> p = MiscMethods.uciToRowCol(move);
            gameLogic.move(p.first.first, p.first.second, p.second.first, p.second.second);
        }
    }

    @Override
    public void updateViews() {
        if (gameLogic == null) return;
        int size = gameLogic.getFENs().size();
        previousPosition = position;
        position = size - 1;
        updatePosition();
        updateAdapter();
    }

    private void updatePosition() {
        // Scroll to position and highlight move
        openingMoves.scrollToPosition(position == 0 ? 0 : position - 1);
        RecyclerView.ViewHolder holder = openingMoves.findViewHolderForAdapterPosition(previousPosition - 1);
        if (holder != null)
            holder.itemView.findViewById(R.id.move).setBackgroundResource(R.drawable.pgn_move_bg);
        openingMoves.post(() -> {
            RecyclerView.ViewHolder holder1 = openingMoves.findViewHolderForAdapterPosition(position - 1);
            if (holder1 != null)
                holder1.itemView.findViewById(R.id.move).setBackgroundResource(R.drawable.pgn_move_highlight);
        });

        openingsBoard.setAnnotation(null, -1);
        int size = gameLogic.getFENs().size();
        openingsBoard.setViewOnly(position != size - 1);
        openingsBoard.setPosition(position == size - 1 ? null : gameLogic.getFENs().get(position));
        Log.d(TAG, String.format("updateViews: Position: %d %s%n%s", position, gameLogic.getBoardModelStack().get(position), gameLogic.getFENs().get(position)));
        MiscMethods.setImageButtonEnabled(binding.movePrevious, position > 0);
        MiscMethods.setImageButtonEnabled(binding.moveNext, position < size - 1);
        MiscMethods.setImageButtonEnabled(binding.undoMove, size > 1);

        // Search Opening
        String result = openings.searchOpening(gameLogic.getPGN().getUCIMoves());
        String[] split = result.split(Openings.separator);
        int pos = Integer.parseInt(split[0]);
        if (pos != -1 && split.length == 3) {
            openingsBoard.setOpening(split[1], split[2]);
            gameLogic.getPGN().setLastBookMoveNo(pos);
        } else Log.i(TAG, "updateViews: Opening not found");

        // Set annotation
        if (position == 0) openingsBoard.setAnnotation(null, -1);
        else if (position > 0 && position - 1 <= gameLogic.getPGN().getLastBookMoveNo())
            openingsBoard.setAnnotation(gameLogic.getBoardModelStack().get(position).toSquare, Annotation.BOOK.getResID());
        openingsBoard.clearSelection();
    }

    private void updateAdapter() {
        PGN.PGNRecyclerViewAdapter adapter = new PGN.PGNRecyclerViewAdapter(requireContext(), gameLogic.getPGN(), this);
        openingMoves.setAdapter(adapter);
        openingMoves.setLayoutManager(linearLayoutManager);
        openingMoves.post(() -> openingMoves.scrollToPosition(adapter.getItemCount() - 1));
    }

    @Override
    public void jumpToMove(int position) {
        previousPosition = this.position;
        this.position = position + 1;
        updatePosition();
    }

    private void moveNext() {
        if (position < gameLogic.getFENs().size() - 1) {
            previousPosition = position;
            position++;
            updatePosition();
        }
    }

    private void movePrevious() {
        if (position > 0) {
            previousPosition = position;
            position--;
            updatePosition();
        }
    }

    private boolean moveFirst() {
        previousPosition = position;
        position = 0;
        updatePosition();
        return true;
    }

    private boolean moveLast() {
        previousPosition = position;
        position = gameLogic.getFENs().size() - 1;
        updatePosition();
        return true;
    }

    @Override
    public int getPosition() {
        return position - 1;
    }

    @Override
    public void terminateGame(String termination) {
        Toast.makeText(requireContext(), "Game terminated\nReset the board to continue", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean saveProgress() {
        return false;
    }
}