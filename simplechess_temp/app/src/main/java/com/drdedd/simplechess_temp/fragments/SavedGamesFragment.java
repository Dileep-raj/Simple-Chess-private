package com.drdedd.simplechess_temp.fragments;

import static com.drdedd.simplechess_temp.data.Regexes.tagsPattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;
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
import androidx.annotation.RequiresApi;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.PGN;
import com.drdedd.simplechess_temp.R;
import com.drdedd.simplechess_temp.databinding.FragmentSavedGamesBinding;
import com.drdedd.simplechess_temp.interfaces.GameRecyclerViewInterface;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

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
        savedGames = new DataManager(requireContext()).savedGames();
        dataManager = new DataManager(requireContext());
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

    private void deleteAll() {
        if (savedGames.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        AlertDialog dialog = builder.setTitle("Delete all games?").setPositiveButton("Yes", (d, i) -> {
            boolean result = true;
            for (String file : savedGames) result = result && dataManager.deleteGame(file);
            if (result) {
                Log.d(TAG, String.format("deleteAll: Deleted all %d files", savedGames.size()));
                Toast.makeText(requireContext(), "All games deleted", Toast.LENGTH_SHORT).show();
            }
            refresh();
        }).setNegativeButton("No", (d, i) -> {
        }).create();

        dialog.setOnShowListener(d -> {
            Resources resources = requireContext().getResources();
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.default_text_color));
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.default_text_color));
        });
        dialog.show();
    }

    @Override
    public void deleteGame(int position) {
        if (dataManager.deleteGame(savedGames.get(position)))
            Toast.makeText(requireContext(), "Game deleted successfully", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void refresh() {
        savedGames = dataManager.savedGames();
        if (savedGames.isEmpty()) {
            binding.noGames.setVisibility(View.VISIBLE);
            binding.savedGamesRecyclerView.setVisibility(View.GONE);
        } else {
            GamesRecyclerViewAdapter adapter = new GamesRecyclerViewAdapter(requireContext(), savedGames, this);
            savedGamesRecyclerView.swapAdapter(adapter, true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void openGame(int position) {
//        Toast.makeText(requireContext(), "Game can't be loaded", Toast.LENGTH_SHORT).show();

        Bundle args = new Bundle();
        try {
            args.putString(LoadGameFragment.PGN_FILE_KEY, new String(Files.readAllBytes(Paths.get(dataManager.savedGameDir + savedGames.get(position)))));
        } catch (IOException e) {
            Log.e(TAG, "openGame: Error while reading pgn:", e);
        }
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

        @RequiresApi(api = Build.VERSION_CODES.O)
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
                    if (tagsMap.containsKey(PGN.WHITE_TAG) && tagsMap.containsKey(PGN.BLACK_TAG))
                        name = String.format("%s vs %s", tagsMap.get(PGN.WHITE_TAG), tagsMap.get(PGN.BLACK_TAG));

                    if (tagsMap.containsKey(PGN.TERMINATION_TAG))
                        result = tagsMap.get(PGN.TERMINATION_TAG);
                    else if (tagsMap.containsKey(PGN.RESULT_TAG))
                        result = tagsMap.get(PGN.RESULT_TAG);
                }
            } catch (Exception e) {
                Log.e(TAG, "onBindViewHolder: Exception while finding result", e);
            }

            holder.name.setText(name);
            if (result != null && !result.isEmpty()) holder.result.setText(result);
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

    public static String getTAG() {
        return TAG;
    }
}