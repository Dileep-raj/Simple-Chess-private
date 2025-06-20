package com.drdedd.simplichess.interfaces;

/**
 * Interface for PGN recyclerview
 */
public interface PGNRecyclerViewInterface {

    /**
     * Goes to the position of the selected move on the board
     *
     * @param position position of the selected move
     */
    void jumpToMove(int position);

    /**
     * @return Position of the current move
     */
    int getPosition();
}
