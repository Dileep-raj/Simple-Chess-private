package com.drdedd.simplechess_temp.GameData;

/**
 * Player type (White/Black)
 */
public enum Player {
    WHITE("White", false), BLACK("Black", false);
    boolean inCheck;
    String name;

    Player(String name, boolean inCheck) {
        this.name = name;
        this.inCheck = inCheck;
    }

    /**
     * Player's name
     */
    public String getName() {
        return name;
    }

    /**
     * Set Player's name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns whether a player is in check
     */
    public boolean isInCheck() {
        return inCheck;
    }

    /**
     * Set check flag
     */
    public void setInCheck(boolean inCheck) {
        this.inCheck = inCheck;
    }
}

