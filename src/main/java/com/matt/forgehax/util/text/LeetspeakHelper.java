package com.matt.forgehax.util.text;

import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Moved Babbaj's 1337speak stuff here */

public class LeetspeakHelper {
    // Uppercase Lookup for LEET
    private static HashMap<Integer, String> LeetMap = new HashMap<>();
    // Custom probability for rarely used LEET replacements
    private static HashMap<Integer, Integer> LeetProbability = new HashMap<>();

    static {
        LeetMap.put(65, "4");
        LeetMap.put(69, "3");
        LeetMap.put(73, "1");
        LeetProbability.put(73, 60);
        LeetMap.put(76, "1");
        LeetMap.put(79, "0");
        LeetMap.put(83, "5");
        LeetMap.put(84, "7");
        LeetMap.put(77, "|\\/|");
        LeetProbability.put(77, 15);
        LeetMap.put(78, "|\\|");
        LeetProbability.put(78, 20);
        LeetMap.put(66, "8");
        LeetProbability.put(66, 20);
        LeetMap.put(67, "k");
        LeetProbability.put(67, 80);
        LeetMap.put(68, "|)");
        LeetProbability.put(68, 40);
        LeetMap.put(71, "9");
        LeetProbability.put(71, 20);
        LeetMap.put(72, "|-|");
        LeetProbability.put(72, 40);
        LeetMap.put(75, "|<");
        LeetProbability.put(75, 40);
        LeetMap.put(80, "|2");
        LeetProbability.put(80, 20);
        LeetMap.put(85, "|_|");
        LeetProbability.put(85, 20);
        LeetMap.put(86, "\\/");
        LeetProbability.put(86, 40);
        LeetMap.put(87, "\\/\\/");
        LeetProbability.put(87, 30);
        LeetMap.put(88, "><");
        LeetProbability.put(88, 50);
    }

    public static String toLeetspeak(String message) {
        char[] messageArray;

        message = message.replaceAll("(?i)dude", "d00d").replaceAll("(^|\\s)ph", "$1f");

        messageArray = message.toCharArray();
        // match and replace the last only S in a word
        Matcher zMatcher = Pattern.compile("(?<![sS])([sS])(?:[^\\w]|$)").matcher(message);

        while (!zMatcher.hitEnd()) {
            if (zMatcher.find()) {
                if (zMatcher.group(1).equals("s")) {
                    messageArray[zMatcher.end(1) - 1] = 'z';
                } else {
                    messageArray[zMatcher.end(1) - 1] = 'Z';
                }
            }
        }

        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        for (char c : messageArray) {
            int key = Character.toUpperCase(c);
            // half the probability for LEET
            if (random.nextInt(2) == 0
                    && LeetMap.get(key) != null
                    && (LeetProbability.get(key) == null
                    || LeetProbability.get(key) > random.nextInt(100))) {
                builder.append(LeetMap.get(key));
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    public HashMap<Integer, String> getLeetMap() {
        return LeetMap;
    }

    public HashMap<Integer, Integer> getLeetProbability() {
        return LeetProbability;
    }
}