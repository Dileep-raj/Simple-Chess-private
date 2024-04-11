package com.drdedd.simplechess_temp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drdedd.simplechess_temp.databinding.FragmentTestBinding;

public class TestFragment extends Fragment {
    private FragmentTestBinding binding;
    private TextView textView;
    private HorizontalScrollView horizontalScrollView;
    private StringBuilder text;
    private int i;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTestBinding.inflate(inflater, container, false);
        text = new StringBuilder();
        i = 0;
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnAddText.setOnClickListener(v -> addText());
        binding.btnScroll.setOnClickListener(v -> horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
        textView = binding.innerTextview;
        horizontalScrollView = binding.scrollView;
    }

    private void addText() {
        text.append(" Sample Text asefaefsegdf ").append(i++);
        textView.setText(text.toString());
        horizontalScrollView.post(() -> horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
//        horizontalScrollView.postDelayed(() -> horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT), 10);
//        horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
    }

}