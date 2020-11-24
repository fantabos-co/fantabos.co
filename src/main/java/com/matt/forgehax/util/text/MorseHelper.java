package com.matt.forgehax.util.text;

import java.util.HashMap;

/* Moved Tonio's Morse stuff here */

public class MorseHelper {
    private static HashMap<Character, String> MorseMap = new HashMap<>();

    static {
        MorseMap.put('a', ".- ");
        MorseMap.put('b',"-... ");
        MorseMap.put('c',"-.-. ");
        MorseMap.put('d',"-.. ");
        MorseMap.put('e',". ");
        MorseMap.put('f',"..-. ");
        MorseMap.put('g',"--. ");
        MorseMap.put('h',".... ");
        MorseMap.put('i',".. ");
        MorseMap.put('j',".--- ");
        MorseMap.put('k',"-.- ");
        MorseMap.put('l',".-.. ");
        MorseMap.put('m',"-- ");
        MorseMap.put('n',"-. ");
        MorseMap.put('o',"--- ");
        MorseMap.put('p',".--. ");
        MorseMap.put('q',"--.- ");
        MorseMap.put('r',".-. ");
        MorseMap.put('s',"... ");
        MorseMap.put('t',"- ");
        MorseMap.put('u',"..- ");
        MorseMap.put('v',"...- ");
        MorseMap.put('w',".-- ");
        MorseMap.put('x',"-..- ");
        MorseMap.put('y',"-.-- ");
        MorseMap.put('z',"--.. ");
        MorseMap.put('1',".---- ");
        MorseMap.put('2',"..--- ");
        MorseMap.put('3',"...-- ");
        MorseMap.put('4',"....- ");
        MorseMap.put('5',"..... ");
        MorseMap.put('6',"-.... ");
        MorseMap.put('7',"--... ");
        MorseMap.put('8',"---.. ");
        MorseMap.put('9',"----. ");
        MorseMap.put('0',"----- ");
        MorseMap.put(' '," | ");
    }

    public static String toMorse(String message) {
        StringBuilder out = new StringBuilder();
        for (char c : message.toLowerCase().toCharArray()) {
            if (MorseMap.get(c) == null) continue;
            out.append(MorseMap.get(c));
        }
        return out.toString();
    }

    public HashMap<Character, String> getMorseMap() {
        return MorseMap;
    }
}
