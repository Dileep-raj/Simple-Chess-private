package com.drdedd.simplechess_temp.pieces;

import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.interfaces.BoardInterface;

/**
 * {@inheritDoc}
 */
public class Queen extends Piece {

    /**
     * Creates a new <code>Queen</code> piece
     *
     * @param player Player type (<code>WHITE|BLACK</code>)
     * @param row    Row number of the piece
     * @param col    Column number of the piece
     * @param resID  Resource ID of the piece
     */
    public Queen(Player player, int row, int col, int resID, String unicode) {
        super(player, row, col, Rank.QUEEN, resID, unicode);
    }

    @Override
    public boolean canMoveTo(BoardInterface boardInterface, int row, int col) {
        return possibleMoves.contains(row * 8 + col);
    }

    @Override
    public boolean canCapture(BoardInterface boardInterface, Piece capturingPiece) {
        return canMoveTo(boardInterface, capturingPiece.getRow(), capturingPiece.getCol());
    }


    @Override
    public void updatePossibleMoves(BoardInterface boardInterface) {
        possibleMoves.clear();
        int i, j;
//        Column top
        for (i = getRow() + 1, j = getCol(); i < 8; i++)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Column bottom
        for (i = getRow() - 1, j = getCol(); i >= 0; i--)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Row right
        for (i = getRow(), j = getCol() + 1; j < 8; j++)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Row left
        for (i = getRow(), j = getCol() - 1; j >= 0; j--)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Top right diagonal
        for (i = getRow() + 1, j = getCol() + 1; i < 8 && j < 8; i++, j++)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Bottom left diagonal
        for (i = getRow() - 1, j = getCol() - 1; i >= 0 && j >= 0; i--, j--)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Bottom right diagonal
        for (i = getRow() - 1, j = getCol() + 1; i >= 0 && j < 8; i--, j++)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Top left diagonal
        for (i = getRow() + 1, j = getCol() - 1; i < 8 && j >= 0; i++, j--)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;
    }
}