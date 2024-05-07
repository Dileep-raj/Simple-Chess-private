package com.drdedd.simplechess_temp.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drdedd.simplechess_temp.databinding.FragmentTestBinding;

public class TestFragment extends Fragment implements GestureDetector.OnGestureListener, View.OnTouchListener {
    private FragmentTestBinding binding;
    private static final String DEBUG_TAG = "Gestures", TAG = "TestFragment";
    private GestureDetector mDetector;
    private final int sideLength = 10;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTestBinding.inflate(inflater, container, false);
        mDetector = new GestureDetector(requireContext(), this);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.getRoot().setOnTouchListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(DEBUG_TAG, String.format("onDown: (%s,%s)", event.getX(), event.getY()));
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, @NonNull MotionEvent event2, float velocityX, float velocityY) {
        if (event1 != null)
            Log.d(DEBUG_TAG, String.format("onFling: (%s,%s) -> (%s,%s)", event1.getX(), event1.getY(), event2.getX(), event2.getY()));
        return true;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent event) {
//        Log.d(DEBUG_TAG, String.format("onLongPress: (%s,%s)", event.getX(), event.getY()));
    }

    @Override
    public boolean onScroll(MotionEvent event1, @NonNull MotionEvent event2, float distanceX, float distanceY) {
        if (event1 != null)
            Log.d(DEBUG_TAG, String.format("onScroll: (%s,%s) -> (%s,%s): (%s,%s)", event1.getX(), event1.getY(), event2.getX(), event2.getY(), distanceX, distanceY));
        return false;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent event) {
//        Log.d(DEBUG_TAG, String.format("onShowPress: (%s,%s)", event.getX(), event.getY()));
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(DEBUG_TAG, String.format("onSingleTapUp: (%s,%s)", event.getX(), event.getY()));
        return true;
    }

    public static String getTAG() {
        return TAG;
    }
}