package com.drdedd.simplechess_temp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drdedd.simplechess_temp.GameData.ChessState;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.fragments.GameFragment;
import com.drdedd.simplechess_temp.interfaces.PGNRecyclerViewInterface;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * <p>PGN (Portable Game Notation) is a standard text format used to record Chess game moves with standard notations</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Portable_Game_Notation"> More about PGN </a>
 */
public class PGN implements Serializable {
    /**
     * Constant String for special moves
     */
    public static final String LONG_CASTLE = "O-O-O", SHORT_CASTLE = "O-O", CAPTURE = "Capture", PROMOTE = "promote", APP_NAME = "Simple Chess";
    public static final String RESULT_DRAW = "1/2-1/2", RESULT_WHITE_WON = "1-0", RESULT_BLACK_WON = "0-1", RESULT_ONGOING = "*";
    public static final String APP_TAG = "App", WHITE_TAG = "White", DATE_TAG = "Date", BLACK_TAG = "Black", SET_UP_TAG = "SetUp", FEN_TAG = "FEN", RESULT_TAG = "Result", TERMINATION_TAG = "Termination";
    private final String app, date, FEN;
    private String white, black, termination = "";
    private ChessState gameState;
    private final LinkedList<String> moves = new LinkedList<>();
    private String appendResult = "";

    /**
     * @param app       Name of app
     * @param white     Name of White player
     * @param black     Name of Black player
     * @param date      Date of the game
     * @param gameState State of the game
     */
    public PGN(String app, String white, String black, String date, ChessState gameState) {
        this.app = app;
        this.white = white;
        this.black = black;
        this.date = date;
        this.gameState = gameState;
        FEN = "";
        moves.clear();
    }

    /**
     * @param app       Name of app
     * @param white     Name of White player
     * @param black     Name of Black player
     * @param date      Date of the game
     * @param gameState State of the game
     * @param FEN       Starting position of the game
     */
    public PGN(String app, String white, String black, String date, ChessState gameState, String FEN) {
        this.app = app;
        this.white = white;
        this.black = black;
        this.date = date;
        this.gameState = gameState;
        this.FEN = FEN;
        moves.clear();
    }

    public void addToPGN(Piece piece, String move) {
        if (move.isEmpty()) moves.addLast(piece.getPosition());
        else moves.addLast(move);
    }

    public void removeLast() {
        if (!moves.isEmpty()) moves.removeLast();
    }

    public ChessState getGameState() {
        return gameState;
    }

    public void setGameState(ChessState gameState) {
        this.gameState = gameState;
    }

    /**
     * Set White and Black player names
     */
    public void setWhiteBlack(String white, String black) {
        this.white = white;
        this.black = black;
    }

    /**
     * @return <code>String</code> - White player name
     */
    public String getWhite() {
        return white;
    }

    /**
     * @return <code>String</code> - Black player name
     */
    public String getBlack() {
        return black;
    }

    /**
     * Converts PGN to standard text format
     *
     * @return String - PGN with tags and moves
     */
    @NonNull
    @Override
    public String toString() {
        return getTags() + getPGN();
    }

    /**
     * Returns PGN tags with their values
     *
     * @return String - PGN Tags text
     */
    private String getTags() {
        StringBuilder tags = new StringBuilder();
        tags.append(String.format("[%s \"", APP_TAG)).append(app).append("\"]\n");
        tags.append(String.format("[%s \"", DATE_TAG)).append(date).append("\"]\n");
        tags.append(String.format("[%s \"", WHITE_TAG)).append(white).append("\"]\n");
        tags.append(String.format("[%s \"", BLACK_TAG)).append(black).append("\"]\n");
        if (!FEN.isEmpty()) {
            tags.append(String.format("[%s \"", SET_UP_TAG)).append(1).append("\"]\n");
            tags.append(String.format("[%s \"", FEN_TAG)).append(FEN).append("\"]\n");
        }
        tags.append(String.format("[%s  \"", RESULT_TAG)).append(getResult()).append("\"]\n");
        if (!termination.isEmpty())
            tags.append(String.format("[%s \"", TERMINATION_TAG)).append(termination).append("\"]\n");
        return tags.toString();
    }

    /**
     * Returns result of the game
     *
     * @return * | 0-1 | 1-0 | 1/2-1/2
     */
    public String getResult() {
        if (appendResult != null && !appendResult.isEmpty()) return appendResult;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            switch (GameFragment.getGameState()) {
                case WHITE_TO_PLAY:
                case BLACK_TO_PLAY:
                    return "*";
                case RESIGN:
                case TIMEOUT:
                    return termination.contains(Player.WHITE.getName()) ? "1-0" : "0-1";
                case CHECKMATE:
                    return Player.WHITE.isInCheck() ? "0-1" : "1-0";
                case STALEMATE:
                case DRAW:
                    return "1/2-1/2";
            }
        }
        return "*";
    }

    /**
     * Returns PGN without tags and just moves
     *
     * @return String - PGN text
     */
    public String getPGN() {
        StringBuilder pgn = new StringBuilder();
        int length = moves.size();
        for (int i = 0; i < length; i++) {
            if (i % 2 == 0) pgn.append(i / 2 + 1).append('.');
            pgn.append(moves.get(i)).append(' ');
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

    public String getMoveAt(int moveNo) {
        if (moveNo < moves.size()) return moves.get(moveNo);
        return null;
    }

    public String getAppendResult() {
        return appendResult;
    }

    public void setAppendResult(String appendResult) {
        this.appendResult = appendResult;
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
            View view = inflater.inflate(R.layout.recycler_view_pgn, parent, false);
            return new PGNViewHolder(view, pgnRecyclerViewInterface);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull PGNViewHolder holder, int position) {
            holder.moveNo.setVisibility(View.VISIBLE);
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