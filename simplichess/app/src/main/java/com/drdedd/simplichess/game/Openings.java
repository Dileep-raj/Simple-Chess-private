package com.drdedd.simplichess.game;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.fragments.HomeFragment;
import com.opencsv.CSVReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Tree collection of opening moves in chess<br/>
 * Eg:
 * <pre>
 *         *
 *       / | \
 *      a3 a4 ... (White's move)
 *    / | \
 *   c5 d5 ...    (Black's move)
 * </pre>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Chess_opening">Chess Openings</a>, <a href="https://en.wikipedia.org/wiki/Encyclopaedia_of_Chess_Openings">ECO</a>
 */
public class Openings implements Serializable {
    private final MoveNode root;
    private final ArrayList<Pair<String, ArrayList<String>>> allOpenings = new ArrayList<>();
    private static Openings openings;
    public static final String separator = "%";
    private static final String TAG = "Openings";

    private Openings(Context context) {
        root = new MoveNode("-", "", "");
        Log.d(TAG, "Openings: Loading Openings");
        try {
            long start = System.nanoTime();
            InputStream inputStream = context.getResources().openRawResource(R.raw.openings);
            CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream));
            List<String[]> lines = csvReader.readAll();
            for (String[] line : lines)
                addOpening(line[2], line[0], line[1]);
            long end = System.nanoTime();
            Log.d(TAG, "Openings: Openings loaded");
            HomeFragment.printTime(TAG, "loading openings", end - start, lines.size());
        } catch (Exception e) {
            Log.e(TAG, "Openings: Error while loading openings", e);
        }
    }

    public static Openings getInstance(Context context) {
        if (openings == null) openings = new Openings(context);
        return openings;
    }

    private void addOpening(String moveSequence, String eco, String name) {
//        String[] moves = moveSequence.replaceAll(moveNumberRegex, "").trim().split("\\s+");
        String[] moves = moveSequence.trim().split("\\s+");
        allOpenings.add(new Pair<>(name, new ArrayList<>(Arrays.asList(moves))));
        String move, openingName;
        MoveNode moveNode = root, tempMoveNode;
        int i = 0, l = moves.length;

        while (i < l) {
            tempMoveNode = null;
            ArrayList<MoveNode> moveNodes = moveNode.moveNodes;
            if (moveNodes.isEmpty()) break;
            move = moves[i];

            int low = 0, high = moveNodes.size() - 1, mid;
            while (low <= high) {
                mid = (low + high) / 2;
                tempMoveNode = moveNodes.get(mid);
                int result = move.compareToIgnoreCase(tempMoveNode.getMove());
                if (result == 0) {
                    moveNode = tempMoveNode;
                    break;
                }
                if (result > 0) low = mid + 1;
                else high = mid - 1;
            }
            if (moveNode != tempMoveNode) break;
            i++;
        }

        MoveNode newMoveNode = moveNode;
        while (i < l) {
            move = moves[i];
//            openingName = (i == l - 1) ? name : "";
            openingName = name;
            newMoveNode.addNode(new MoveNode(move, eco, openingName));
            newMoveNode = newMoveNode.moveNodes.get(newMoveNode.moveNodes.size() - 1);
            i++;
        }
    }

    /**
     * Use {@link Openings#separator separator} to separate move number and opening
     *
     * @param movesList List of moves
     * @return Opening and move number<br>Format: <code>MoveNumber%Opening</code>
     */
    public String searchOpening(LinkedList<String> movesList) {
        Log.d(TAG, "searchOpening: Searching opening");
        long start = System.nanoTime(), end;
        ArrayList<String> moves = new ArrayList<>(movesList);
        String move, eco = "", opening = "";

        MoveNode moveNode = root;
        int low, high, mid, pos = -1, i = 0;

//      Iterate through moves to find opening
        while (i < 36 && i < moves.size()) {
            MoveNode tempMoveNode = null;
            ArrayList<MoveNode> moveNodes = moveNode.moveNodes;
            move = moves.get(i);
            low = 0;
            high = moveNodes.size() - 1;

//          Perform binary search and find the matching opening move
            while (low <= high) {
                mid = (low + high) / 2;
                tempMoveNode = moveNodes.get(mid);
                int result = move.compareToIgnoreCase(tempMoveNode.getMove());

//              If player's move matches the opening move, continue the search
                if (result == 0) {
                    moveNode = tempMoveNode;
//                    if (!tempMoveNode.openingName.isEmpty()) opening = tempMoveNode.openingName;
                    opening = tempMoveNode.openingName;
                    eco = tempMoveNode.eco;
                    pos = i;
                    i++;
                    break;
                }
                if (result > 0) low = mid + 1;
                else high = mid - 1;
            }
            if (moveNode != tempMoveNode) {
                end = System.nanoTime();
                StringBuilder openingMoves = new StringBuilder();
                for (int j = 0; j <= pos; j++) openingMoves.append(moves.get(j)).append(' ');
                Log.d(TAG, String.format("searchOpening: Time for searching opening: %,3d ns%nOpening: %s%nMoves: %s", end - start, opening, openingMoves));
                return pos + separator + eco + separator + opening;
            }
        }
        end = System.nanoTime();
        StringBuilder openingMoves = new StringBuilder();
        for (int j = 0; j <= pos; j++) openingMoves.append(moves.get(j)).append(' ');
        Log.d(TAG, String.format("searchOpening: Time for searching opening: %,3d ns%nOpening: %s%nMoves: %s", end - start, opening, openingMoves));
        return pos + separator + eco + separator + opening;
    }

    public ArrayList<Pair<String, ArrayList<String>>> getAllOpenings() {
        return allOpenings;
    }

    public String buildTree() {
        StringBuilder tree = new StringBuilder();
        tree.append(root.move);
        return tree.toString();
    }

    private static class MoveNode {
        private final String move, openingName, eco;
        private final ArrayList<MoveNode> moveNodes;

        MoveNode(String move, String eco, String openingName) {
            this.move = move;
            this.eco = eco;
            this.openingName = openingName;
            moveNodes = new ArrayList<>();
        }

        public void addNode(MoveNode moveNode) {
            moveNodes.add(moveNode);
        }

        public String getMove() {
            return move;
        }
    }
}