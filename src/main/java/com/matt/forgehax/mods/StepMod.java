package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.LocalPlayerUpdateMovementEvent;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class StepMod extends ToggleMod {

  private static final float DEFAULT_STEP_HEIGHT = 0.6f;

  private final Setting<Boolean> doubleStep =
          getCommandStub()
                  .builders()
                  .<Boolean>newSettingBuilder()
                  .name("double-step")
                  .description("step up 2 blocks instead of just 1")
                  .defaultTo(false)
                  .build();

  private final Setting<Boolean> unstep =
          getCommandStub()
                  .builders()
                  .<Boolean>newSettingBuilder()
                  .name("unstep")
                  .description("step down instead of falling")
                  .defaultTo(false)
                  .build();

  public StepMod() {
    super(Category.MOVEMENT, "Step", false, "Step up blocks");
  }

  @SubscribeEvent
  public void onLocalPlayerMovementEvent(LocalPlayerUpdateEvent event) {

    if (!MC.player.collidedHorizontally) return;
    if (!MC.player.onGround || MC.player.isOnLadder() || MC.player.isInWater() || MC.player.isInLava() || MC.player.movementInput.jump || MC.player.noClip) return;
    if (MC.player.moveForward == 0 && MC.player.moveStrafing == 0) return;

    final double n = get_n_normal();

      if (n < 0 || n > 2) return;

    if (n == 2.0 && doubleStep.get()) {
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 0.42, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 0.78, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 0.63, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 0.51, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 0.9, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 1.21, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 1.45, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 1.43, MC.player.posZ, MC.player.onGround));
      MC.player.setPosition(MC.player.posX, MC.player.posY + 2.0, MC.player.posZ);
    }
    if (n == 1.5) {
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 0.41999998688698, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 0.7531999805212, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 1.00133597911214, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 1.16610926093821, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 1.24918707874468, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 1.1707870772188, MC.player.posZ, MC.player.onGround));
      MC.player.setPosition(MC.player.posX, MC.player.posY + 1.5, MC.player.posZ);
    }
    if (n == 1.0) {
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 0.41999998688698, MC.player.posZ, MC.player.onGround));
      MC.player.connection.sendPacket(new CPacketPlayer.Position(MC.player.posX, MC.player.posY + 0.7531999805212, MC.player.posZ, MC.player.onGround));
      MC.player.setPosition(MC.player.posX, MC.player.posY + 1.0, MC.player.posZ);
    }


  }

 @SubscribeEvent
  public void onLocalPlayerMovementEvent2(LocalPlayerUpdateMovementEvent event) {
    if (!MC.player.onGround || MC.player.isOnLadder() || MC.player.isInWater() || MC.player.isInLava() || MC.player.movementInput.jump || MC.player.noClip) return;
    if (MC.player.moveForward == 0 && MC.player.moveStrafing == 0) return;

    if(unstep.get()) {
      MC.player.motionY = -1;
    }
  }

  public double get_n_normal() {

    MC.player.stepHeight = 0.5f;

    double max_y = -1;

    final AxisAlignedBB grow = MC.player.getEntityBoundingBox().offset(0, 0.05, 0).grow(0.05);

    if (!MC.world.getCollisionBoxes(MC.player, grow.offset(0, 2, 0)).isEmpty()) return 100;

    for (final AxisAlignedBB aabb : MC.world.getCollisionBoxes(MC.player, grow)) {

      if (aabb.maxY > max_y) {
        max_y = aabb.maxY;
      }

    }

    return max_y - MC.player.posY;

  }

}
