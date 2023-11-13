package com.drdedd.simplechess_temp.GameData;

/**
 * Player type and state of check (White/Black)
 */
public enum Player {
    WHITE(false), BLACK(false);
    boolean inCheck;

    Player(boolean inCheck) {
        this.inCheck = inCheck;
    }

    public boolean isInCheck() {
        return inCheck;
    }

    public void setInCheck(boolean inCheck) {
        this.inCheck = inCheck;
    }
}

