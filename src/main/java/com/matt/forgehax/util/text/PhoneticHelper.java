package com.matt.forgehax.util.text;

/* Moved and tweaked Babbaj's "small FancyChat" here */

public class PhoneticHelper {
    private static final char[] alphabet = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
            'y', 'z'
    };

    private static final char[] fancyAlphabet = {
            0x1D43, 0x1D47, 0x1D9C, 0x1D48, 0x1D49, 0x1DA0, 0x1D4D, 0x2B0, 0x1DA4, 0x2B2, 0x1D4F, 0x02E1,
            0x1D50, 0x1DAF, 0x1D52, 0x1D56, 0x1DA3, 0x2B3, 0x2E2, 0x1D57, 0x1D58, 0x1D5B, 0x02B7, 0x02E3,
            0x02B8, 0x1DBB
    };

    public static char toPhoneticFancyChat(char c) {
        for(int i = 0; i < alphabet.length; i++)
            if(c == alphabet[i])
                return fancyAlphabet[i];
        return 0; //Placeholder character that indicates failure
    }

    public static String toPhoneticFancyChat(String s) {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray()) {
            if(toPhoneticFancyChat(c) != 0)
                sb.append(toPhoneticFancyChat(c));
            else sb.append(c);
        }
        return sb.toString();
    }

    public static char revertPhoneticFancyChat(char c) {
        for(int i = 0; i < fancyAlphabet.length; i++)
            if(c == fancyAlphabet[i])
                return alphabet[i];
        return 0; //Placeholder character that indicates failure
    }

    public static String revertPhoneticFancyChat(String s) {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray()) {
            if(revertPhoneticFancyChat(c) != 0)
                sb.append(revertPhoneticFancyChat(c));
            else sb.append(c);
        }
        return sb.toString();
    }

    public static char[] getAlphabet() {
        return alphabet;
    }

    public static char[] getFancyAlphabet() {
        return fancyAlphabet;
    }
}
