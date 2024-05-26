package com.drdedd.simplechess_temp.data;

import com.drdedd.simplechess_temp.R;

import java.util.HashMap;
import java.util.Map;

public class MoveAnnotation {
    public static final String BRILLIANT = "!!", GREAT = "!", INTERESTING = "!?", DUBIOUS = "?!", MISTAKE = "?", BLUNDER = "??", BEST = "BEST";
    private static final HashMap<String, String> CHESS_DOT_COM_ANNOTATIONS = new HashMap<>(Map.of("$1", GREAT, "$2", MISTAKE, "$3", BRILLIANT, "$4", BLUNDER, "$5", INTERESTING, "$6", DUBIOUS));
    private static final HashMap<String, Integer> ANNOTATIONS_RESOURCE = new HashMap<>(Map.of(BRILLIANT, R.drawable.brilliant, GREAT, R.drawable.great, INTERESTING, R.drawable.interesting, BEST, R.drawable.best, DUBIOUS, R.drawable.dubious, MISTAKE, R.drawable.mistake, BLUNDER, R.drawable.blunder));

    public static String parseChessDotComAnnotation(String annotation) {
        if (annotation.startsWith("$")) return CHESS_DOT_COM_ANNOTATIONS.get(annotation);
        return annotation;
    }

    public static int getAnnotationResource(String annotation) {
        if (ANNOTATIONS_RESOURCE.containsKey(annotation))
            return ANNOTATIONS_RESOURCE.get(annotation);
        else return -1;
    }
}
