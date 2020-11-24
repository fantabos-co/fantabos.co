package com.matt.forgehax.util.text;

import java.util.concurrent.ThreadLocalRandom;

public class CapitalizationHelper {
    private static boolean waveCharIsUpper(double x, double span, double xoff, double yoff) {
        // defaults: span = 0.4, xoff=0, yoff=-0.5
        return Math.sin(x * span + xoff) + yoff > 0;
    }

    public static String makeWave(String message) {
        char[] messageArray = message.toCharArray();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        double span = rand.nextDouble(0.4D, 1.3D);
        double xoff = rand.nextDouble(0, 32);
        double yoff = rand.nextDouble(-0.4, 0.6);

        for (int i = 0; i < messageArray.length; i++)
            if (waveCharIsUpper(i, span, xoff, yoff))
                messageArray[i] = Character.toUpperCase(messageArray[i]);
        return new String(messageArray);
    }

    public static String randomCase(String message) {
        char[] messageArray = message.toCharArray();
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        for (int i = 0; i < messageArray.length; i++) {
            if (rand.nextBoolean())
                messageArray[i] = Character.toUpperCase(messageArray[i]);
            else
                messageArray[i] = Character.toLowerCase(messageArray[i]);
        }
        return new String(messageArray);
    }
}
