package com.matt.forgehax.util.command;

import com.google.gson.JsonObject;
import com.matt.forgehax.Helper;
import com.matt.forgehax.util.command.exception.CommandExecuteException;

import java.nio.file.Path;

import javax.annotation.Nonnull;

import joptsimple.internal.Strings;

/**
 * Created on 6/2/2017 by fr1kin
 */
public class CommandGlobal extends CommandStub {
  
  private static final CommandGlobal INSTANCE = new CommandGlobal();
  private static String active_loadout = "default";
  
  public static CommandGlobal getInstance() {
    return INSTANCE;
  }
  
  private CommandGlobal() {
    super(
        CommandBuilders.getInstance()
            .newStubBuilder()
            .name(Strings.EMPTY)
            .helpOption(false)
            .getData());
  }

  public String getLoadout() { return active_loadout; }

  private void switch_loadout(String name) {
    active_loadout = name;
    Helper.printLog("Switched to loadout \"" + active_loadout + "\"");
  }

  public void saveConfiguration(String name) {
    switch_loadout(name);
    saveConfiguration();
  }

  public void saveConfiguration() {
    JsonObject newConfig = new JsonObject();
    this.serialize(newConfig);
    try {
      Helper.getFileManager().saveConfigObject(active_loadout + ".json", newConfig);
      Helper.printLog("Saved config \"" + active_loadout + "\"");
    } catch (Exception e) {
      e.printStackTrace(); // ignore
    }
  }

  public void loadConfiguration(String name) {
    switch_loadout(name);
    loadConfiguration();
  }

  public void loadConfiguration() {
    try {
      JsonObject config = Helper.getFileManager().getConfigObject(active_loadout + ".json");
      if (config != null) {
        this.reset_defaults(); // default values are not stored so always revert to defaults before loading
        this.deserialize(config);
        Helper.printLog("Loaded config \"" + active_loadout + "\"");
      }
    } catch (Exception e) {
      e.printStackTrace(); // ignore
    }
  }

  @Override
  public void serialize(JsonObject in) {
    getChildren().forEach(c -> c.serialize(in));
  }
  
  @Override
  public void deserialize(JsonObject in) {
    getChildren().forEach(c -> c.deserialize(in));
  }
  
  @Override
  public boolean isGlobal() {
    return true;
  }
  
  @Override
  public String getName() {
    return Strings.EMPTY;
  }
  
  @Override
  public String getAbsoluteName() {
    return Strings.EMPTY;
  }
  
  @Override
  public void run(@Nonnull String[] args) throws CommandExecuteException, NullPointerException {
    if (!processChildren(args)) {
      if (args.length > 0) {
        throw new CommandExecuteException(String.format("Unknown command \"%s\"", args[0]));
      } else {
        throw new CommandExecuteException("Missing argument(s)");
      }
    }
  }
}
