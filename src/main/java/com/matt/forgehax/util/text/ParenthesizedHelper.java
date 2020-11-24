package com.matt.forgehax.util.text;

public class ParenthesizedHelper {
    public static char toParenthesized(char c) {
        if(c >= 0x0031 && c <= 0x0039) c+= 0x2443; //Numbers 1-9
        else if(c >= 0x0061 && c <= 0x007A) c += 0x243B; //Letters a-z
        else c = 0;
        return c;
    }

    public static String toParenthesized(String s) {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray()) {
            if(toParenthesized(c) != 0)
                sb.append(toParenthesized(c));
            else sb.append(c);
        }
        return sb.toString();
    }

    public static String revertParenthesized(char c) {
        StringBuilder sb = new StringBuilder();
        if(c >= 0x2474 && c <= 0x247C) c -= 0x2443; //Numbers 1-9
        else if(c >= 0x249C && c <= 0x24B5) c -= 0x243B; //Letters a-z
        else if(c >= 0x247D && c <= 0x2486) { sb.append("1"); c -= 0x244D; } //Parenthesized 10-19
        else if(c == 0x2487) sb.append("20"); //Parenthesized 20
        else return null;
        sb.append(c);
        return sb.toString();
    }

    public static String revertParenthesized(String s) {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray()) {
            if(revertParenthesized(c) != null)
                sb.append(revertParenthesized(c));
            else sb.append(c);
        }
        return sb.toString();
    }
}
