package com.drdedd.simplichess.fragments;

import static com.drdedd.simplichess.data.Regexes.tagsPattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.data.DataManager;
import com.drdedd.simplichess.databinding.FragmentSavedGamesBinding;
import com.drdedd.simplichess.game.pgn.PGN;
import com.drdedd.simplichess.interfaces.GameRecyclerViewInterface;
import com.drdedd.simplichess.misc.Constants;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;

/**
 * {@inheritDoc}
 * Fragment to load or delete games from PGN library
 */
public class SavedGamesFragment extends Fragment implements GameRecyclerViewInterface {
    private final static String TAG = "SavedGamesFragment";
    private FragmentSavedGamesBinding binding;
    private RecyclerView savedGamesRecyclerView;
    private ArrayList<SavedGame> savedGames;
    private DataManager dataManager;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSavedGamesBinding.inflate(inflater, container, false);
        dataManager = new DataManager(requireContext());
        loadGames();
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.saved_games_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.delete_all_saved_games) {
                    deleteAll();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
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
        savedGamesRecyclerView = binding.savedGamesRecyclerView;

        savedGamesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        refresh();
    }

    /**
     * Refreshes views visibility and content
     */
    private void refresh() {
        loadGames();
        if (savedGames.isEmpty()) {
            binding.noGames.setVisibility(View.VISIBLE);
            savedGamesRecyclerView.setVisibility(View.GONE);
        } else {
            binding.noGames.setVisibility(View.GONE);
            savedGamesRecyclerView.setVisibility(View.VISIBLE);
            GamesRecyclerViewAdapter adapter = new GamesRecyclerViewAdapter(requireContext(), savedGames, this);
            savedGamesRecyclerView.swapAdapter(adapter, true);
        }
    }

    private void loadGames() {
        savedGames = new ArrayList<>();
        ArrayList<String> games = dataManager.savedGames();
        Collections.reverse(games);
        for (String fileName : games) {
            String result = "", date = "?", title = fileName, pgn;
            try {
                pgn = new String(Files.readAllBytes(Paths.get(dataManager.savedGameDir + fileName)));

                if (!pgn.isEmpty()) {
                    HashMap<String, String> tagsMap = new HashMap<>();
                    Matcher tags = tagsPattern.matcher(pgn);
                    while (tags.find()) {
                        String tag = tags.group();
                        String tagName = tag.substring(1, tag.indexOf(' '));
                        String tagValue = tag.substring(tag.indexOf('"') + 1, tag.lastIndexOf('"'));
                        tagsMap.put(tagName, tagValue);
                    }
                    if (tagsMap.containsKey(PGN.TAG_WHITE) && tagsMap.containsKey(PGN.TAG_BLACK))
                        title = String.format("%s vs %s", tagsMap.get(PGN.TAG_WHITE), tagsMap.get(PGN.TAG_BLACK));

                    if (tagsMap.containsKey(PGN.TAG_TERMINATION))
                        result = tagsMap.get(PGN.TAG_TERMINATION);
                    else result = tagsMap.getOrDefault(PGN.TAG_RESULT, "");

                    date = tagsMap.getOrDefault(PGN.TAG_DATE, "?");
                }
            } catch (Exception e) {
                Log.e(TAG, "onBindViewHolder: Exception while finding result", e);
            }
            savedGames.add(new SavedGame(fileName, title, date, result));
        }
    }

    /**
     * Delete all PGNs in the library
     */
    private void deleteAll() {
        if (savedGames.isEmpty()) return;
        createDialog("Delete all games?", (d, i) -> {
            boolean result = true;
            for (SavedGame file : savedGames)
                result = result && dataManager.deleteGame(file.fileName);
            if (result) {
                Log.d(TAG, String.format("deleteAll: Deleted all %d files", savedGames.size()));
                Toast.makeText(requireContext(), "All games deleted", Toast.LENGTH_SHORT).show();
            }
            refresh();
        });
    }

    @Override
    public void deleteGame(int position) {
        createDialog("Delete selected game?", (d, i) -> {
            if (dataManager.deleteGame(savedGames.get(position).fileName))
                Toast.makeText(requireContext(), "Game deleted successfully", Toast.LENGTH_SHORT).show();
            refresh();
        });
    }

    @Override
    public void openGame(int position) {
        Bundle args = new Bundle();
        try {
            args.putString(Constants.PGN_CONTENT_KEY, new String(Files.readAllBytes(Paths.get(dataManager.savedGameDir + savedGames.get(position).fileName))));
            args.putBoolean(Constants.FILE_EXISTS_KEY, true);
        } catch (Exception e) {
            Log.e(TAG, "openGame: Error while reading pgn:", e);
        }
        navController.popBackStack();
        navController.navigate(R.id.nav_load_game, args);
    }

    private void createDialog(String title, DialogInterface.OnClickListener positive) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        AlertDialog dialog = builder.setTitle(title).setPositiveButton("Yes", positive).setNegativeButton("No", (d, i) -> {
        }).create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.default_text_color));
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.default_text_color));
        });
        dialog.show();
    }

    /**
     * RecyclerView adapter for saved games
     */
    static class GamesRecyclerViewAdapter extends RecyclerView.Adapter<GamesRecyclerViewAdapter.GamesViewHolder> {
        private final Context context;
        private final ArrayList<SavedGame> games;
        private final GameRecyclerViewInterface gameRecyclerViewInterface;

        private GamesRecyclerViewAdapter(Context context, ArrayList<SavedGame> games, GameRecyclerViewInterface gameRecyclerViewInterface) {
            this.context = context;
            this.games = games;
            this.gameRecyclerViewInterface = gameRecyclerViewInterface;
        }

        @NonNull
        @Override
        public GamesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new GamesViewHolder(LayoutInflater.from(context).inflate(R.layout.rv_saved_game, parent, false), gameRecyclerViewInterface);
        }

        @Override
        public void onBindViewHolder(@NonNull GamesViewHolder holder, int position) {
            SavedGame savedGame = games.get(position);
            holder.date.setText(savedGame.date);
            holder.result.setText(savedGame.result);
            holder.name.setText(savedGame.title);
        }

        @Override
        public int getItemCount() {
            return games.size();
        }

        /**
         * ViewHolder for saved games RecyclerView adapter
         */
        static class GamesViewHolder extends RecyclerView.ViewHolder {
            private final TextView name, result, date;

            private GamesViewHolder(@NonNull View itemView, GameRecyclerViewInterface gameRecyclerViewInterface) {
                super(itemView);
                name = itemView.findViewById(R.id.name);
                result = itemView.findViewById(R.id.result);
                date = itemView.findViewById(R.id.savedDate);

                itemView.findViewById(R.id.delete_game).setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION)
                        gameRecyclerViewInterface.deleteGame(adapterPosition);
                });

                itemView.setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION)
                        gameRecyclerViewInterface.openGame(adapterPosition);
                });
            }
        }
    }

    static class SavedGame {
        private final String fileName, title, date, result;

        private SavedGame(String fileName, String title, String date, String result) {
            this.fileName = fileName;
            this.title = title;
            this.date = date;
            this.result = result;
        }
    }
}