package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.events.ForgeHaxEvent;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

@RegisterMod
public class BaritoneCompatibility extends ToggleMod {

  private final KeyBinding bindMacro = new KeyBinding("Baritone Macro", Keyboard.KEY_K, "ForgeHax");

  private final Setting<Boolean> auto_enable =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("start")
          .description("Automatically enable when out of queue")
          .defaultTo(false)
          .build();

  private final Setting<Boolean> auto_stop =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("stop")
          .description("Stop baritone when mod is disabled")
          .defaultTo(true)
          .build();
  
  private final Setting<String> on_string =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("on-string")
          .description("Message to enable baritone")
          .defaultTo("#mine diamond_ore")
          .build();
  
  private final Setting<String> off_string =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("off-string")
          .description("Message to disable baritone")
          .defaultTo("#stop")
          .build();

  private final Setting<String> macro_string =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("macro-string")
          .description("Command to execute on keybind press")
          .defaultTo("#pause")
          .build();
  
  public BaritoneCompatibility() {
    super(Category.MISC, "BaritoneCompatibility", false, "the lazy compatibility mod");
    ClientRegistry.registerKeyBinding(this.bindMacro);
  }
  
  private boolean off = false;
  private boolean once = false;
  
  private void turnOn() {
    off = false;
    getLocalPlayer().sendChatMessage(on_string.get());
  }
  
  private void turnOff() {
    off = true;
    getLocalPlayer().sendChatMessage(off_string.get());
  }
  
  @Override
  protected void onDisabled() {
    off = once = false;
    if (auto_stop.get())
      turnOff();
  }
  
  @SubscribeEvent
  public void onWorldUnload(WorldEvent.Unload event) {
    onDisabled();
  }

  @SubscribeEvent
  public void onKeyPress(InputEvent.KeyInputEvent event) {
    if (bindMacro.isPressed() && getLocalPlayer() != null) {
      getLocalPlayer().sendChatMessage(macro_string.get());
    }
  }
  
  @SubscribeEvent
  public void onTick(LocalPlayerUpdateEvent event) {
    if (auto_enable.get() && !once) {
      once = true;
      BlockPos pos = getLocalPlayer().getPosition();
      if (pos.getX() != 0 && pos.getZ() != 0) {
        turnOn();
      }
    }
  }
  
  @SubscribeEvent
  public void onEvent(ForgeHaxEvent event) {
    if (getLocalPlayer() == null) {
      return;
    }
    
    switch (event.getType()) {
      case EATING_START:
      case EATING_SELECT_FOOD: {
        if (!off) {
          turnOff();
        }
        break;
      }
      case EATING_STOP: {
        turnOn();
        break;
      }
    }
  }
}
