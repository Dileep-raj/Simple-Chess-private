package com.drdedd.simplichess.game.pgn;

import androidx.annotation.NonNull;

import com.drdedd.simplichess.game.gameData.Annotation;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class PGNMove {
    private final int n;
    private final String san, uci;
    private String e, cl;
    private Annotation a;
    private ArrayList<String> al, cs;

    /**
     * @param moveNumber     Half move number/ply number
     * @param san            Move in SAN notation
     * @param uci            Move in UCI notation
     * @param eval           Eval of the position
     * @param annotation     Annotation of the move
     * @param alternateMoves Alternate move sequences
     * @param comments       Comments on the move
     */
    public PGNMove(int moveNumber, String san, String uci, String eval, Annotation annotation, ArrayList<String> alternateMoves, ArrayList<String> comments) {
        n = moveNumber;
        this.san = san;
        this.uci = uci;
        e = eval;
        a = annotation;
        al = alternateMoves;
        cs = comments;
    }

    public void setAnnotation(Annotation a) {
        this.a = a;
    }

    public void setEval(String eval) {
        e = eval;
    }

    public void setClock(String clock) {
        cl = clock;
    }

    public void setAlternateMoves(ArrayList<String> a) {
        al = a;
    }

    public void addAlternateMoves(String a) {
        al.add(a);
    }

    public void setComments(ArrayList<String> c) {
        cs = c;
    }

    public void addComment(String c) {
        cs.add(c);
    }

    public String getSAN() {
        return san;
    }

    public String getUCI() {
        return uci;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s %s%s%s%s ", n / 2 + 1 + n % 2 == 0 ? "." : "...", san, a == null ? "" : a.getAnnotation(), al(), c());
    }

    private String al() {
        if (al == null || al.isEmpty()) return "";
        return al.stream().map(a -> String.format(" (%s)", a)).collect(Collectors.joining());
    }

    private String c() {
        if (cs == null || cs.isEmpty()) return "";
        return cs.stream().map(c -> String.format(" {%s}", c)).collect(Collectors.joining());
    }
}