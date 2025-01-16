package com.drdedd.simplichess.game.pgn;

import static com.drdedd.simplichess.data.Regexes.commentNumberStrictRegex;
import static com.drdedd.simplichess.data.Regexes.moveAnnotationPattern;
import static com.drdedd.simplichess.data.Regexes.moveNumberRegex;
import static com.drdedd.simplichess.data.Regexes.moveNumberStrictRegex;
import static com.drdedd.simplichess.data.Regexes.numberedAnnotationRegex;
import static com.drdedd.simplichess.data.Regexes.resultRegex;
import static com.drdedd.simplichess.data.Regexes.singleMoveStrictRegex;
import static com.drdedd.simplichess.data.Regexes.startingMovePattern;
import static com.drdedd.simplichess.misc.MiscMethods.toNotation;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.drdedd.simplichess.data.Regexes;
import com.drdedd.simplichess.dialogs.ProgressBarDialog;
import com.drdedd.simplichess.fragments.HomeFragment;
import com.drdedd.simplichess.game.GameLogic;
import com.drdedd.simplichess.game.Openings;
import com.drdedd.simplichess.game.ParsedGame;
import com.drdedd.simplichess.game.gameData.Annotation;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.gameData.Rank;
import com.drdedd.simplichess.game.pieces.King;
import com.drdedd.simplichess.game.pieces.Pawn;
import com.drdedd.simplichess.game.pieces.Piece;
import com.drdedd.simplichess.misc.Constants;
import com.drdedd.simplichess.misc.MiscMethods;
import com.drdedd.simplichess.misc.lichess.LichessAPI;
import com.drdedd.simplichess.misc.lichess.LichessGame;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PGN parser to validate PGN moves and convert to game objects
 */
public class PGNParser extends Thread {
    private static final String TAG = "PGNParser", dialogTitle = "Loading PGN";
    private final LinkedList<String> invalidWords;
    private final Context context;
    private final Bundle data;
    private final String pgnContent;
    private final Handler handler;
    private final ProgressBarDialog progressBarDialog;
    private final PGNData pgnData;
    private LichessGame lichessGame;
    private GameLogic gameLogic;

    /**
     * @param context    Context of the application
     * @param pgnContent PGN in <code>String</code> format
     * @param handler    Handler to return results
     */
    public PGNParser(Context context, String pgnContent, Handler handler) {
        this.context = context;
        this.pgnContent = pgnContent;
        this.handler = handler;
        pgnData = new PGNData();
        invalidWords = new LinkedList<>();
        data = new Bundle();
        progressBarDialog = new ProgressBarDialog(context, dialogTitle, false);
        progressBarDialog.show();
    }

