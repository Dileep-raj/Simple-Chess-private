package com.drdedd.simplichess.game;

import com.drdedd.simplichess.game.pgn.PGN;

import java.io.Serializable;
import java.util.Stack;

public class ParsedGame implements Serializable {
    private final Stack<BoardModel> boardModelStack;
    private final Stack<String> FENs;
    private final PGN pgn;
    private final String eco, opening;

    public ParsedGame(Stack<BoardModel> boardModelStack, Stack<String> FENs, PGN pgn, String eco, String opening) {
        this.boardModelStack = boardModelStack;
        this.FENs = FENs;
        this.pgn = pgn;
        this.eco = eco;
        this.opening = opening;
    }

    public Stack<BoardModel> getBoardModelStack() {
        return boardModelStack;
    }

    public Stack<String> getFENs() {
        return FENs;
    }

    public PGN getPGN() {
        return pgn;
    }

    public String getECO() {
        return eco == null ? "" : eco;
    }

    public String getOpening() {
        return opening == null ? "" : opening;
    }
}
