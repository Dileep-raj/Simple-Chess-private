package com.drdedd.simplechess_temp.GameData;

/**
 * State of the game ({@link ChessState#CHECKMATE Checkmate}, {@link ChessState#DRAW Draw}, {@link ChessState#ONGOING Ongoing}, {@link ChessState#RESIGN Resigned},
 * {@link ChessState#STALEMATE Stalemate}, {@link ChessState#TIMEOUT Timeout})
 */
public enum ChessState {
    /**
     * Game over by checkmate
     */
    CHECKMATE,
    /**
     * Game over by draw
     */
    DRAW,
    /**
     * Game is ongoing
     */
    ONGOING,
    /**
     * Game over by Resignation
     */
    RESIGN,
    /**
     * Game over by stalemate
     */
    STALEMATE,
    /**
     * Game over by timeout
     */
    TIMEOUT
}