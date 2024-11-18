package com.drdedd.simplichess.game.gameData;

import androidx.annotation.DrawableRes;

import com.drdedd.simplichess.R;

/**
 * Collection of chess annotations
 */
public enum Annotation {

    /**
     * Best move (Custom annotation of Simpli Chess)
     */
    BEST("$9", "$9", R.drawable.best),
    /**
     * Blunder move <b color="#D02323">??</b>
     */
    BLUNDER("$4", "??", R.drawable.blunder),
    /**
     * Book move (Custom annotation of Simpli Chess)
     */
    BOOK("$0", "$0", R.drawable.book),
    /**
     * Brilliant move <b color="#44DADA">!!</b>
     */
    BRILLIANT("$3", "!!", R.drawable.brilliant),
    /**
     * Dubious move <b color="#FDED30">?!</b>
     */
    INACCURACY("$6", "?!", R.drawable.inaccuracy),
    /**
     * Great move <b color="#22AAD0">!</b>
     */
    GREAT("$1", "!", R.drawable.great),
    /**
     * Interesting move <b color="#2290A5">!?</b>
     */
    INTERESTING("$5", "!?", R.drawable.interesting),
    /**
     * Mistake <b color="#FFA600">?</b>
     */
    MISTAKE("$2", "?", R.drawable.mistake);
    private static final Annotation[] annotations = values();

    private final String number, annotation;
    private final @DrawableRes int resID;

    Annotation(String number, String annotation, @DrawableRes int resID) {
        this.number = number;
        this.annotation = annotation;
        this.resID = resID;
    }

    /**
     * @return <code>String</code> - Number notation of the annotation
     */
    public String getNumber() {
        return number;
    }

    /**
     * @return <code>String</code> - General notation of annotation
     */
    public String getAnnotation() {
        return annotation;
    }

    /**
     * @return <code>int</code> - Resource id of the annotation
     */
    public int getResID() {
        return resID;
    }

    /**
     * @param s Annotation or numbered annotation
     * @return <code>Annotation|null</code>
     */
    public static Annotation getAnnotation(String s) {
        for (Annotation a : annotations)
            if (a.getAnnotation().equals(s) || a.getNumber().equals(s) || a.toString().equalsIgnoreCase(s))
                return a;
        return null;
    }
}