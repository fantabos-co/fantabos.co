package com.matt.forgehax.util.command;

import com.matt.forgehax.util.SafeConverter;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.ColorClamp;
import com.matt.forgehax.util.command.callbacks.OnChangeCallback;
import com.matt.forgehax.util.command.options.OptionBuilders;
import com.matt.forgehax.util.typeconverter.TypeConverter;
import com.matt.forgehax.util.typeconverter.TypeConverterRegistry;
import com.matt.forgehax.util.typeconverter.types.ColorType;

import net.minecraft.util.text.TextFormatting;

import java.util.Comparator;
import java.util.function.Consumer;

public class SettingColorBuilder
    extends BaseCommandBuilder<SettingColorBuilder, Setting<Color>> {

  public SettingColorBuilder changed(Consumer<OnChangeCallback<Color>> consumer) {
    getCallbacks(CallbackType.CHANGE).add(consumer);
    return this;
  }
  
  public SettingColorBuilder defaultTo(Color defaultValue) {
    return insert(Setting.DEFAULTVALUE, defaultValue).convertFrom(defaultValue.getClass());
  }
  
  private SettingColorBuilder converter(TypeConverter<Color> converter) {
    return insert(Setting.CONVERTER, converter).comparator(converter.comparator());
  }
  
  private SettingColorBuilder comparator(Comparator<Color> comparator) {
    return insert(Setting.COMPARATOR, comparator);
  }
  
  // private SettingColorBuilder min(Color minValue) {
  //   return insert(Setting.MINVALUE, minValue);
  // }
  // 
  // private SettingColorBuilder max(Color maxValue) {
  //   return insert(Setting.MAXVALUE, maxValue);
  // }
  
  private SettingColorBuilder convertFrom(Class<? extends Color> clazz) {
    TypeConverter<Color> converter = TypeConverterRegistry.get(clazz);
    if (converter == null) {
      converter = new ColorType() ;
    }
    
    return converter(converter).comparator(converter.comparator());
  }
  
  @Override
  public Setting<Color> build() {
    data.put(Setting.DEFAULTPROCESSOR, false);
    options(OptionBuilders::rgba);
    // processor(OptionProcessors::rgba); // this resets all channels not specified
    Setting<Color> out = new Setting<Color>(has(Command.REQUIREDARGS) ? data : requiredArgs(0).data);
    out.processors.add(e -> { // can probably be made fancier
      if (e.getArgumentCount() == 0 && !e.options().hasOptions()) {
        e.write(out.getPrintText());
        return;
      }

      if (e.getArgumentCount() > 0 && TextFormatting.getValueByName(e.getArgumentAsString(0)) != null) {
        out.set(
          ColorClamp.getFromMinecraftColor(
            TextFormatting.getValueByName(e.getArgumentAsString(0)))); // this is kinda lazy and can be done better
        e.markSuccess();
      }
      if (e.hasOption("red") || e.hasOption("green") ||
          e.hasOption("blue") || e.hasOption("alpha")) {
        Color prev = out.get();
        int r = SafeConverter.toInteger(e.getOption("red"), prev.getRed());
        int g = SafeConverter.toInteger(e.getOption("green"), prev.getGreen());
        int b = SafeConverter.toInteger(e.getOption("blue"), prev.getBlue());
        int a = SafeConverter.toInteger(e.getOption("alpha"), prev.getAlpha());
        out.set(Color.of(r, g, b, a));
        e.markSuccess();
      }
    });
    return out;
  }
}
