package com.drdedd.simplichess.game.pieces;

import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.gameData.Rank;
import com.drdedd.simplichess.interfaces.GameLogicInterface;

import java.util.HashSet;

/**
 * {@inheritDoc}
 * ♗ ♝
 */
public class Bishop extends Piece {

    /**
     * Creates a new <code>Bishop</code> piece
     *
     * @param player Player type (<code>WHITE|BLACK</code>)
     * @param row    Row number of the piece
     * @param col    Column number of the piece
     * @param resID  Resource ID of the piece
     */
    public Bishop(Player player, int row, int col, int resID, String unicode) {
        super(player, row, col, Rank.BISHOP, resID, unicode);
    }

    @Override
    public boolean canMoveTo(GameLogicInterface gameLogicInterface, int row, int col) {
        return getPossibleMoves(gameLogicInterface).contains(row * 8 + col);
    }

    @Override
    public boolean canCapture(GameLogicInterface gameLogicInterface, Piece capturingPiece) {
        return canMoveTo(gameLogicInterface, capturingPiece.getRow(), capturingPiece.getCol());
    }

    @Override
    public HashSet<Integer> getPossibleMoves(GameLogicInterface gameLogicInterface) {
        HashSet<Integer> possibleMoves = new HashSet<>();
        int i, j;
//        Top right diagonal
        for (i = getRow() + 1, j = getCol() + 1; i < 8 && j < 8; i++, j++)
            if (!addMove(possibleMoves, gameLogicInterface.pieceAt(i, j), i, j)) break;
//        Bottom left diagonal
        for (i = getRow() - 1, j = getCol() - 1; i >= 0 && j >= 0; i--, j--)
            if (!addMove(possibleMoves, gameLogicInterface.pieceAt(i, j), i, j)) break;
//        Bottom right diagonal
        for (i = getRow() - 1, j = getCol() + 1; i >= 0 && j < 8; i--, j++)
            if (!addMove(possibleMoves, gameLogicInterface.pieceAt(i, j), i, j)) break;
//        Top left diagonal
        for (i = getRow() + 1, j = getCol() - 1; i < 8 && j >= 0; i++, j--)
            if (!addMove(possibleMoves, gameLogicInterface.pieceAt(i, j), i, j)) break;
        return possibleMoves;
    }
}