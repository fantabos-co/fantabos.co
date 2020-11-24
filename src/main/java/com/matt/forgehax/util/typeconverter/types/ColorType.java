package com.matt.forgehax.util.typeconverter.types;

import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.typeconverter.TypeConverter;
import java.util.Comparator;
import javax.annotation.Nullable;

public class ColorType extends TypeConverter<Color> {
  
  @Override
  public String label() {
    return "color";
  }
  
  @Override
  public Class<Color> type() {
    return Color.class;
  }
  
  @Override
  public Color parse(String value) {
    return Color.of(Integer.parseInt(value));
  }

  @Override
  public String toStringPretty(Color value) {
    return String.format("RGBA(%d, %d, %d, %d)", value.getRed(),
                    value.getGreen(), value.getBlue(), value.getAlpha());
  }
  
  @Override
  public String toString(Color value) {
    return value != null ? String.format("%d", value.toBuffer()) : "0";
  }

  @Override
  public String toStringSafe(Color value) {
    return value != null ? String.format("%d", value.toBuffer()) : "0";
  }
  

  @Nullable
  @Override
  public Comparator<Color> comparator() {
    return new Comparator<Color>() {
      @Override
      public int compare(Color o1, Color o2) {
        return o1.toBuffer() - o2.toBuffer();
      }
    };
  }
}
