package com.drdedd.simplichess.game.pgn;

import com.drdedd.simplichess.game.gameData.Annotation;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;

public class PGNData implements Serializable {
    private final LinkedList<String> sanMoves, uciMoves;
    private LinkedList<String> tempMoves;
    private final LinkedHashMap<String, String> tagsMap;
    private final LinkedHashMap<Integer, String> evalMap, commentsMap, alternateMoveSequence;
    private final LinkedHashMap<Integer, Annotation> annotationMap;

    public PGNData() {
        sanMoves = new LinkedList<>();
        uciMoves = new LinkedList<>();
        tagsMap = new LinkedHashMap<>();
        commentsMap = new LinkedHashMap<>();
        annotationMap = new LinkedHashMap<>();
        alternateMoveSequence = new LinkedHashMap<>();
        evalMap = new LinkedHashMap<>();
        tempMoves = new LinkedList<>();
    }

    public PGNData(LinkedList<String> sanMoves, LinkedList<String> uciMoves, LinkedHashMap<String, String> tagsMap, LinkedHashMap<Integer, String> commentsMap, LinkedHashMap<Integer, Annotation> annotationMap, LinkedHashMap<Integer, String> alternateMoveSequence, LinkedHashMap<Integer, String> evalMap) {
        this.sanMoves = sanMoves;
        this.uciMoves = uciMoves;
        this.tagsMap = tagsMap;
        this.commentsMap = commentsMap;
        this.annotationMap = annotationMap;
        this.alternateMoveSequence = alternateMoveSequence;
        this.evalMap = evalMap;
        tempMoves = new LinkedList<>();
    }

    public LinkedList<String> getSANMoves() {
        return sanMoves;
    }

    public LinkedList<String> getUCIMoves() {
        return uciMoves;
    }

    public Set<String> getTags() {
        return tagsMap.keySet();
    }

    public LinkedHashMap<String, String> getTagsMap() {
        return tagsMap;
    }

    public String getTagOrDefault(String tagName, String defaultValue) {
        return tagsMap.getOrDefault(tagName, defaultValue);
    }

    public LinkedHashMap<Integer, String> getCommentsMap() {
        return commentsMap;
    }

    public LinkedHashMap<Integer, Annotation> getAnnotationMap() {
        return annotationMap;
    }

    public LinkedHashMap<Integer, String> getAlternateMoveSequence() {
        return alternateMoveSequence;
    }

    public LinkedHashMap<Integer, String> getEvalMap() {
        return evalMap;
    }

    public LinkedList<String> getTempMoves() {
        return tempMoves;
    }

    public void setTempMoves(LinkedList<String> tempMoves) {
        this.tempMoves = tempMoves;
    }

    public String getLastTempMove() {
        return tempMoves.getLast();
    }

    public void addTempMove(String move) {
        tempMoves.add(move);
    }

    public void addMove(String sanMove, String uciMove) {
        sanMoves.add(sanMove);
        uciMoves.add(uciMove);
    }

    public void addTag(String tag, String value) {
        tagsMap.put(tag, value);
    }

    public void addComment(int moveNo, String comment) {
        commentsMap.put(moveNo, comment);
    }

    public void addAnnotation(int moveNo, Annotation annotation) {
        if (annotation == null) return;
        annotationMap.put(moveNo, annotation);
    }

    public void addAlternateMoveSequence(int moveNo, String alternateMoveSequence) {
        this.alternateMoveSequence.put(moveNo, alternateMoveSequence);
    }

    public void addEval(int moveNo, String eval) {
        evalMap.put(moveNo, eval);
    }
}