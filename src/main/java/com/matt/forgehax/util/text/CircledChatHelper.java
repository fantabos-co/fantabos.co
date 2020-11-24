package com.matt.forgehax.util.text;

public class CircledChatHelper {
    public static char toCircled(char c) {
        if(c == 0x0030) c = 0x24EA; //Zero
        else if(c >= 0x0031 && c <= 0x0039) c+= 0x242F; //Numbers 1-9
        else if(c >= 0x0041 && c <= 0x005A) c += 0x2475; //Letters A-Z
        else if(c >= 0x0061 && c <= 0x007A) c += 0x246F; //Letters a-z
        else c = 0;
        return c;
    }

    public static String toCircled(String s) {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray()) {
            if(toCircled(c) != 0)
                sb.append(toCircled(c));
            else sb.append(c);
        }
        return sb.toString();
    }

    public static String revertCircled(char c) {
        StringBuilder sb = new StringBuilder();
        if(c >= 0x2460 && c <= 0x2468) c -= 0x242F; //Numbers 1-9
        else if(c == 0x24EA || c == 0x24FF) c = 0x0030; //Circled 0, both transparent and black
        else if(c >= 0x24B6 && c <= 0x24CF) c -= 0x2475; //Letters A-Z
        else if(c >= 0x24D0 && c <= 0x24E9) c -= 0x246F; //Letters a-z
        else if(c >= 0x24F5 && c <= 0x24FD) c -= 0x24C4; //Double circled 1-9
        /* Being actually composed of two digits, these must be converted separately from the others */
        else if(c >= 0x2469 && c <= 0x2472) { sb.append("1"); c -= 0x2439; } //Circled 10-19
        else if(c == 0x2473) sb.append("20"); //Circled 20
        else if(c >= 0x24EB && c <= 0x24F3) { sb.append("1"); c -= 0x24BA; } //Black circle 11-19
        else if(c == 0x24F4) sb.append("20"); //Black circle 20
        else if(c == 0x24FE) sb.append("10"); //Double circle 10
        else return null;
        sb.append(c);
        return sb.toString();
    }

    public static String revertCircled(String s) {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray()) {
            if(revertCircled(c) != null)
                sb.append(revertCircled(c));
            else sb.append(c);
        }
        return sb.toString();
    }
}