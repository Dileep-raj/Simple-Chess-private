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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isInCheck() {
        return inCheck;
    }

    public void setInCheck(boolean inCheck) {
        this.inCheck = inCheck;
    }
}

