package com.drdedd.simplichess.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.drdedd.simplichess.BuildConfig;
import com.drdedd.simplichess.R;
import com.drdedd.simplichess.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {
    private FragmentAboutBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater);
        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.show();
        }
        binding.versionNumber.setText(BuildConfig.VERSION_NAME);
        StringBuilder stringBuilder = new StringBuilder();
        String[] developersList = requireContext().getResources().getStringArray(R.array.developers);
        for (String developer : developersList)
            stringBuilder.append(developer).append("\n");
        binding.developers.setText(stringBuilder);
        binding.SVGLink.setMovementMethod(LinkMovementMethod.getInstance());
    }
}