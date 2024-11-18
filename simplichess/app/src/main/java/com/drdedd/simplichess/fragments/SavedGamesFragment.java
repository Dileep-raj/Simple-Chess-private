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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    private ArrayList<String> savedGames;
    private DataManager dataManager;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSavedGamesBinding.inflate(inflater, container, false);
        dataManager = new DataManager(requireContext());
        savedGames = dataManager.savedGames();
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

        if (savedGames.isEmpty()) savedGamesRecyclerView.setVisibility(View.GONE);
        else {
            binding.noGames.setVisibility(View.GONE);
            GamesRecyclerViewAdapter adapter = new GamesRecyclerViewAdapter(requireContext(), savedGames, this);
            savedGamesRecyclerView.setAdapter(adapter);
            savedGamesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
    }

    /**
     * Delete all PGNs in the library
     */
    private void deleteAll() {
        if (savedGames.isEmpty()) return;
        createDialog("Delete all games?", (d, i) -> {
            boolean result = true;
            for (String file : savedGames) result = result && dataManager.deleteGame(file);
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
            if (dataManager.deleteGame(savedGames.get(position)))
                Toast.makeText(requireContext(), "Game deleted successfully", Toast.LENGTH_SHORT).show();
            refresh();
        });
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
     * Refreshes views visibility and content
     */
    private void refresh() {
        savedGames = dataManager.savedGames();
        if (savedGames.isEmpty()) {
            binding.noGames.setVisibility(View.VISIBLE);
            savedGamesRecyclerView.setVisibility(View.GONE);
        } else {
            GamesRecyclerViewAdapter adapter = new GamesRecyclerViewAdapter(requireContext(), savedGames, this);
            savedGamesRecyclerView.swapAdapter(adapter, true);
        }
    }

    @Override
    public void openGame(int position) {
        Bundle args = new Bundle();
        try {
            args.putString(LoadGameFragment.PGN_CONTENT_KEY, new String(Files.readAllBytes(Paths.get(dataManager.savedGameDir + savedGames.get(position)))));
            args.putBoolean(LoadGameFragment.FILE_EXISTS_KEY, true);
        } catch (IOException e) {
            Log.e(TAG, "openGame: Error while reading pgn:", e);
        }
        navController.popBackStack();
        navController.navigate(R.id.nav_load_game, args);
    }

    /**
     * RecyclerView adapter for saved games
     */
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
            return new GamesViewHolder(LayoutInflater.from(context).inflate(R.layout.rv_saved_game, parent, false), gameRecyclerViewInterface);
        }

        @Override
        public void onBindViewHolder(@NonNull GamesViewHolder holder, int position) {
            String name = games.get(position);

            String result = "";
            try {
                String contents = new String(Files.readAllBytes(Paths.get(dataManager.savedGameDir + games.get(position))));

                if (!contents.isEmpty()) {
                    HashMap<String, String> tagsMap = new HashMap<>();
                    Matcher tags = tagsPattern.matcher(contents);
                    while (tags.find()) {
                        String tag = tags.group();
                        String tagName = tag.substring(1, tag.indexOf(' '));
                        String tagValue = tag.substring(tag.indexOf('"') + 1, tag.lastIndexOf('"'));
                        tagsMap.put(tagName, tagValue);
                    }
                    if (tagsMap.containsKey(PGN.TAG_WHITE) && tagsMap.containsKey(PGN.TAG_BLACK))
                        name = String.format("%s vs %s", tagsMap.get(PGN.TAG_WHITE), tagsMap.get(PGN.TAG_BLACK));

                    if (tagsMap.containsKey(PGN.TAG_TERMINATION))
                        result = tagsMap.get(PGN.TAG_TERMINATION);
                    else if (tagsMap.containsKey(PGN.TAG_RESULT))
                        result = tagsMap.get(PGN.TAG_RESULT);
                }
            } catch (Exception e) {
                Log.e(TAG, "onBindViewHolder: Exception while finding result", e);
            }

            holder.name.setText(name);
            if (result != null) holder.result.setText(result);
        }

        @Override
        public int getItemCount() {
            return games.size();
        }

        /**
         * ViewHolder for saved games RecyclerView adapter
         */
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

                itemView.setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION)
                        gameRecyclerViewInterface.openGame(adapterPosition);
                });
            }
        }
    }
}