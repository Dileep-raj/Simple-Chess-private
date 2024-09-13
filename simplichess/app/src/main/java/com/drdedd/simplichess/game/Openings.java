package com.drdedd.simplichess.game;

import static com.drdedd.simplichess.data.Regexes.moveNumberRegex;

import android.content.Context;
import android.util.Log;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.fragments.HomeFragment;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Tree collection of opening moves in chess
 *
 * @see <a href="https://en.wikipedia.org/wiki/Chess_opening">Chess Openings</a> <br> <a href="https://en.wikipedia.org/wiki/Encyclopaedia_of_Chess_Openings">ECO</a>
 */
public class Openings {
    private final Node root;
    private static Openings openings;
    public static final String separator = "%";
    private static final String TAG = "Openings";

    private Openings(Context context) {
        root = new Node("-", "");
        Log.d(TAG, "Openings: Loading Openings");
        try {
            long start = System.nanoTime();
            List<String[]> lines;
            InputStream inputStream = context.getResources().openRawResource(R.raw.openings);
            try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(inputStream)).withSkipLines(1).build()) {
                lines = csvReader.readAll();
                for (String[] line : lines)
                    addOpening(line[2], line[0] + ": " + line[1]);
            }
            long end = System.nanoTime();
            HomeFragment.printTime(TAG, "loading openings", end - start, lines.size());
        } catch (Exception e) {
            Log.e(TAG, "Openings: Error while loading openings", e);
        }
    }

    public static Openings getInstance(Context context) {
        if (openings == null) openings = new Openings(context);
        return openings;
    }

    private void addOpening(String moveSequence, String name) {
        String[] moves = moveSequence.replaceAll(moveNumberRegex, "").trim().split("\\s+");
        String move, openingName;
        Node node = root, tempNode;
        int i = 0, l = moves.length;
        while (i < l) {
            tempNode = null;
            ArrayList<Node> nodes = node.getNodes();
            if (nodes.isEmpty()) break;
            move = moves[i];

            int low = 0, high = nodes.size() - 1, mid;
            while (low <= high) {
                mid = (low + high) / 2;
                tempNode = nodes.get(mid);
                int result = move.compareToIgnoreCase(tempNode.getMove());
                if (result == 0) {
                    node = tempNode;
                    break;
                }
                if (result > 0) low = mid + 1;
                else high = mid - 1;
            }
            if (node != tempNode) break;
            i++;
        }
        Node newNode = node;
        while (i < l) {
            move = moves[i];
//            openingName = (i == l - 1) ? name : "";
            openingName = name;
            newNode.addNode(new Node(move, openingName));
            newNode = newNode.getNodes().get(newNode.getNodes().size() - 1);
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
        ArrayList<String> moves = new ArrayList<>(movesList);
        String move, opening = "";

        Node node = root;
        int low, high, mid, pos = -1, i = 0;

//      Iterate through moves to find opening
        while (i < 36 && i < moves.size()) {
            Node tempNode = null;
            ArrayList<Node> nodes = node.getNodes();
            move = moves.get(i);
            low = 0;
            high = nodes.size() - 1;

//          Perform binary search and find the matching opening move
            while (low <= high) {
                mid = (low + high) / 2;
                tempNode = nodes.get(mid);
                int result = move.compareToIgnoreCase(tempNode.getMove());

//              If move matches node move
                if (result == 0) {
                    node = tempNode;
//                    if (!tempNode.getOpeningName().isEmpty()) opening = tempNode.getOpeningName();
                    opening = tempNode.getOpeningName();
                    pos = i;
                    i++;
                    break;
                }
                if (result > 0) low = mid + 1;
                else high = mid - 1;
            }
            if (node != tempNode) return pos + separator + opening;
        }
        return pos + separator + opening;
    }

    private static class Node {
        private final String move, openingName;
        private final ArrayList<Node> nodes;

        Node(String move, String openingName) {
            this.move = move;
            this.openingName = openingName;
            nodes = new ArrayList<>();
        }

        public void addNode(Node node) {
            nodes.add(node);
        }

        public String getMove() {
            return move;
        }

        public ArrayList<Node> getNodes() {
            return nodes;
        }

        public String getOpeningName() {
            return openingName;
        }
    }
}