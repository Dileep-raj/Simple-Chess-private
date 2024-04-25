package com.drdedd.simplechess_temp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drdedd.simplechess_temp.BoardModel;
import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.PGN;
import com.drdedd.simplechess_temp.R;
import com.drdedd.simplechess_temp.databinding.FragmentSavedGamesBinding;
import com.drdedd.simplechess_temp.interfaces.GameRecyclerViewInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

public class SavedGamesFragment extends Fragment implements GameRecyclerViewInterface {
    private FragmentSavedGamesBinding binding;
    private ArrayList<String> savedGames;
    private DataManager dataManager;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSavedGamesBinding.inflate(inflater, container, false);
        savedGames = new DataManager(requireContext()).savedGames();
        dataManager = new DataManager(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        RecyclerView savedGamesRecyclerView = binding.savedGamesRecyclerView;

        if (savedGames.isEmpty()) savedGamesRecyclerView.setVisibility(View.GONE);
        else {
            binding.noGames.setVisibility(View.GONE);
            GamesRecyclerViewAdapter adapter = new GamesRecyclerViewAdapter(requireContext(), savedGames, this);
            savedGamesRecyclerView.setAdapter(adapter);
            savedGamesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
    }

    @Override
    public void deleteGame(int position) {
        if (dataManager.deleteGameFiles(savedGames.get(position)))
            Toast.makeText(requireContext(), "Game deleted successfully", Toast.LENGTH_SHORT).show();
        NavController navController = Navigation.findNavController(requireActivity(), R.id.main_fragment);
        navController.popBackStack();
        navController.navigate(R.id.nav_saved_games);
    }

    @Override
    public void openGame(int position) {
        Stack<BoardModel> boardModels = (Stack<BoardModel>) dataManager.readObject(savedGames.get(position) + File.separator + DataManager.stackFile);
        PGN pgn = (PGN) dataManager.readObject(savedGames.get(position) + File.separator + DataManager.PGNFile);

        if (pgn == null || boardModels == null)
            Toast.makeText(requireContext(), "Game can't be loaded", Toast.LENGTH_SHORT).show();

        Bundle args = new Bundle();
        args.putSerializable(LoadGameFragment.LOAD_GAME_KEY, boardModels);
        args.putSerializable(LoadGameFragment.LOAD_PGN_KEY, pgn);
        navController.popBackStack();
        navController.navigate(R.id.nav_load_game, args);
    }

    static class GamesRecyclerViewAdapter extends RecyclerView.Adapter<GamesRecyclerViewAdapter.GamesViewHolder> {
        private final Context context;
        private final ArrayList<String> games;
        private final GameRecyclerViewInterface gameRecyclerViewInterface;
        private final DataManager dataManager;

        public GamesRecyclerViewAdapter(Context context, ArrayList<String> games, GameRecyclerViewInterface gameRecyclerViewInterface) {
            this.context = context;
            this.games = games;
            this.gameRecyclerViewInterface = gameRecyclerViewInterface;
            dataManager = new DataManager(context);
        }

        @NonNull
        @Override
        public GamesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new GamesViewHolder(LayoutInflater.from(context).inflate(R.layout.recycler_view_saved_game_row, parent, false), gameRecyclerViewInterface);
        }

        @Override
        public void onBindViewHolder(@NonNull GamesViewHolder holder, int position) {
            PGN pgn = (PGN) dataManager.readObject(games.get(position) + File.separator + DataManager.PGNFile);
            if (pgn != null) {
                holder.name.setText(String.format("%s vs %s", pgn.getWhite(), pgn.getBlack()));
                holder.result.setText(pgn.getTermination());
            }
//            holder.name.setText(games.get(position));
        }

        @Override
        public int getItemCount() {
            return games.size();
        }

        static class GamesViewHolder extends RecyclerView.ViewHolder {
            TextView name, result;

            public GamesViewHolder(@NonNull View itemView, GameRecyclerViewInterface gameRecyclerViewInterface) {
                super(itemView);
                name = itemView.findViewById(R.id.name);
                result = itemView.findViewById(R.id.result);

                itemView.findViewById(R.id.delete_game).setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION)
                        gameRecyclerViewInterface.deleteGame(adapterPosition);
                });

                itemView.findViewById(R.id.open_game).setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION)
                        gameRecyclerViewInterface.openGame(adapterPosition);
                });
            }
        }
    }
}