    @Override
    public void run() {
        super.run();
        boolean lichess, readResult;
        long start = 0, end = 0;

        if (MiscMethods.isLichessLink(pgnContent)) {
            if (MiscMethods.isConnected(context)) {
                Log.d(TAG, "inputPGNDialog: Given lichess game link");
                start = System.nanoTime();
                lichessGame = LichessGame.parse(LichessAPI.getGameJSONById(LichessAPI.extractCode(pgnContent)));
                end = System.nanoTime();
                lichess = true;
            } else {
                data.putString(Constants.ERROR_KEY, "Couldn't connect to internet");
                Message message = new Message();
                message.setData(data);
                handler.sendMessage(message);
                if (progressBarDialog != null) progressBarDialog.dismiss();
                return;
            }
        } else if (pgnContent.matches(Regexes.lichessGameCodeRegex)) {
            if (MiscMethods.isConnected(context)) {
                Log.d(TAG, "inputPGNDialog: Given lichess game id");
                start = System.nanoTime();
                lichessGame = LichessGame.parse(LichessAPI.getGameJSONById(pgnContent));
                end = System.nanoTime();
                lichess = true;
            } else {
                data.putString(Constants.ERROR_KEY, "Couldn't connect to internet");
                Message message = new Message();
                message.setData(data);
                handler.sendMessage(message);
                if (progressBarDialog != null) progressBarDialog.dismiss();
                return;
            }
        } else lichess = false;

        if (lichess) {
            if (lichessGame != null && lichessGame.isValid()) {
                readResult = true;
                pgnData.setTempMoves(new LinkedList<>(lichessGame.getMoves()));

                LichessGame.LichessPlayer white = lichessGame.getWhite(), black = lichessGame.getBlack(), winner = lichessGame.getWinner();
                if (white != null) {
                    pgnData.addTag(PGN.TAG_WHITE, white.getName());
                    pgnData.addTag(PGN.TAG_WHITE_TITLE, white.getTitle());
                    pgnData.addTag(PGN.TAG_WHITE_ELO, white.getElo());
                }
                if (black != null) {
                    pgnData.addTag(PGN.TAG_BLACK, black.getName());
                    pgnData.addTag(PGN.TAG_BLACK_TITLE, black.getTitle());
                    pgnData.addTag(PGN.TAG_BLACK_ELO, black.getElo());
                }

                if (lichessGame.getWinner() != null) {
                    if (winner == white) pgnData.addTag(PGN.TAG_RESULT, PGN.RESULT_WHITE_WON);
                    else if (winner == black) pgnData.addTag(PGN.TAG_RESULT, PGN.RESULT_BLACK_WON);
                }

                if (lichessGame.isGameAnalysed()) {
                    LichessGame.PlayerAccuracy whiteAccuracy = lichessGame.getWhiteAccuracy(), blackAccuracy = lichessGame.getBlackAccuracy();

                    data.putInt(Constants.WHITE_INACCURACY, whiteAccuracy.getInaccuracy());
                    data.putInt(Constants.BLACK_INACCURACY, blackAccuracy.getInaccuracy());

                    data.putInt(Constants.WHITE_MISTAKE, whiteAccuracy.getMistake());
                    data.putInt(Constants.BLACK_MISTAKE, blackAccuracy.getMistake());

                    data.putInt(Constants.WHITE_BLUNDER, whiteAccuracy.getBlunder());
                    data.putInt(Constants.BLACK_BLUNDER, blackAccuracy.getBlunder());

                    data.putInt(Constants.WHITE_ACPL, whiteAccuracy.getACPL());
                    data.putInt(Constants.BLACK_ACPL, blackAccuracy.getACPL());

                    data.putInt(Constants.WHITE_ACCURACY, whiteAccuracy.getAccuracy());
                    data.putInt(Constants.BLACK_ACCURACY, blackAccuracy.getAccuracy());
                }
            } else {
                readResult = false;
                data.putString(Constants.ERROR_KEY, "Lichess game not found!");
            }
        } else {
            start = System.nanoTime();
            readResult = readPGN();
            end = System.nanoTime();
        }

        data.putBoolean(Constants.READ_RESULT_KEY, readResult);
        if (readResult) {
            HomeFragment.printTime(TAG, "Reading PGN", end - start, pgnContent.length());
            Log.v(TAG, "run: No syntax errors in PGN");

            gameLogic = new GameLogic(context, pgnData);

            start = System.nanoTime();
            boolean parseResult = parsePGN();
            end = System.nanoTime();

//            data.putBoolean(PARSE_RESULT_FLAG, parseResult);
//            if (parseResult) {
            Log.d(TAG, String.format("run: Time to Parse: %,3d ns", end - start));
            Log.d(TAG, String.format("run: Game valid and parsed!%nFinal position:%s", gameLogic.getBoardModel()));

            String opening, eco;
            if (gameLogic.getPGN().isFENEmpty()) {
                start = System.nanoTime();
                Openings openings = Openings.getInstance(context);
                String openingResult = openings.searchOpening(gameLogic.getPGN().getUCIMoves());
                end = System.nanoTime();

                String[] split = openingResult.split(Openings.separator);
                int lastBookMove = Integer.parseInt(split[0]);
                if (lastBookMove != -1 && split.length == 3) {
                    HomeFragment.printTime(TAG, "searching opening", end - start, lastBookMove);
                    eco = split[1];
                    opening = split[2];
                    gameLogic.getPGN().setLastBookMoveNo(lastBookMove);
                    gameLogic.getPGN().addTag(PGN.TAG_ECO, eco);
                    gameLogic.getPGN().addTag(PGN.TAG_OPENING, opening);
                    for (int i = 0; i <= lastBookMove; i++)
                        gameLogic.getPGN().getPGNData().addAnnotation(i, Annotation.BOOK);
                } else {
                    opening = eco = "";
                    Log.d(TAG, String.format("readPGN: Opening not found!\n%s\nMoves: %s", Arrays.toString(split), gameLogic.getPGN().getUCIMoves().subList(0, Math.min(gameLogic.getPGN().getUCIMoves().size(), 10))));
                }
            } else opening = eco = "";

            data.putSerializable(Constants.PARSED_GAME_KEY, new ParsedGame(gameLogic.getBoardModelStack(), gameLogic.getFENs(), gameLogic.getPGN(), eco, opening));
//            } else Log.d(TAG, "run: Game not parsed!");
        }

        Log.d(TAG, "run: Total invalid words: " + invalidWords.size());
        Message message = new Message();
        message.setData(data);
        handler.sendMessage(message);
        if (progressBarDialog != null) progressBarDialog.dismiss();
    }

