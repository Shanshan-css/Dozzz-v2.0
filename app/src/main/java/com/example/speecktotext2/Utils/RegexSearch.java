package com.example.speecktotext2.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSearch {
    public static boolean searchOrNot(String s, String patternStr){
        Pattern p = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(s);
        return m.find();
    }
}
