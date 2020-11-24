package com.matt.forgehax.util.text;

public class FullwidthHelper {
    public static char toFullwidthFancyChat(char c) {
        if(c >= 0x0021 && c <= 0x007E) c += 0xFEE0;
        else c = 0;
        return c;
    }

    public static String toFullwidthFancyChat(String s) {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray()) {
            if (toFullwidthFancyChat(c) != 0)
                sb.append(toFullwidthFancyChat(c));
            else sb.append(c);
        }
        return sb.toString();
    }

    public static char revertFullwidthFancyChat(char c) {
        if(c >= 0xFF01 && c <= 0xFF5E) c -= 0xFEE0;
        else c = 0;
        return c;
    }

    public static String revertFullwidthFancyChat(String s) {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray()) {
            if (revertFullwidthFancyChat(c) != 0)
                sb.append(revertFullwidthFancyChat(c));
            else sb.append(c);
        }
        return sb.toString();
    }
}
