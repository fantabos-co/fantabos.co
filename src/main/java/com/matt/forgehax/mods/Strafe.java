package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.asm.events.EntityBlockSlipApplyEvent;
import com.matt.forgehax.asm.events.EntityGroundCheckEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class Strafe extends ToggleMod {

  private final Setting<Float> slipping =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("slipperiness")
      .description("Amount of slipperiness to apply, lower values cause \"ice speed\"")
      .min(0f)
      .max(1f)
      .defaultTo(0.6F)
      .build();

  private final Setting<Boolean> stop_when_flying =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("stop-in-flight")
      .description("Don't set player speed when flying")
      .defaultTo(true)
      .build();
  
  public Strafe() {
    super(Category.MOVEMENT, "Strafe", false, "Allows better player control in air");
  }

  @SubscribeEvent(priority = EventPriority.LOW)
  public void onBlockSlip(EntityBlockSlipApplyEvent event) {
    if (getLocalPlayer() != null
        && getLocalPlayer().equals(event.getEntityLivingBase())
        && (!stop_when_flying.get() ||
            !(getLocalPlayer().isElytraFlying() || getLocalPlayer().capabilities.isFlying))) {
      event.setSlipperiness(slipping.get());
    }
  }

  @SubscribeEvent
  public void onGroundCheck(EntityGroundCheckEvent event) {
    if (getLocalPlayer() != null
      && getLocalPlayer().equals(event.getEntityLivingBase())
      && (!stop_when_flying.get() ||
            !(getLocalPlayer().isElytraFlying() || getLocalPlayer().capabilities.isFlying))) {
      event.setCanceled(true);
    }
  }
}
