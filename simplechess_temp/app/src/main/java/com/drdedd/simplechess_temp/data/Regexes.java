package com.drdedd.simplechess_temp.data;

import java.util.regex.Pattern;

public class Regexes {
    public static final String tagsRegex = "\\[(\\w*) \"([-\\w.,/* ]*)\"]";
    public static final Pattern tagsPattern = Pattern.compile(tagsRegex);

//    public static final String commentsRegex = "\\{.*?\\}(\\s?\\([\\w\\s+=#-]*\\))?(\\s?[0-9]*\\.{3})?";

    //    "^[^]]*?.*?1\\."
    public static final String startingMoveRegex = "1\\.\\s*[NP]?[a-h][1-8]";
    public static final Pattern startingMovePattern = Pattern.compile(startingMoveRegex, Pattern.MULTILINE);

    public static final String singleMoveStrictRegex = "^(\\d+\\.)??([KQRNBP]?[a-h]?[1-8]?x?[a-h][1-8](=?[QRNB])?|O-O-O|O-O)[+#]?(!!|\\?\\?|\\?!|!\\?|!|\\?)?$";

    public static final String moveNumberRegex = "\\d+\\.";
    public static final String moveNumberStrictRegex = "^\\d+\\.$";
    public static final String commentNumberStrictRegex = "^\\d+\\.{3}$";

    public static final String resultRegex = "(1/2-1/2|\\*|0-1|1-0)\\s*$";
    public static final Pattern resultPattern = Pattern.compile(resultRegex);

    public static final String FENRegex = "^([kqbnrp1-8]+/){7}[kqbnrp1-8]+ [wb] (-|[kq]+) (-|[a-h][1-8])( (-|[0-9]+) (-|[0-9]*))?";
    public static final Pattern FENPattern = Pattern.compile(FENRegex, Pattern.CASE_INSENSITIVE);

    public static final String activePlayerRegex = "\\sw|b\\s";
    public static final Pattern activePlayerPattern = Pattern.compile(activePlayerRegex);

    public static final String moveAnnotationRegex = "!!|\\?\\?|\\?!|!\\?|!|\\?";
    public static final Pattern moveAnnotationPattern = Pattern.compile(moveAnnotationRegex);

    public static final String numberedAnnotationRegex = "^\\$\\d+$";
}