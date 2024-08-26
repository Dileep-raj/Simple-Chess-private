package com.drdedd.simplechess_temp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drdedd.simplechess_temp.interfaces.PGNRecyclerViewInterface;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * PGN (Portable Game Notation) is a standard text format used to record Chess game moves with standard notations
 *
 * @see <a href="https://en.wikipedia.org/wiki/Portable_Game_Notation"> More about PGN </a>
 */
public class PGN implements Serializable {
    /**
     * Constant String for special moves
     */
    public static final String LONG_CASTLE = "O-O-O", SHORT_CASTLE = "O-O", CAPTURE = "Capture", PROMOTE = "promote";
    public static final String RESULT_DRAW = "1/2-1/2", RESULT_WHITE_WON = "1-0", RESULT_BLACK_WON = "0-1", RESULT_ONGOING = "*";
    public static final String TAG_APP = "App", TAG_WHITE = "White", TAG_DATE = "Date", TAG_BLACK = "Black", TAG_SET_UP = "SetUp", TAG_FEN = "FEN", TAG_RESULT = "Result", TAG_TERMINATION = "Termination";
    //    public static final String TAG_ECO = "ECO", TAG_OPENING = "Opening";
    private final String FEN;
    private boolean whiteToPlay;
    private String termination = "";
    private final LinkedList<String> moves = new LinkedList<>();
    private final LinkedHashMap<String, String> allTags = new LinkedHashMap<>();
    public LinkedHashMap<Integer, String> commentsMap, moveAnnotationMap, alternateMoveSequence, evalMap;

    /**
     * @param app         Name of app
     * @param white       Name of White player
     * @param black       Name of Black player
     * @param date        Date of the game
     * @param whiteToPlay Player to play
     */
    public PGN(String app, String white, String black, String date, boolean whiteToPlay) {
        allTags.put(TAG_APP, app);
        allTags.put(TAG_WHITE, white);
        allTags.put(TAG_BLACK, black);
        allTags.put(TAG_DATE, date);
        this.whiteToPlay = whiteToPlay;
        FEN = "";
        moves.clear();
        commentsMap = new LinkedHashMap<>();
        moveAnnotationMap = new LinkedHashMap<>();
        alternateMoveSequence = new LinkedHashMap<>();
    }

    /**
     * @param app         Name of app
     * @param white       Name of White player
     * @param black       Name of Black player
     * @param date        Date of the game
     * @param whiteToPlay Player to play
     * @param FEN         Starting position of the game
     */
    public PGN(String app, String white, String black, String date, boolean whiteToPlay, String FEN) {
        allTags.put(TAG_APP, app);
        allTags.put(TAG_WHITE, white);
        allTags.put(TAG_BLACK, black);
        allTags.put(TAG_DATE, date);
        this.whiteToPlay = whiteToPlay;
        this.FEN = FEN;
        moves.clear();
        commentsMap = new LinkedHashMap<>();
        moveAnnotationMap = new LinkedHashMap<>();
        alternateMoveSequence = new LinkedHashMap<>();
    }

    /**
     * Adds move to PGN
     *
     * @param piece Piece moved
     * @param move  Special move if any
     */
    public void addToPGN(Piece piece, String move) {
        if (move.isEmpty()) moves.addLast(piece.getPosition());
        else moves.addLast(move);
    }

    /**
     * Removes last move from PGN
     */
    public void removeLast() {
        if (!moves.isEmpty()) moves.removeLast();
    }

    /**
     * @return <code>true|false</code> - White to play
     */
    public boolean isWhiteToPlay() {
        return whiteToPlay;
    }

    /**
     * Sets white to play
     *
     * @param whiteToPlay White to play
     */
    public void setWhiteToPlay(boolean whiteToPlay) {
        this.whiteToPlay = whiteToPlay;
    }

    /**
     * Set White and Black player names
     *
     * @param white Name of white player
     * @param black Name of black player
     */
    public void setWhiteBlack(String white, String black) {
        allTags.put(TAG_WHITE, white);
        allTags.put(TAG_BLACK, black);
    }

    /**
     * @return <code>String</code> - White player name
     */
    public String getWhite() {
        return allTags.get(TAG_WHITE);
    }

    /**
     * @return <code>String</code> - Black player name
     */
    public String getBlack() {
        return allTags.get(TAG_BLACK);
    }

    /**
     * Converts PGN to standard text format
     *
     * @return String - PGN with tags and moves
     */
    @NonNull
    @Override
    public String toString() {
        return getTags() + getPGNCommented();
    }

    /**
     * PGN tags with their values
     *
     * @return String - PGN Tags text
     */
    private String getTags() {
        StringBuilder tags = new StringBuilder();
        allTags.put(TAG_RESULT, getResult());
        if (!termination.isEmpty()) allTags.put(TAG_TERMINATION, termination);
        if (FEN != null && !FEN.isEmpty()) {
            allTags.put(TAG_SET_UP, "1");
            allTags.put(TAG_FEN, FEN);
        }
        Set<String> tagSet = allTags.keySet();
        for (String tag : tagSet) {
            String value = allTags.get(tag);
            if (value == null || value.isEmpty()) value = "?";
            tags.append(String.format("[%s \"%s\"]\n", tag, value));
        }
        return tags.toString();
    }

