package com.drdedd.simplichess.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.databinding.FragmentTestBinding;
import com.drdedd.simplichess.game.GameLogic;
import com.drdedd.simplichess.game.PGN;
import com.drdedd.simplichess.interfaces.GameFragmentInterface;
import com.drdedd.simplichess.views.ChessBoard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class TestFragment extends Fragment implements GameFragmentInterface {
    private static final String TAG = "TestFragment";
    private FragmentTestBinding binding;
    private OutputStreamWriter writer;
    private ChessBoard chessBoard;
    private GameLogic gameLogic;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.hide();
        }
        binding.btnTest.setOnClickListener(v -> test());
        chessBoard = binding.testChessBoard;
        gameLogic = new GameLogic(this, requireContext(), chessBoard, true);
    }

    private void test() {
//        Handler handler = new Handler(Looper.getMainLooper()) {
//            @Override
//            public void handleMessage(@NonNull Message msg) {
//                super.handleMessage(msg);
//                //Add handler task
//            }
//        };
//        TestThread thread = new TestThread(handler);
//        thread.start();
        chessBoard.flipBoard();
//        chessBoard.setInvertBlackPieces(true);
//        chessBoard.invalidate();
    }

    private void testExecutable() {
        String fileName = "testExecutable", executeCommand = "chmod 744 ";
        File file = null;
        Process process;
        try {
            try (InputStream inputStream = requireContext().getResources().openRawResource(R.raw.helloworld)) {
                byte[] bytes = new byte[inputStream.available()];
                Log.d(TAG, "onViewCreated: Read buffer bytes: " + inputStream.read(bytes));
                try (FileOutputStream fileOutputStream = requireContext().openFileOutput(fileName, Context.MODE_PRIVATE)) {
                    fileOutputStream.write(bytes);
                }
            }

            file = requireContext().getFileStreamPath(fileName);
//            Log.d(TAG, "onViewCreated: Executable set: " + file.setExecutable(true));

            process = Runtime.getRuntime().exec(executeCommand + file.getAbsolutePath());
//            process = new ProcessBuilder("chmod", "-R", "777", file.getAbsolutePath()).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new OutputStreamWriter(process.getOutputStream());
            sendCommand("isready");
            sendCommand("d");
            String line;
            while ((line = reader.readLine()) != null) Log.d(TAG, "onViewCreated: Output: " + line);
            Log.d(TAG, "onViewCreated: Output ended");

            process.destroy();
            if (file.delete()) Log.d(TAG, "onViewCreated: File deleted after execution");
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated: Exception occurred!", e);
            if (file != null && file.exists() && file.delete())
                Log.d(TAG, "onViewCreated: File deleted successfully!");
        }
    }

    private void sendCommand(String command) {
        try {
            writer.write(command + '\n');
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "sendCommand: Exception during executing command!", e);
        }
    }

    @SuppressLint("SetTextI18n")
    private void printSystemProperties() {
        Properties properties = System.getProperties();
        Set<Object> keySet = properties.keySet();
        StringBuilder outputBuilder = new StringBuilder();
        for (Object key : keySet)
            outputBuilder.append(String.format("%s = %s", key.toString(), properties.get(key))).append('\n');
//        binding.output.setText(outputBuilder.toString());

//        binding.output.setText(System.getProperty("os.name"));
//        binding.output.setText("No output yet");
        File file = requireContext().getFilesDir();
        File[] files = file.listFiles(File::isFile);
        if (files != null)
            Log.d(TAG, "printSystemProperties: Files in app folder: " + Arrays.toString(files));
    }

    @Override
    public void updateViews() {
    }

    @Override
    public void terminateGame(String termination) {
    }

    @Override
    public boolean saveProgress() {
        return false;
    }

    private static class TestThread extends Thread {
        private final Handler handler;
        private final ArrayList<String> evaluations;

        TestThread(Handler handler) {
            this.handler = handler;
            evaluations = new ArrayList<>(List.of("-1.23", "+2.4", "+5", "-5", "+4", "-4", "+M1", "-M1", "M-2", PGN.RESULT_WHITE_WON, PGN.RESULT_BLACK_WON, PGN.RESULT_DRAW));
        }

        @Override
        public void run() {
            super.run();
            for (String eval : evaluations) {
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("eval", eval);
                message.setData(bundle);
                handler.sendMessage(message);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "run: Exception in Thread", e);
                }
            }
        }

    }

    public static String getTAG() {
        return TAG;
    }
}