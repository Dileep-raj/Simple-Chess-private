package com.drdedd.simplechess_temp.data;

import java.util.regex.Pattern;

public class Regexes {
    public static final String tagsRegex = "\\[(\\w*) \"([-\\w.,/* ]*)\"]";
    public static final Pattern tagsPattern = Pattern.compile(tagsRegex);

    //    public static final String commentsRegex = "(\\{[\\w!@#$%^&*();:,.<>\"'|/?\\-=+\\[\\]\\s]*\\}(\\s?[0-9]*\\.{3})?)|(;\\s[\\w!@#$%^&*();:,.<>\"'|/?\\-=+\\[\\]\\s]*\\s[0-9]*\\.{3})";
    public static final String commentsRegex = "\\{.*?\\}(\\s?\\([\\w\\s+=#-]*\\))?(\\s?[0-9]*\\.{3})?";

    public static final String singleMoveRegex = "[KQRNBP]?[a-h]?[1-8]?x?[a-h][1-8](=[QRNB])?[+#]?|O-O|O-O-O";
    public static final Pattern singleMovePattern = Pattern.compile(singleMoveRegex);

    public static final String resultRegex = "(1/2-1/2|\\*|0-1|1-0)\\s*$";
    public static final Pattern resultPattern = Pattern.compile(resultRegex);

    public static final String FENRegex = "^([kqbnrp1-8]+/){7}[kqbnrp1-8]+ [wb] (-|[kq]+) (-|[a-h][1-8])( (-|[0-9]+) (-|[0-9]*))?";
    public static final Pattern FENPattern = Pattern.compile(FENRegex, Pattern.CASE_INSENSITIVE);

    public static final String activePlayerRegex = "\\sw|b\\s";
    public static final Pattern activePlayerPattern = Pattern.compile(activePlayerRegex);
}
