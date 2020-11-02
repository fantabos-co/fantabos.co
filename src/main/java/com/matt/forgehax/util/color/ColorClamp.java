package com.matt.forgehax.util.color;

import net.minecraft.util.text.TextFormatting;

public class ColorClamp implements Colors {
  
  private static TextFormatting[] available = {
    TextFormatting.BLACK,
    TextFormatting.DARK_BLUE,
    TextFormatting.DARK_GREEN,
    TextFormatting.DARK_AQUA,
    TextFormatting.DARK_RED,
    TextFormatting.DARK_PURPLE,
    TextFormatting.GOLD, 
    TextFormatting.GRAY, 
    TextFormatting.DARK_GRAY,
    TextFormatting.BLUE, 
    TextFormatting.GREEN,
    TextFormatting.AQUA, 
    TextFormatting.RED,
    TextFormatting.LIGHT_PURPLE,
    TextFormatting.YELLOW,
    TextFormatting.WHITE,
  };

  private static Color[] equivalent = {
    Color.of(0, 0, 0), // BLACK
    Color.of(0, 0, 170), // DARK_BLUE
    Color.of(0, 170, 0), // DARK_GREEN
    Color.of(0, 170, 170), // DARK_AQUA
    Color.of(170, 0, 0), // DARK_RED
    Color.of(170, 0, 170), // DARK_PURPLE
    Color.of(255, 170, 0), // GOLD
    Color.of(170, 170, 170), // GRAY
    Color.of(85, 85, 85), // DARK_GRAY
    Color.of(85, 85, 255), // BLUE
    Color.of(85, 255, 85), // GREEN
    Color.of(85, 255, 255), // AQUA
    Color.of(255, 85, 85), // RED
    Color.of(255, 85, 255), // LIGHT_PURPLE
    Color.of(255, 255, 85), // YELLOW
    Color.of(255, 255, 255) // WHITE
  };
  
  public static TextFormatting getClampedColor(int color) {
    int min = 99999;
    int index = 15;
    int buf;

    Color c = Color.of(color);

    for (int i = 0; i < 16; i++) {
      buf = Math.abs(c.getRed() - equivalent[i].getRed()) +
            Math.abs(c.getGreen() - equivalent[i].getGreen()) +
            Math.abs(c.getBlue() - equivalent[i].getBlue());
      if (buf < min) {
        min = buf;
        index = i;
      }
    }
    
    return available[index];
  }
}