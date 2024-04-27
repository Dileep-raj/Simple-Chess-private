package com.drdedd.simplechess_temp.interfaces;

/**
 * Interface for saved games recyclerview
 */
public interface GameRecyclerViewInterface {
    /**
     * Delete the selected game
     *
     * @param position Position of the game file in the recycler view
     */
    void deleteGame(int position);

    /**
     * Open a selected game
     *
     * @param position Position of the game file in recycler view
     */
    void openGame(int position);
}