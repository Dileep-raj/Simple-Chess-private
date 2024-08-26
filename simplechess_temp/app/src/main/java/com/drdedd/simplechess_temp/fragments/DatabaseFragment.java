package com.drdedd.simplechess_temp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drdedd.simplechess_temp.data.GameStatistics;
import com.drdedd.simplechess_temp.databinding.FragmentDatabaseBinding;

import java.util.ArrayList;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DatabaseFragment extends Fragment {
    private FragmentDatabaseBinding binding;
    private GameStatistics gameStatistics;
    private ArrayList<String> namesList;
    private String name;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDatabaseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gameStatistics = new GameStatistics(requireContext());
        namesList = gameStatistics.getNames();

        ArrayAdapter<String> namesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, namesList);
        binding.namesSpinner.setAdapter(namesAdapter);
        binding.namesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                name = namesList.get(i);
                updateData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void updateData() {
        ArrayList<GameStatistics.RecordsData> records = gameStatistics.getRecords(name);
    }
}