    /**
     * Reads each word in the PGN and checks syntax
     *
     * @return <code>true|false</code> - PGN is syntactically valid
     */
    private boolean readPGN() {
        int moveCount = -1;
        readTags(pgnContent);

        Matcher startingMoveMatcher = startingMovePattern.matcher(pgnContent);
        boolean foundMoves = startingMoveMatcher.find();
        if (!foundMoves) {
            String error = "No moves in PGN!";
            Log.v(TAG, String.format("readPGN: %s\n%s", error, pgnContent));
            data.putString(Constants.ERROR_KEY, error);
            return false;
        }

        Scanner PGNReader = new Scanner(pgnContent.substring(startingMoveMatcher.start()));

//      Iterate through every word in the PGN
        while (PGNReader.hasNext()) {
            String word = null;
            try {
                word = PGNReader.next();

//              If comment is found, extract full comment
                if (word.startsWith("{")) {
                    StringBuilder commentBuilder = new StringBuilder(word);
                    while (PGNReader.hasNext()) {
                        if (word.endsWith("}")) break;
                        else if (word.contains("}")) {
                            commentBuilder = new StringBuilder(word.substring(0, word.indexOf('}') + 1));
                            if (word.contains("("))
                                extractAlternateMoves(moveCount, word.substring(word.indexOf('(')), PGNReader);
                            break;
                        }

                        word = PGNReader.next();
                        if (word.contains("}")) {
                            commentBuilder.append(' ').append(word, 0, word.indexOf('}') + 1);
                            if (word.contains("("))
                                extractAlternateMoves(moveCount, word.substring(word.indexOf('(')), PGNReader);
                            break;
                        }
                        commentBuilder.append(' ').append(word);
                    }
                    String comment = commentBuilder.toString();
//                    commentsMap.put(moveCount, comment);
                    pgnData.addComment(moveCount, comment);
                    findMoveFeedback(comment, moveCount);
                    findEval(comment, moveCount);
                    continue;
                }

                if (word.startsWith("(")) {
                    extractAlternateMoves(moveCount, word, PGNReader);
                    continue;
                }

//              If a move is found add move to the moves list
                if (word.matches(singleMoveStrictRegex)) {
                    String move = word.replaceAll(moveNumberRegex, "");
                    pgnData.addTempMove(move);
                    moveCount++;
                    findMoveFeedback(word, moveCount);
                    continue;
                }

                if (word.matches(numberedAnnotationRegex)) {
                    pgnData.addAnnotation(moveCount, Annotation.getAnnotation(word));
                    continue;
                }

                if (word.matches(resultRegex)) {
//                    evalMap.put(moveCount, word);
                    pgnData.addEval(moveCount, word);
                    continue;
                }

                if (word.matches(moveNumberStrictRegex) || word.matches(commentNumberStrictRegex))
                    continue;
                invalidWords.add(word + ",after move: " + pgnData.getLastTempMove());
            } catch (Exception e) {
                String error = "Error at :" + word;
                Log.e(TAG, "readPGN: " + error, e);
                data.putString(Constants.ERROR_KEY, error);
            }
        }
        return true;
    }

