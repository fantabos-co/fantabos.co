package com.matt.forgehax.util.command;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.matt.forgehax.util.command.callbacks.CallbackData;
import com.matt.forgehax.util.command.exception.CommandBuildException;
import com.matt.forgehax.util.command.exception.CommandExecuteException;
import com.matt.forgehax.util.key.IKeyBind;
import com.matt.forgehax.util.serialization.ISerializableJson;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * Created on 6/8/2017 by fr1kin
 */
public class CommandStub extends Command implements IKeyBind {
  
  public static final String KEYBIND = "Command.keybind";
  public static final String KEYBIND_OPTIONS = "Command.keybind_options";
  
  private final KeyBinding bind;
  
  protected CommandStub(Map<String, Object> data) throws CommandBuildException {
    super(data);
    
    // key binding
    Integer keyCode = (Integer) data.getOrDefault(KEYBIND, -1);
    if (keyCode != -1) {
      bind = new KeyBinding(getAbsoluteName(), keyCode, "ForgeHax");
      ClientRegistry.registerKeyBinding(bind);
      
      Boolean genOptions = (Boolean) data.getOrDefault(KEYBIND_OPTIONS, true);
      if (genOptions) {
        parser.accepts("bind", "Bind to the given key").withRequiredArg();
        parser.accepts("unbind", "Sets bind to KEY_NONE");
        
        this.processors.add(
            dt -> {
              if (dt.hasOption("bind")) {
                String key = dt.getOptionAsString("bind").toUpperCase();
                
                int kc = Keyboard.getKeyIndex(key);
                if (Keyboard.getKeyIndex(key) == Keyboard.KEY_NONE) {
                  throw new CommandExecuteException(
                      String.format("\"%s\" is not a valid key name", key));
                }
                
                bind(kc);
                // serialize();
                
                dt.write(String.format("Bound %s to key %s [code=%d]", getAbsoluteName(), key, kc));
                dt.stopProcessing();
              } else if (dt.hasOption("unbind")) {
                unbind();
                // serialize();
                
                dt.write(String.format("Unbound %s", getAbsoluteName()));
                dt.stopProcessing();
              }
            });
        this.processors.add(
            dt -> {
              if (!dt.options().hasOptions() && dt.getArgumentCount() > 0) {
                dt.write(
                    String.format(
                        "Unknown command \"%s\"", Strings.nullToEmpty(dt.getArgumentAsString(0))));
              }
            });
      }
    } else {
      bind = null;
    }
  }

  @Override
  public void reset_defaults() {
    getChildren().forEach(c -> c.reset_defaults());
    if (bind != null)
      unbind();
  }

  @Override
  public void serialize(JsonObject in) {
    JsonObject add = new JsonObject();
    getChildren().forEach(c -> c.serialize(add));
    if (bind != null && bind.getKeyCode() > 0) add.addProperty("bind", bind.getKeyCode());
    if (!add.entrySet().isEmpty())
      in.add(getName(), add);
  }
  
  @Override
  public void deserialize(JsonObject in) {
    JsonObject from = in.getAsJsonObject(getName());
    if (from == null) {
      unbind();
      return;
    }
    getChildren().forEach(c -> c.deserialize(from));
    if (from.get("bind") != null && from.get("bind").getAsInt() > 0)
      bind(from.get("bind").getAsInt());
    else unbind();
  }
  
  @Override
  public void bind(int keyCode) {
    if (bind != null) {
      bind.setKeyCode(keyCode);
      KeyBinding.resetKeyBindingArrayAndHash();
    }
  }
  
  @Nullable
  public KeyBinding getBind() {
    return bind;
  }
  
  @Override
  public void onKeyPressed() {
    invokeCallbacks(CallbackType.KEY_PRESSED, new CallbackData(this));
  }
  
  @Override
  public void onKeyDown() {
    invokeCallbacks(CallbackType.KEY_DOWN, new CallbackData(this));
  }
}
