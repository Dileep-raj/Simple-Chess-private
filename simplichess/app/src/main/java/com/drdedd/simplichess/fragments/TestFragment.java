package com.drdedd.simplichess.fragments;

import static com.drdedd.simplichess.misc.MiscMethods.isConnected;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.databinding.FragmentTestBinding;
import com.drdedd.simplichess.game.GameLogic;
import com.drdedd.simplichess.interfaces.GameFragmentInterface;
import com.drdedd.simplichess.misc.lichess.LichessAPI;
import com.drdedd.simplichess.misc.lichess.LichessGame;
import com.drdedd.simplichess.views.CompactBoard;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestFragment extends Fragment implements GameFragmentInterface {
    private static final String TAG = "TestFragment";
    private FragmentTestBinding binding;
    private OutputStreamWriter writer;

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
        CompactBoard compactBoard = binding.compactBoard;
        new GameLogic(this, requireContext(), compactBoard.getBoard(), true);
        compactBoard.setPlayersData("GM", "White player", "GM", "Black player");
        compactBoard.setToggleSizeButton(binding.shrinkExpandButton, R.drawable.ic_shrink, R.drawable.ic_expand);
    }

    @SuppressLint("SetTextI18n")
    private void test() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                String id = "nD062ydS";
                JSONObject json = LichessAPI.getGameJSONById(id);
                LichessGame game = LichessGame.parse(json);
                Log.d(TAG, "test:" + game);
            } catch (Exception e) {
                Log.e(TAG, "test:Exception", e);
            }
        });

        if (!isConnected(requireContext()))
            Toast.makeText(requireContext(), "Not connected to internet", Toast.LENGTH_SHORT).show();
    }

    private void sendCommand(String command) {
        try {
            writer.write(command + '\n');
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "sendCommand:Exception during executing command!", e);
        }
    }

    @SuppressLint("SetTextI18n")
    private void printSystemProperties() {
        Properties properties = System.getProperties();
        Set<Object> keySet = properties.keySet();
        StringBuilder outputBuilder = new StringBuilder();
        for (Object key : keySet)
            outputBuilder.append(String.format("%s = %s", key.toString(), properties.get(key))).append('\n');
//        File file = requireContext().getFilesDir();
//        File[] files = file.listFiles(File::isFile);
//        if (files != null)
//            Log.d(TAG,"printSystemProperties:Files in app folder:" + Arrays.toString(files));
    }

    @Override
    public void updateViews() {
    }

    @Override
    public void terminateGame(String termination) {
        Toast.makeText(requireContext(), termination, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean saveProgress() {
        return false;
    }

    public static String getTAG() {
        return TAG;
    }

    private static class TestThread extends Thread {
        private CircularProgressIndicator circular;
        private LinearProgressIndicator linear;

        TestThread() {
        }

        TestThread(CircularProgressIndicator testProgress, LinearProgressIndicator testLinear) {
            this.circular = testProgress;
            this.linear = testLinear;
        }

        @Override
        public void run() {
            super.run();
            String id = "nD062ydS";
            JSONObject json = LichessAPI.getGameJSONById(id);
            if (json != null) {
                LichessGame game = LichessGame.parse(json);
                Log.d(TAG, "run: Lichess game:\n" + game);
            }
//            Set<String> testGames = LichessGame.testGamesSet;
//            int i = 0;
//            for (String id : testGames) {
//                try {
//                    JSONObject json = LichessAPI.getGameJSONById(id);
//                    if (json != null) {
//                        LichessGame game = LichessGame.parse(json);
//                        Log.d(TAG, String.format("run: ID: %s Game result: %s", id, game.getResult()));
//                    }
//                    int progress = 100 * ++i / testGames.size();
//                    circular.setProgressCompat(progress, true);
//                    linear.setProgress(progress, true);
//                } catch (Exception e) {
//                    Log.e(TAG, "run: Exception!", e);
//                }
//            }
            Log.d(TAG, "run: Method ended");
        }
    }

    private final static String json = "{\"id\":\"3TjDcbH9\",\"rated\":true,\"variant\":\"standard\",\"speed\":\"rapid\",\"perf\":\"rapid\",\"createdAt\":1726039185370,\"lastMoveAt\":1726039585398,\"status\":\"mate\",\"source\":\"pool\",\"players\":{\"white\":{\"user\":{\"name\":\"Searoute\",\"id\":\"searoute\"},\"rating\":1281,\"ratingDiff\":-17,\"analysis\":{\"inaccuracy\":2,\"mistake\":6,\"blunder\":2,\"acpl\":94,\"accuracy\":73}},\"black\":{\"user\":{\"name\":\"DrDedd\",\"id\":\"drdedd\"},\"rating\":1322,\"ratingDiff\":7,\"analysis\":{\"inaccuracy\":1,\"mistake\":1,\"blunder\":2,\"acpl\":50,\"accuracy\":84}}},\"winner\":\"black\",\"opening\":{\"eco\":\"C44\",\"name\":\"King's Knight Opening:Normal Variation\",\"ply\":4},\"moves\":\"e4 e5 Nf3 Nc6 h3 Nf6 Nc3 Bc5 d4 exd4 Ne2 O-O Bg5 Be7 Nexd4 Nxe4 Bxe7 Qxe7 Be2 Nxd4 Nxd4 c5 Nf5 Qe5 g4 d6 f3 Ng3 Nxd6 Nxh1 Nc4 Qg3+ Kd2 Be6 Kc1 Nf2 Qe1 Rad8 Ne3 Nd3+ Kd2 Nxe1+ Kc3 Qe5#\",\"clocks\":[  60003,60003,59827,59899,59467,59731,59219,59579,58427,59419,55379,58139,54683,57787,53075,57403,51331,56419,49579,54307,49251,53347,48331,52675,46931,52003,46163,50827,44603,49939,41611,49507,40939,48467,40083,48043,38971,47259,37851,46843,36779,45931,36065,45126],\"analysis\":[{\"eval\":18},{\"eval\":21},{\"eval\":21},{\"eval\":17},{\"eval\":5},{\"eval\":3},{\"eval\":0},{\"eval\":35},{\"eval\":-82,\"best\":\"f3e5\",\"variation\":\"Nxe5 Nxe5 d4 Bd6 dxe5 Bxe5 Bd3 d5 exd5 Bxc3+\",\"judgment\":{\"name\":\"Mistake\",\"comment\":\"Mistake. Nxe5 was best.\"}},{\"eval\":-97},{\"eval\":-302,\"best\":\"c3d5\",\"variation\":\"Nd5 h6 Bd3 d6 O-O O-O Bd2 a5 Nxf6+ Qxf6\",\"judgment\":{\"name\":\"Blunder\",\"comment\":\"Blunder. Nd5 was best.\"}},{\"eval\":-234},{\"eval\":-331,\"best\":\"e2d4\",\"variation\":\"Nexd4 Nxd4\",\"judgment\":{\"name\":\"Inaccuracy\",\"comment\":\"Inaccuracy. Nexd4 was best.\"}},{\"eval\":-180,\"best\":\"f8e8\",\"variation\":\"Re8 Qd2 Nxe4 Bxd8 Nxd2 Kxd2 Rxd8 Nc1 a5 Nb3 Bb6 a4\",\"judgment\":{\"name\":\"Mistake\",\"comment\":\"Mistake. Re8 was best.\"}},{\"eval\":-188},{\"eval\":-186},{\"eval\":-322,\"best\":\"d4c6\",\"variation\":\"Nxc6 bxc6 Be3 f5 Bd3 d5 O-O c5 c3 Bb7 Bf4 Rb8\",\"judgment\":{\"name\":\"Mistake\",\"comment\":\"Mistake. Nxc6 was best.\"}},{\"eval\":-336},{\"eval\":-326},{\"eval\":-225,\"best\":\"e7b4\",\"variation\":\"Qb4+ Kf1 Qxb2 Nb5 Nc3 Nxc3 Qxc3 Kg1 d6 Kh2 b6 Re1\",\"judgment\":{\"name\":\"Inaccuracy\",\"comment\":\"Inaccuracy. Qb4+ was best.\"}},{\"eval\":-374,\"best\":\"d1d4\",\"variation\":\"Qxd4 Nf6 Kf1 d5 Qf4 Qc5 Bd3 Ne4 Kg1 Qxf2+ Kh2 Qb6\",\"judgment\":{\"name\":\"Mistake\",\"comment\":\"Mistake. Qxd4 was best.\"}},{\"eval\":-147,\"best\":\"e7b4\",\"variation\":\"Qb4+ c3 Qxb2 Qc1 Qxc3+ Qxc3 Nxc3 Bf3 d5 Rc1 Re8+ Kf1\",\"judgment\":{\"name\":\"Blunder\",\"comment\":\"Blunder. Qb4+ was best.\"}},{\"eval\":-225,\"best\":\"d4f3\",\"variation\":\"Nf3 d5 O-O Be6 Re1 Rad8 Bd3 f5 Qe2 c4 Nd4 cxd3\",\"judgment\":{\"name\":\"Inaccuracy\",\"comment\":\"Inaccuracy. Nf3 was best.\"}},{\"eval\":-171},{\"eval\":-330,\"best\":\"f5e3\",\"variation\":\"Ne3 Ng3 fxg3 Qxe3 Qd3 Qxd3 cxd3 d6 Bf3 Be6 b3 Rab8\",\"judgment\":{\"name\":\"Mistake\",\"comment\":\"Mistake. Ne3 was best.\"}},{\"eval\":-286},{\"eval\":-538,\"best\":\"f5e3\",\"variation\":\"Ne3 Re8 Bf3 Nxf2 Kxf2 Qxe3+ Kg2 Qg5 Rf1 Be6 Qxd6 Rad8\",\"judgment\":{\"name\":\"Mistake\",\"comment\":\"Mistake. Ne3 was best.\"}},{\"eval\":-268,\"best\":\"c8f5\",\"variation\":\"Bxf5 fxe4 Bxe4 Rf1 d5 Qd2 Qxb2 Rd1 Rae8 c3 Qb6 Rf2\",\"judgment\":{\"name\":\"Blunder\",\"comment\":\"Blunder. Bxf5 was best.\"}},{\"eval\":-639,\"best\":\"f5g3\",\"variation\":\"Nxg3 Qxg3+ Kf1 Bd7 Qe1 Qe5 Qc3 Qf4 Rd1 Rfe8 Qd2 Qe5\",\"judgment\":{\"name\":\"Blunder\",\"comment\":\"Blunder. Nxg3 was best.\"}},{\"eval\":-644},{\"eval\":-813},{\"eval\":-808},{\"eval\":-789},{\"eval\":-789},{\"eval\":-837},{\"eval\":-834},{\"eval\":-1133},{\"eval\":-904},{\"mate\":-10,\"best\":\"c1b1\",\"variation\":\"Kb1 Bxc4 Bxc4 Rd1+ Qxd1 Nxd1 a4 Qg1 Ka2 Qd4 Rxd1 Qxd1\",\"judgment\":{\"name\":\"Mistake\",\"comment\":\"Checkmate is now unavoidable. Kb1 was best.\"}},{\"mate\":-9},{\"mate\":-6},{\"mate\":-5},{\"mate\":-1}],\"clock\":{\"initial\":600,\"increment\":0,\"totalTime\":600},\"division\":{\"middle\":21}}";
}