    /**
     * Parses and converts PGN to game objects, by evaluating each move validity
     *
     * @return <code>true|false</code> - Whether all PGN moves are valid
     */
    public boolean parsePGN() {
        char ch;
        LinkedList<String> moves = pgnData.getTempMoves();
        int i, startRow, startCol, destRow, destCol, totalMoves = moves.size(), moveNo = 0;
        boolean promotion;
        Rank rank = null, promotionRank;
        Piece piece;
        Player player;

        for (String move : moves) {
            if (progressBarDialog != null) progressBarDialog.updateProgress(++moveNo, totalMoves);
            move = move.trim();
            Log.v(TAG, "parsePGN: Move: " + move);
            startRow = -1;
            startCol = -1;
            destRow = -1;
            destCol = -1;
            promotion = false;
            promotionRank = null;
            piece = null;
            player = gameLogic.playerToPlay();

            if (lichessGame != null && lichessGame.isGameAnalysed()) {
                LichessGame.MoveAnalysis moveAnalysis = lichessGame.getMoveAnalyses().get(moveNo - 1);
                Log.d(TAG, String.format("parsePGN: MoveAnalysis: %s", moveAnalysis));
                pgnData.addComment(moveNo - 1, "{" + moveAnalysis + '}');
                pgnData.addAnnotation(moveNo - 1, Annotation.getAnnotation(moveAnalysis.getAnnotation()));
                pgnData.addEval(moveNo - 1, moveAnalysis.getEval());
            }

            try {
                if (move.startsWith(PGN.LONG_CASTLE)) {
                    King king = gameLogic.isWhiteToPlay() ? gameLogic.getBoardModel().getWhiteKing() : gameLogic.getBoardModel().getBlackKing();
                    if (king.canLongCastle(gameLogic))
                        Log.d(TAG, String.format("parsePGN: Move: Long castle %s!", gameLogic.move(king.getRow(), king.getCol(), king.getRow(), king.getCol() - 2) ? "success" : "failed"));
                    continue;
                }
                if (move.startsWith(PGN.SHORT_CASTLE)) {
                    King king = gameLogic.isWhiteToPlay() ? gameLogic.getBoardModel().getWhiteKing() : gameLogic.getBoardModel().getBlackKing();
                    if (king.canShortCastle(gameLogic)) {
                        Log.d(TAG, String.format("parsePGN: Move: Short castle %s!", gameLogic.move(king.getRow(), king.getCol(), king.getRow(), king.getCol() + 2) ? "success" : "failed"));
                    }
                    continue;
                }

                ch = move.charAt(0);
                if (Character.isLetter(ch)) switch (ch) {
                    case 'K':
                        rank = Rank.KING;
                        break;
                    case 'Q':
                        rank = Rank.QUEEN;
                        break;
                    case 'R':
                        rank = Rank.ROOK;
                        break;
                    case 'N':
                        rank = Rank.KNIGHT;
                        break;
                    case 'B':
                        rank = Rank.BISHOP;
                        break;
                    case 'P':
                    default:
                        rank = Rank.PAWN;
                }

                label:
                for (i = 0; i < move.length(); i++) {
                    ch = move.charAt(i);
                    switch (ch) {
                        case 'a':
                        case 'b':
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'f':
                        case 'g':
                        case 'h':
                            if (destCol != -1) startCol = destCol;
                            destCol = ch - 'a';
                            break;

                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                            if (destRow != -1) startRow = destRow;
                            destRow = ch - '1';
                            break;

                        case '=':
                            promotion = true;
                            ch = move.charAt(i + 1);
                            Log.v(TAG, "parsePGN: Promotion");
                            switch (ch) {
                                case 'Q':
                                    promotionRank = Rank.QUEEN;
                                    break;
                                case 'R':
                                    promotionRank = Rank.ROOK;
                                    break;
                                case 'N':
                                    promotionRank = Rank.KNIGHT;
                                    break;
                                case 'B':
                                    promotionRank = Rank.BISHOP;
                                    break;
                            }
                            Log.v(TAG, "parsePGN: Promotion rank: " + promotionRank);
                            if (promotionRank == null) {
                                data.putString(Constants.ERROR_KEY, "Invalid move " + move);
                                return false;
                            }
                            break label;

                        case 'Q':
                        case 'R':
                        case 'N':
                        case 'B':
                            if (destCol != -1 && destRow != -1) {
                                promotion = true;
                                Log.v(TAG, "parsePGN: Promotion");
                                switch (ch) {
                                    case 'Q':
                                        promotionRank = Rank.QUEEN;
                                        break;
                                    case 'R':
                                        promotionRank = Rank.ROOK;
                                        break;
                                    case 'N':
                                        promotionRank = Rank.KNIGHT;
                                        break;
                                    case 'B':
                                        promotionRank = Rank.BISHOP;
                                        break;
                                }
                                Log.v(TAG, "parsePGN: Promotion rank: " + promotionRank);
                                break label;
                            }

                        case 'K':
                        case 'P':
                        case 'x':
                        case '+':
                        case '#':
                            break;
                    }
                }

                if (startRow != -1 && startCol != -1) {
                    piece = gameLogic.getBoardModel().pieceAt(startRow, startCol);
                    Log.d(TAG, String.format("parsePGN: piece at %s piece: %s", toNotation(startRow, startCol), piece));
                } else if (startCol != -1) {
                    piece = gameLogic.getBoardModel().searchCol(gameLogic, player, rank, startCol, destRow, destCol);
                    Log.d(TAG, String.format("parsePGN: searched col: %d piece:%s", startCol, piece));
                } else if (startRow != -1) {
                    piece = gameLogic.getBoardModel().searchRow(gameLogic, player, rank, startRow, destRow, destCol);
                    Log.d(TAG, String.format("parsePGN: searched row: %d piece: %s", startRow, piece));
                }

                if (piece == null) {
                    piece = gameLogic.getBoardModel().searchPiece(gameLogic, player, rank, destRow, destCol);
                    Log.d(TAG, "parsePGN: piece searched");
                }

                if (piece != null && promotion) {
                    Pawn pawn = (Pawn) piece;
                    if (gameLogic.promote(pawn, destRow, destCol, startRow, startCol, promotionRank)) {
                        Log.d(TAG, String.format("parsePGN: Promoted to %s at %s", promotionRank, toNotation(destRow, destCol)));
                        continue;
                    }
                }

                if (piece != null) {
                    if (gameLogic.move(piece.getRow(), piece.getCol(), destRow, destCol)) {
                        Log.d(TAG, String.format("parsePGN: Move success %s", move));
                    } else {
                        Log.d(TAG, "parsePGN: Second search!");
                        LinkedHashSet<Piece> pieces = gameLogic.getBoardModel().pieces, tempPieces = new LinkedHashSet<>();
                        for (Piece tempPiece : pieces)
                            if (tempPiece.getPlayer() == player && tempPiece.getRank() == rank) {
                                if (startRow != -1 && tempPiece.getRow() == startRow) {
                                    tempPieces.add(tempPiece);
                                } else if (startCol != -1 && tempPiece.getCol() == startCol) {
                                    tempPieces.add(tempPiece);
                                } else tempPieces.add(tempPiece);
                            }

                        for (Piece tempPiece : tempPieces)
                            if (gameLogic.getAllLegalMoves().containsKey(tempPiece.getSquare()) && Objects.requireNonNull(gameLogic.getAllLegalMoves().get(tempPiece.getSquare())).contains(destCol + destRow * 8))
                                piece = tempPiece;

                        if (gameLogic.move(piece.getRow(), piece.getCol(), destRow, destCol))
                            Log.d(TAG, "parsePGN: Move success after 2nd search! " + move);
                        else {
                            StringBuilder legalMoves = new StringBuilder();
                            HashSet<Integer> pieceLegalMoves = gameLogic.getAllLegalMoves().get(piece.getSquare());
                            if (pieceLegalMoves != null) for (int legalMove : pieceLegalMoves)
                                legalMoves.append(toNotation(legalMove)).append(' ');
                            Log.d(TAG, String.format("parsePGN: Move failed: %s%nPiece: %s%nLegalMoves: %s", move, piece, legalMoves));
                            data.putString(Constants.ERROR_KEY, "Invalid move " + move);
                            return false;
                        }
                    }
                } else {
                    data.putString(Constants.ERROR_KEY, "Invalid move " + move);
                    Log.d(TAG, String.format("parsePGN: Move invalid! Piece not found! %s %s (%d,%d) -> %s move: %s", player, rank, startRow, startCol, toNotation(destRow, destCol), move));
//                    Toast.makeText(context, "Invalid move " + move, Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (Exception e) {
//                Toast.makeText(context, "Error occurred after move " + move, Toast.LENGTH_LONG).show();
                data.putString(Constants.ERROR_KEY, "Invalid move " + move);
                Log.e(TAG, "parsePGN: Error occurred after move " + move, e);
                return false;
            }
        }

        Set<String> tags = pgnData.getTags();
        for (String tag : tags) {
            String value = pgnData.getTagOrDefault(tag, null);
            if (value != null && !value.isEmpty()) gameLogic.getPGN().addTag(tag, value);
            Log.d(TAG, String.format("parsePGN: Tag: [%s \"%s\"]", tag, value));
        }
        return true;
    }

    private void extractAlternateMoves(int moveCount, String word, Scanner PGNReader) {
        StringBuilder movesBuilder = new StringBuilder(word.substring(word.indexOf("(")));
        while (PGNReader.hasNext()) {
            if (word.endsWith(")")) break;
            word = PGNReader.next();
            movesBuilder.append(' ').append(word);
            if (word.endsWith(")")) break;
        }
//        alternateMoveSequence.put(moveCount, movesBuilder.toString());
        pgnData.addAlternateMoveSequence(moveCount, movesBuilder.toString());
    }

    /**
     * Extracts move feedback if any
     *
     * @param word      Move word or comment
     * @param moveCount Move number
     */
    private void findMoveFeedback(String word, int moveCount) {
        String feedback = null;
        Matcher feedbackMatcher = moveAnnotationPattern.matcher(word);
        if (feedbackMatcher.find()) feedback = feedbackMatcher.group();
        if (feedback != null) pgnData.addAnnotation(moveCount, Annotation.getAnnotation(feedback));
    }

    private void findEval(String comment, int moveCount) {
        Matcher matcher = Pattern.compile("\\[%eval [-+]?[#M]?-?[\\d.]+]").matcher(comment);
        if (matcher.find()) {
            String group = matcher.group().replace('#', 'M');
            group = group.substring(group.indexOf(" "), group.length() - 1).trim();
            if (group.contains("-")) group = "-" + group.replace("-", "");
            else if (!group.contains("+")) group = "+" + group;
//            evalMap.put(moveCount, group);
            pgnData.addEval(moveCount, group);
        }
    }

    /**
     * Extracts Tags from the PGN
     *
     * @param pgn PGN in <code>String</code> format
     */
    private void readTags(String pgn) {
        Scanner tagReader = new Scanner(pgn);
        String word = null;
        while (tagReader.hasNext()) {
            try {
                word = tagReader.next();
                if (word.startsWith("1.")) return;
                if (word.startsWith("[")) {
                    String tag = word.substring(1);
                    StringBuilder tagBuilder = new StringBuilder();
                    while (tagReader.hasNext()) {
                        word = tagReader.next();
                        tagBuilder.append(word).append(' ');
                        if (word.endsWith("]")) break;
                    }
                    String value = tagBuilder.substring(tagBuilder.indexOf("\"") + 1, tagBuilder.lastIndexOf("\""));
                    pgnData.addTag(tag, value);
                }
            } catch (Exception e) {
                Log.e(TAG, "readTags: Error at : " + word, e);
            }
        }
    }

    public Bundle getData() {
        return data;
    }
}