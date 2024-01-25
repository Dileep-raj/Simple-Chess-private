package com.drdedd.simplechess_temp.GameData;

/**
 * Piece rank (King, Queen, Rook, Bishop, Knight, Pawn)
 */
public enum Rank {
    KING(Integer.MAX_VALUE), QUEEN(9), ROOK(5), BISHOP(3), KNIGHT(3), PAWN(1);
    private final int value;

    Rank(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
