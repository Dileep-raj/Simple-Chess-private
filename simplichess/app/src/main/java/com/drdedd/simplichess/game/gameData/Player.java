package com.drdedd.simplichess.game.gameData;

/**
 * Player type (White|Black)
 */
public enum Player {
    /**
     * White player
     */
    WHITE("White", false),
    /**
     * Black player
     */
    BLACK("Black", false);
    private boolean inCheck;
    private String name;

    Player(String name, boolean inCheck) {
        this.name = name;
        this.inCheck = inCheck;
    }

    /**
     * @return Player's name
     */
    public String getName() {
        return name;
    }

    /**
     * Set Player's name
     *
     * @param name Name of the player
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Whether a player is in check
     */
    public boolean isInCheck() {
        return inCheck;
    }

    /**
     * Set check flag <code>(true|false)</code>
     */
    public void setInCheck(boolean inCheck) {
        this.inCheck = inCheck;
    }
}

