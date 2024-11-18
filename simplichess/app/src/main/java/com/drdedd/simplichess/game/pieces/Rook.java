package com.drdedd.simplichess.game.pieces;

import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.gameData.Rank;
import com.drdedd.simplichess.interfaces.GameLogicInterface;

import java.util.HashSet;

/**
 * {@inheritDoc}
 * ♖ ♜
 */
public class Rook extends Piece {

    /**
     * Creates a new <code>Rook</code> piece
     *
     * @param player Player type (<code>WHITE|BLACK</code>)
     * @param row    Row number of the piece
     * @param col    Column number of the piece
     * @param resID  Resource ID of the piece
     */
    public Rook(Player player, int row, int col, int resID, String unicode) {
        super(player, row, col, Rank.ROOK, resID, unicode);
        moved = false;
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
//        Column top
        for (i = getRow() + 1, j = getCol(); i < 8; i++)
            if (!addMove(possibleMoves, gameLogicInterface.pieceAt(i, j), i, j)) break;
//        Column bottom
        for (i = getRow() - 1, j = getCol(); i >= 0; i--)
            if (!addMove(possibleMoves, gameLogicInterface.pieceAt(i, j), i, j)) break;
//        Row right
        for (i = getRow(), j = getCol() + 1; j < 8; j++)
            if (!addMove(possibleMoves, gameLogicInterface.pieceAt(i, j), i, j)) break;
//        Row left
        for (i = getRow(), j = getCol() - 1; j >= 0; j--)
            if (!addMove(possibleMoves, gameLogicInterface.pieceAt(i, j), i, j)) break;
        return possibleMoves;
    }
}