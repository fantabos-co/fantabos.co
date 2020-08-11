package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getRidingOrPlayer;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.Switch.Handle;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.LocalPlayerUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class NoclipMod extends ToggleMod {

  public final Setting<Boolean> flight =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("flight")
      .description("Also make you fly when enabling noClip")
      .defaultTo(false)
      .build();
  
  public NoclipMod() {
    super(Category.MOVEMENT, "NoClip", false, "Enables player noclip");
  }

  private final Handle flying = LocalPlayerUtils.getFlySwitch().createHandle(getModName());
  
  @Override
  public void onDisabled() {
    Entity local = getRidingOrPlayer();
    if (local != null) {
      local.noClip = false;
    }
    if (flight.get()) {
      flying.disable();
    }
  }
  
  @SubscribeEvent
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent event) {
    Entity local = getRidingOrPlayer();
    if (local == null) {
      return;
    }

    local.noClip = true;
    local.onGround = false;
    local.fallDistance = 0;

    if (flight.get()) {
      flying.enable();
    }
  }
}
