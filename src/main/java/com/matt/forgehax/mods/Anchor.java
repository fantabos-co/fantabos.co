package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.asm.events.LocalPlayerUpdateMovementEvent;
import com.matt.forgehax.mods.services.HoleService;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class Anchor extends ToggleMod {

  private final Setting<Double> range =
  getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("range")
      .description("Distance from the center to trigger at")
      .defaultTo(0.2D)
      .build();
      
  public Anchor() {
    super(Category.MOVEMENT, "Anchor", false, "Make you better fall in holes");
  }
  
  @SubscribeEvent
  public void onLocalPlayerUpdateMovement(LocalPlayerUpdateMovementEvent event) {
    if (getLocalPlayer() == null) return;
    if (getLocalPlayer().capabilities.isFlying || getLocalPlayer().capabilities.isCreativeMode) return;
    for (BlockPos pos : HoleService.holes) {
      if (HoleService.isAboveHole(pos, getLocalPlayer(), range.get())) {
        if (!MC.gameSettings.keyBindJump.isKeyDown()) {
          if (!HoleService.isAboveHole(pos, getLocalPlayer())) { // hitbox won't fit
            BlockPos round = new BlockPos(getLocalPlayer().getPositionVector());
            getLocalPlayer().motionX = ((round.getX() + 0.5D) - getLocalPlayer().posX) / 5;
            getLocalPlayer().motionZ = ((round.getZ() + 0.5D) - getLocalPlayer().posZ) / 5;
          } else {
            getLocalPlayer().motionX = 0;
            getLocalPlayer().motionZ = 0;
          }
        }
      }
    }
  }
}