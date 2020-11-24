package com.matt.forgehax.util.command;

import com.google.gson.JsonObject;
import com.matt.forgehax.util.SafeConverter;
import com.matt.forgehax.util.command.callbacks.OnChangeCallback;
import com.matt.forgehax.util.command.exception.CommandBuildException;
import com.matt.forgehax.util.console.ConsoleIO;
import com.matt.forgehax.util.serialization.ISerializableJson;
import com.matt.forgehax.util.typeconverter.TypeConverter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created on 6/2/2017 by fr1kin
 */
public class Setting<E> extends Command implements ISerializableJson {

  public static final String DEFAULTVALUE = "Setting.defaultValue";
  public static final String CONVERTER = "Setting.converter";
  public static final String COMPARATOR = "Setting.comparator";
  public static final String MINVALUE = "Setting.minvalue";
  public static final String MAXVALUE = "Setting.maxvalue";
  public static final String RESETAUTOGEN = "Setting.resetAutoGen";
  public static final String DEFAULTPROCESSOR = "Setting.defaultProcessor";

  private final E defaultValue;
  private final TypeConverter<E> converter;
  private final Comparator<E> comparator;
  private final E minValue;
  private final E maxValue;

  private E value;

  @SuppressWarnings("unchecked")
  protected Setting(Map<String, Object> data) throws CommandBuildException {
    super(data);
    try {
      this.converter = (TypeConverter<E>) data.get(CONVERTER);
      Objects.requireNonNull(this.converter, "Setting requires converter");

      this.defaultValue = (E) data.get(DEFAULTVALUE);
      this.comparator = (Comparator<E>) data.get(COMPARATOR);
      this.minValue = (E) data.get(MINVALUE);
      this.maxValue = (E) data.get(MAXVALUE);

      Boolean defaultProcessor = (Boolean) data.getOrDefault(DEFAULTPROCESSOR, true);
      if (defaultProcessor) {
        parser.acceptsAll(Arrays.asList("force", "f"), "Set a value ignoring min/max");

        processors.add(
            in -> {
              if (in.getArgumentCount() == 0) {
                in.write(getPrintText());
                return;
              }
              
              Object arg = in.getArgument(0);
              boolean force = in.hasOption("force");
              if (arg != null) {
                if (force) rawForceSet(String.valueOf(arg));
                else rawSet(String.valueOf(arg));
                // serialize();
                in.markSuccess();
              } else {
                in.markFailed();
              }
            });
      }

      Boolean resetAutoGen = (Boolean) data.getOrDefault(RESETAUTOGEN, true);
      if (resetAutoGen) {
        parser.acceptsAll(Arrays.asList("reset"), "Sets the command to its default value");
      }

      // set with constraints
      //set(defaultValue, false);
      this.value = defaultValue;
    } catch (Throwable t) {
      throw new CommandBuildException("Failed to build setting", t);
    }
  }

  public E get() {
    return value;
  }

  public E getMin() {
    return minValue;
  }

  public E getMax() {
    return maxValue;
  }

  public E getDefault() {
    return defaultValue;
  }

  @Nonnull
  public Class<?> getType() {
    return converter.type();
  }

  public boolean getAsBoolean() {
    return SafeConverter.toBoolean(get());
  }

  public byte getAsByte() {
    return SafeConverter.toByte(get());
  }

  public char getAsCharacter() {
    return SafeConverter.toCharacter(get());
  }

  public double getAsDouble() {
    return SafeConverter.toDouble(get());
  }

  public float getAsFloat() {
    return SafeConverter.toFloat(get());
  }

  public int getAsInteger() {
    return SafeConverter.toInteger(get());
  }

  public long getAsLong() {
    return SafeConverter.toLong(get());
  }

  public short getAsShort() {
    return SafeConverter.toShort(get());
  }

  public String getAsString() {
    return converter.toString(get());
  }

  public E cap(E value) {
    if (comparator != null && value != null && this.value != null) {
      // clamp value to minimum and maximum value
      if (minValue != null && comparator.compare(value, minValue) < 0) {
        value = minValue;
      } else if (maxValue != null && comparator.compare(value, maxValue) > 0) {
        value = maxValue;
      }
    }
    return value;
  }

  public boolean set(E value, final boolean commandOutput) {
    value = cap(value);

    boolean was_set = set_and_invoke(value);

    if (was_set && commandOutput) {
      String logMsg = String.format("%s = %s", getAbsoluteName(), converter.toStringPretty(value));
      ConsoleIO.write(logMsg); // Print for every other setting
    }
    return was_set;
  }

  public boolean set(E value) {
    return set(value, true);
  }

  public boolean force_set(E value) {
    // Force set should always be logged
    boolean was_set = set_and_invoke(value);

    if (was_set) {
      String logMsg = String.format("%s := %s", getAbsoluteName(), converter.toStringPretty(value));
      ConsoleIO.write(logMsg); // Print for every other setting
    }

    return was_set;
  }

  private boolean set_and_invoke(E value) {
    if (!Objects.equals(get(), value)) {
      OnChangeCallback<E> cb = new OnChangeCallback<>(this, get(), value);
      invokeCallbacks(CallbackType.CHANGE, cb);
      if (cb.isCanceled())
        return false;
      this.value = value;
      return true;
    }
    return false;
  }

  public boolean rawSet(String value, boolean output) {
    return set(converter.parseSafe(value), output);
  }

  public boolean rawSet(String value) {
    return rawSet(value, true);
  }

  public boolean rawForceSet(String value) {
    return force_set(converter.parseSafe(value));
  }

  public boolean reset(boolean commandOutput) {
    return set(defaultValue, commandOutput);
  }

  public TypeConverter<E> getConverter() {
    return converter;
  }

  @Override
  public String getPrintText() {
    return getName() + " = " + converter.toStringPretty(get()) + " - " + getDescription();
  }

  @Override
  public boolean addChild(@Nonnull Command child) {
    throw new UnsupportedOperationException(
        "Command::addChild is not supported for a Setting type");
  }

  @Override
  public boolean removeChild(@Nonnull Command child) {
    return false;
  }
  
  @Nullable
  @Override
  public Command getChild(String name) {
    return null;
  }
  
  @Override
  public Collection<Command> getChildren() {
    return Collections.emptySet();
  }
  
  @Override
  public void getChildrenDeep(Collection<Command> all) {
  }
  
  @Override
  public Collection<Command> getChildrenDeep() {
    return Collections.emptySet();
  }

  @Override
  protected boolean preprocessor(String[] args) {
    if (args.length > 0) {
      String opt = args[0];
      if (opt.matches("--reset")) {
        reset(true);
        // serialize();
        return false;
      }
    }
    return true;
  }

  @Override
  public void reset_defaults() {
    this.set(this.getDefault(), false);
  }

  @Override
  public void serialize(JsonObject in) {
    // Settings can't have children, so just serialize self
    if (!get().equals(getDefault()))
      in.addProperty(getName(), getAsString());
  }

  @Override
  public void deserialize(JsonObject in) {
    // Settings can't have children, so just deserialize self
    if (in.get(getName()) != null) {
      String from = in.get(getName()).getAsString();
      if (from != null) rawSet(from, false);
    }
  }
}
