package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.key.Bindings;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class AutoLadder extends ToggleMod {

  private final Setting<Double> speed_up =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("up-speed")
      .description("Speed at which you go up")
      .min(0.D)
      .max(1.D)
      .defaultTo(0.1D)
      .build();

  private final Setting<Double> speed_down =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("down-speed")
      .description("Speed at which you go down")
      .min(0.D)
      .max(1.D)
      .defaultTo(0.1D)
      .build();
  
  public AutoLadder() {
    super(Category.MOVEMENT, "AutoLadder", false, "Automatically climbs ladders");
  }
  
  @SubscribeEvent
  public void onUpdate(LocalPlayerUpdateEvent event) {
    if (getLocalPlayer().isOnLadder()) {
      if (Bindings.jump.isPressed())
        getLocalPlayer().motionY = speed_up.get();
      else if (Bindings.sprint.isPressed()) // sneak locks you onto the ladder
        getLocalPlayer().motionY = -speed_down.get();
      else getLocalPlayer().motionY = 0;
    }
  }
}