    /**
     * Returns result of the game
     *
     * @return <code> * | 0-1 | 1-0 | 1/2-1/2 </code>
     */
    public String getResult() {
        if (allTags.containsKey(TAG_RESULT)) return allTags.get(TAG_RESULT);
        return "";
    }

    /**
     * Adds game result to the tag data
     *
     * @param result Result of the game
     */
    public void setResult(String result) {
        if (!result.isEmpty()) addTag(TAG_RESULT, result);
    }

    /**
     * @return <code>String</code> - PGN moves without tags, comments and annotations
     */
    public String getPGNMoves() {
        StringBuilder pgn = new StringBuilder();
        int length = moves.size();
        for (int i = 0; i < length; i++) {
            if (i % 2 == 0) pgn.append(i / 2 + 1).append('.');
            pgn.append(moves.get(i)).append(' ');
        }
        return pgn.toString();
    }

    /**
     * @return <code>String</code> - PGN moves with comments and annotation
     */
    public String getPGNCommented() {
        StringBuilder pgn = new StringBuilder();
        int length = moves.size();
        for (int i = 0; i < length; i++) {
            if (i % 2 == 0) pgn.append(i / 2 + 1).append('.');
            pgn.append(moves.get(i));
            if (moveAnnotationMap.containsKey(i)) pgn.append(moveAnnotationMap.get(i));
            pgn.append(' ');
            if (commentsMap.containsKey(i)) pgn.append(commentsMap.get(i)).append(' ');
            if (alternateMoveSequence.containsKey(i))
                pgn.append(alternateMoveSequence.get(i)).append(' ');
        }
        return pgn.toString();
    }

    /**
     * Set termination message for the game
     *
     * @param termination Termination message
     */
    public void setTermination(String termination) {
        this.termination = termination;
    }

    /**
     * Get termination message of the game
     *
     * @return <code>String</code> - Termination message
     */
    public String getTermination() {
        return termination;
    }

    /**
     * @return Number of moves in PGN
     */
    public int getMoveCount() {
        return moves.size();
    }

    /**
     * @param moveNo Move number
     * @return <code>String|null</code> - Move at given position
     */
    public String getMoveAt(int moveNo) {
        if (moveNo < moves.size()) return moves.get(moveNo);
        return null;
    }

    /**
     * @return List of moves
     */
    public LinkedList<String> getMoves() {
        return moves;
    }

    /**
     * Adds tag to PGN tags
     *
     * @param tag   Tag name
     * @param value Tag value
     */
    public void addTag(String tag, String value) {
        allTags.put(tag, value);
    }

    public void setCommentsMap(LinkedHashMap<Integer, String> commentsMap) {
        this.commentsMap = commentsMap;
    }

    public void setMoveAnnotationMap(LinkedHashMap<Integer, String> moveAnnotationMap) {
        this.moveAnnotationMap = moveAnnotationMap;
    }

    public void setAlternateMoveSequence(LinkedHashMap<Integer, String> alternateMoveSequence) {
        this.alternateMoveSequence = alternateMoveSequence;
    }

    public void setEvalMap(LinkedHashMap<Integer, String> evalMap) {
        this.evalMap = evalMap;
    }

    public boolean isFENEmpty() {
        return FEN.isEmpty();
    }

    /**
     * RecyclerView Adapter for PGN
     */
    public static class PGNRecyclerViewAdapter extends RecyclerView.Adapter<PGNRecyclerViewAdapter.PGNViewHolder> {
        private final Context context;
        private final PGN pgn;
        private final PGNRecyclerViewInterface pgnRecyclerViewInterface;

        public PGNRecyclerViewAdapter(Context context, PGN pgn, PGNRecyclerViewInterface pgnRecyclerViewInterface) {
            this.context = context;
            this.pgn = pgn;
            this.pgnRecyclerViewInterface = pgnRecyclerViewInterface;
        }

        @NonNull
        @Override
        public PGNViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.rv_pgn_move, parent, false);
            PGNViewHolder pgnViewHolder = new PGNViewHolder(view, pgnRecyclerViewInterface);
            pgnViewHolder.setIsRecyclable(false);
            return pgnViewHolder;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull PGNViewHolder holder, int position) {
            holder.moveNo.setVisibility(View.VISIBLE);
            holder.move.setBackgroundResource(position == pgnRecyclerViewInterface.getPosition() ? R.drawable.pgn_move_highlight : R.drawable.pgn_move_bg);
            if (position % 2 == 0) holder.moveNo.setText(position / 2 + 1 + ".");
            else holder.moveNo.setVisibility(View.GONE);
            holder.move.setText(pgn.getMoveAt(position));
        }

        @Override
        public int getItemCount() {
            return pgn.getMoveCount();
        }

        public static class PGNViewHolder extends RecyclerView.ViewHolder {
            private final TextView moveNo, move;

            public PGNViewHolder(@NonNull View itemView, PGNRecyclerViewInterface pgnRecyclerViewInterface) {
                super(itemView);
                moveNo = itemView.findViewById(R.id.moveNo);
                move = itemView.findViewById(R.id.move);

                move.setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        pgnRecyclerViewInterface.jumpToMove(adapterPosition);
                        move.setBackgroundResource(R.drawable.pgn_move_highlight);
                    }
                });
            }
        }
    }
}