package com.drdedd.simplechess_temp.GameData;

/**
 * State of the game (White to play, Black to play, Checkmate, Resign, Stalemate, Draw)
 */
public enum ChessState {
    /**
     * White's turn to play
     */
    WHITE_TO_PLAY,
    /**
     * Black's turn to play
     */
    BLACK_TO_PLAY,
    /**
     * Game over by checkmate
     */
    CHECKMATE,
    /**
     * Game over by Resignation
     */
    RESIGN,
    /**
     * Game over by stalemate
     */
    STALEMATE,
    /**
     * Game over by draw
     */
    DRAW,
    /**
     * Game over by timeout
     */
    TIMEOUT
}