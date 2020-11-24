package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.asm.utils.fasttype.FastField;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.key.Bindings;
import com.matt.forgehax.util.key.KeyBindingHandler;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Created by Babbaj on 1/30/2018.
 */
@RegisterMod
public class AutoKey extends ToggleMod {
  
  public AutoKey() {
    super(Category.PLAYER, "AutoKey", false, "Automatically click/press keys");
  }
  
  private final Setting<Integer> delay =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("delay")
          .description("delay(ms) between clicks")
          .defaultTo(500) // 500 ms
          .min(0)
          .max(600000) // 10 min
          .build();
  
  private static Setting<Integer> holdTime; // static to allow easy access from ClickMode
  
  {
    holdTime =
        getCommandStub()
            .builders()
            .<Integer>newSettingBuilder()
            .name("holdTime")
            .description("how long to hold button for tap")
            .defaultTo(150) // approximate minimum for reliable key pressing
            .min(0)
            .max(1000)
            .build();
  }
  
  // TODO: make serializable and save as json
  private final Map<KeyBindingHandler, ClickMode> activeKeys = new HashMap<>();
  
  private long lastTimeMillis;

  @Override
  protected void onEnabled() {
    lastTimeMillis = System.currentTimeMillis();
  }
  
  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null) return;

    final int lastClick = (int) (System.currentTimeMillis() - lastTimeMillis);
    if (lastClick >= delay.get()) {
      lastTimeMillis = System.currentTimeMillis();
    }
    
    activeKeys.forEach((key, mode) -> mode.apply(key, lastClick));
  }
  
  @Override
  public void onLoad() {
    // add a key
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("add")
        .description("add a key to the active key list - (ex: add [use/jump/attack/...] [tap/hold]")
        .processor(
            data -> {
              data.requiredArguments(2);
              KeyBindingHandler key = Bindings.getKey(data.getArgumentAsString(0));
              if (key == null) {
                Helper.printMessage("Unknown key: %s", data.getArgumentAsString(0));
                return;
              }
              
              String mode = data.getArgumentAsString(1);
              ClickMode clickMode =
                  Arrays.stream(ClickMode.values())
                      .filter(m -> m.toString().toLowerCase().contains(mode.toLowerCase()))
                      .findFirst()
                      .orElseGet(
                          () -> {
                            Helper.printMessage("Unknown mode, defaulting to tap");
                            return ClickMode.TAP;
                          });
              activeKeys.put(key, clickMode);
            })
        .build();
    
    // remove all keys
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("clear")
        .description("clear all the active keys")
        .processor(
            data -> {
              if (data.getArgumentCount() > 0) {
                Helper.printError("Unexpected arguments!");
                return;
              }
              activeKeys.clear();
            })
        .build();
    
    // remove a single key
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("remove")
        .description("remove an active key - (ex: remove \"jump\"")
        .processor(
            data -> {
              data.requiredArguments(1);
              KeyBindingHandler key = Bindings.getKey(data.getArgumentAsString(0));
              ClickMode mode = activeKeys.remove(key);
              if (mode != null) {
                Helper.printMessage("Removed key: %s", key.getBinding().getKeyDescription());
              } else {
                Helper.printError("Unknown key");
              }
            })
        .build();
  }
  
  private static void incrementPressTime(KeyBindingHandler binding) {
    FastField<Integer> field = FastReflection.Fields.Binding_pressTime;
    int currTime = field.get(binding.getBinding());
    field.set(binding.getBinding(), currTime + 1);
  }
  
  private enum ClickMode {
    TAP(
        (key, time) -> {
          if (time < holdTime.getAsInteger()) {
            // incrementPressTime(key);
            key.setPressed(true);
          } else {
            key.setPressed(false);
          }
        }), // hold key for at least 150ms
    
    HOLD(
        (key, time) -> {
          // incrementPressTime(key);
          key.setPressed(true);
        }); // hold key forever
    
    BiConsumer<KeyBindingHandler, Integer> clickAction;
    
    ClickMode(BiConsumer<KeyBindingHandler, Integer> action) {
      this.clickAction = action;
    }
    
    public void apply(KeyBindingHandler key, int lastTime) {
      clickAction.accept(key, lastTime);
    }
  }
}
