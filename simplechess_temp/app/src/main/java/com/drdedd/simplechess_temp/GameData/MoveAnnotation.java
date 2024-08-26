package com.drdedd.simplechess_temp.GameData;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.drdedd.simplechess_temp.R;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Collection of chess annotations
 */
public class MoveAnnotation {
    /**
     * Brilliant move <b>!!</b>
     */
    public static final String BRILLIANT = "!!";
    /**
     * Great move <b>!</b>
     */
    public static final String GREAT = "!";
    /**
     * Interesting move <b>!?</b>
     */
    public static final String INTERESTING = "!?";
    /**
     * Dubious move <b>?!</b>
     */
    public static final String DUBIOUS = "?!";
    /**
     * Mistake <b>?</b>
     */
    public static final String MISTAKE = "?";
    /**
     * Blunder move <b>??</b>
     */
    public static final String BLUNDER = "??";
    /**
     * Best move
     */
    public static final String BEST = "BEST";
    /**
     * Book move
     */
    public static final String BOOK = " $8";
    private static final HashMap<String, String> NUMBERED_ANNOTATIONS = new HashMap<>(Map.of("$1", GREAT, "$2", MISTAKE, "$3", BRILLIANT, "$4", BLUNDER, "$5", INTERESTING, "$6", DUBIOUS, "$8", BOOK));
    private static final HashMap<String, Integer> ANNOTATIONS_RESOURCE = new HashMap<>(Map.of(BRILLIANT, R.drawable.brilliant, GREAT, R.drawable.great, INTERESTING, R.drawable.interesting, BEST, R.drawable.best, DUBIOUS, R.drawable.dubious, MISTAKE, R.drawable.mistake, BLUNDER, R.drawable.blunder, BOOK, R.drawable.book));
    private static final HashMap<Integer, Bitmap> ANNOTATION_BITMAPS = new HashMap<>();
    private static boolean loaded = false;

    /**
     * @param annotation Annotation number
     * @return <code>String</code> form of the numbered annotation
     */
    public static String parseNumberedAnnotation(String annotation) {
        if (annotation.startsWith("$")) return NUMBERED_ANNOTATIONS.get(annotation);
        return annotation;
    }

    /**
     * @param annotation Annotation string
     * @return Resource id of the annotation
     */
    public static int getAnnotationResource(String annotation) {
        if (ANNOTATIONS_RESOURCE.containsKey(annotation))
            return ANNOTATIONS_RESOURCE.get(annotation);
        return -1;
    }

    /**
     * Loads bitmaps of all annotations
     *
     * @param context Context to retrieve resources
     */
    public static void loadBitmaps(Context context) {
        if (loaded) return;
        Resources res = context.getResources();
        Collection<Integer> resIDs = ANNOTATIONS_RESOURCE.values();
        for (int resID : resIDs) {
            Bitmap b = BitmapFactory.decodeResource(res, resID);
            if (b != null) ANNOTATION_BITMAPS.put(resID, b);
        }
        loaded = true;
    }

    /**
     * @param annotation Annotation resource id
     * @return <code>Bitmap</code> of the annotation
     */
    public static Bitmap getBitmap(int annotation) {
        if (ANNOTATION_BITMAPS.containsKey(annotation)) return ANNOTATION_BITMAPS.get(annotation);
        return null;
    }
}
