package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.LocalPlayerUpdateMovementEvent;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import java.util.HashMap;
import java.util.Objects;

import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.matt.forgehax.Helper.getLocalPlayer;

/*
      Created by Tonio, mostly rewrote by Nathan
 */

@RegisterMod
public class FlyMod extends ToggleMod {

  public final Setting<Float> speed =
          getCommandStub()
                  .builders()
                  .<Float>newSettingBuilder()
                  .name("Speed")
                  .description("Flight speed")
                  .defaultTo(0.14f)
                  .min(0f)
                  .max(0.3f)
                  .build();
  private final Setting<Integer> Factor =
          getCommandStub()
                  .builders()
                  .<Integer>newSettingBuilder()
                  .name("Factor")
                  .description("Factor at which to send packets")
                  .min(1)
                  .max(10)
                  .defaultTo(1)
                  .build();
  private final Setting<Integer> MaxBuffer =
          getCommandStub()
                  .builders()
                  .<Integer>newSettingBuilder()
                  .name("BufferAmount")
                  .description("Max packets to send per buffer")
                  .min(5)
                  .max(100)
                  .defaultTo(50)
                  .build();
  public final Setting<Boolean> SetBack =
          getCommandStub()
                  .builders()
                  .<Boolean>newSettingBuilder()
                  .name("Set-Back")
                  .description("Process position packets normally")
                  .defaultTo(true)
                  .build();
  public final Setting<Boolean> AntiKick =
          getCommandStub()
                  .builders()
                  .<Boolean>newSettingBuilder()
                  .name("Anti-Kick")
                  .description("What do you think it means?")
                  .defaultTo(true)
                  .build();
  public final Setting<Boolean> OnGround =
          getCommandStub()
                  .builders()
                  .<Boolean>newSettingBuilder()
                  .name("OnGroundSpoof")
                  .description("Spoofs your position as on ground.")
                  .defaultTo(true)
                  .build();
  public final Setting<Boolean> Phase =
          getCommandStub()
                  .builders()
                  .<Boolean>newSettingBuilder()
                  .name("Phase")
                  .description("Allows you to phase through blocks")
                  .defaultTo(false)
                  .build();
  public final Setting<Boolean> FastPhase =
          getCommandStub()
                  .builders()
                  .<Boolean>newSettingBuilder()
                  .name("BetterPhase")
                  .description("Probably broken")
                  .defaultTo(false)
                  .build();
  public final Setting<Boolean> Rotations =
          getCommandStub()
                  .builders()
                  .<Boolean>newSettingBuilder()
                  .name("Rotate")
                  .description("Allows for rotations in packetfly")
                  .defaultTo(false)
                  .build();


  public FlyMod() {
    super(Category.EXPLOIT, "PacketFly", false, "Enables flying using Position packets");
  }

    // Variable Declaration

  HashMap positions = new HashMap<>();

  int teleport_id = 0;
  int positionPacketsReceived = 0;
  int latestTeleportId = -1;

  boolean isInBlocks;
  boolean serverSprintState = false;
  boolean serverSneakState = false;

  double lastReportedX;
  double lastReportedY;
  double lastReportedZ;

  float serverYaw;
  float serverPitch;

  @Override
  protected void onEnabled()
  {

    if (MC.world == null || MC.player == null) {
      return;
    }

    serverSprintState = MC.player.isSprinting();
    serverSneakState = MC.player.isSneaking();

    lastReportedX = MC.player.posX;
    lastReportedY = MC.player.posY;
    lastReportedZ = MC.player.posZ;

    serverYaw = MC.player.rotationYaw;
    serverPitch = MC.player.rotationPitch;
  }


  @Override
  protected void onDisabled() {
    MinecraftForge.EVENT_BUS.unregister(this);

    if (latestTeleportId != -1 && MC.player != null) {
      MC.player.setPosition(lastReportedX, lastReportedY, lastReportedZ);
      MC.player.connection.sendPacket(new CPacketConfirmTeleport(latestTeleportId));

      if (Objects.nonNull(getLocalPlayer())) {
        getLocalPlayer().noClip = false;
      }
    }
  }

  @SubscribeEvent
  public void onMove(LocalPlayerUpdateMovementEvent event)
  {
    MC.player.noClip = Phase.get();
    MC.player.onGround = OnGround.get();
    if (SetBack.get())
      MC.player.setVelocity(0, 0, 0);
  }

  @SubscribeEvent
  public void onPacket(PacketEvent.Incoming.Pre event)
  {
    if (event.getPacket() instanceof SPacketPlayerPosLook) {
      if (MC.player == null || MC.world == null || MC.currentScreen instanceof GuiDownloadTerrain || !MC.player.isEntityAlive())
        return;

      SPacketPlayerPosLook packet = (SPacketPlayerPosLook)event.getPacket();

      event.setCanceled(true);

      teleport_id = packet.getTeleportId();
      lastReportedX = packet.getX();
      lastReportedY = packet.getY();
      lastReportedZ = packet.getZ();

      serverYaw = packet.getYaw();
      serverPitch = packet.getPitch();

      if (!SetBack.get() && positions.containsKey(packet.getTeleportId())) {
        final double[] posAtTime = (double[]) positions.get(packet.getTeleportId());

        positions.remove(packet.getTeleportId());

        if (posAtTime[0] == packet.getX() && posAtTime[1] == packet.getY() && posAtTime[2] == packet.getZ())
          return;
      }

      latestTeleportId = packet.getTeleportId();

      positionPacketsReceived++;

      if (SetBack.get())
        return;

      MC.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
      MC.player.connection.sendPacket(new CPacketConfirmTeleport(packet.getTeleportId()));
    }
  }

  @SubscribeEvent
  public void onPreMotion(LocalPlayerUpdateMovementEvent event)
  {
    event.setCanceled(true);


    MC.player.setVelocity(0, 0, 0);

    if (MC.player.isSprinting() != serverSprintState) {
      if (MC.player.isSprinting()) {
        MC.player.connection.sendPacket(new CPacketEntityAction(MC.player, CPacketEntityAction.Action.START_SPRINTING));
      } else {
        MC.player.connection.sendPacket(new CPacketEntityAction(MC.player, CPacketEntityAction.Action.STOP_SPRINTING));
      }

      serverSprintState = MC.player.isSprinting();
    }

    if (MC.player.isSneaking() != serverSneakState) {
      if (MC.player.isSneaking()) {
        MC.player.connection.sendPacket(new CPacketEntityAction(MC.player, CPacketEntityAction.Action.START_SNEAKING));
      } else {
        MC.player.connection.sendPacket(new CPacketEntityAction(MC.player, CPacketEntityAction.Action.STOP_SNEAKING));
      }

      serverSneakState = MC.player.isSneaking();
    }

    if (latestTeleportId == -1 || positions.size() > MaxBuffer.getAsInteger()) {
      positionPacketsReceived++;
      if (packetCheck(4))
        updatePosition(0.0, 0.0, 0.0);

      return;
    }

    double speedX = 0.0;
    double speedY = 0.0;
    double speedZ = 0.0;

    if (MC.world.getBlockState(MC.player.getPosition()).getBlock() != Blocks.AIR) {
      isInBlocks = true;
    }

    float speedSetting = speed.getAsFloat();
    float speed = (isInBlocks && !MC.gameSettings.keyBindJump.isKeyDown() && !MC.gameSettings.keyBindSneak.isKeyDown()) ? (SetBack.get() ? 0.031f : (FastPhase.get() ? 0.062f : 0.031f)) : speedSetting;
    double forward = MC.player.movementInput.moveForward;
    double strafe = MC.player.movementInput.moveStrafe;
    float yaw = MC.player.rotationYaw;

    if (MC.gameSettings.keyBindJump.isKeyDown()) {
      speedY = AntiKick.get() && (packetCheck(SetBack.get() ? 10 : 20) && !isInBlocks) ? -0.0325 : 0.056;
      if (!isInBlocks) {
        speed = 0.0f;
      } else {
        speed /= 20.0f;
        speedY /= 2.0;
      }

    } else if (MC.gameSettings.keyBindSneak.isKeyDown()) {
      speedY = -0.061;
      if (!isInBlocks) {
        speed /= 2.0f;
      } else {
        speed /= 6.0f;
        speedY /= 2.0;
      }
    } else {
      speedY = (AntiKick.get() && packetCheck(6) && !isInBlocks) ? -0.032 : 0.0;
    }

    if (forward == 0.0 && strafe == 0.0) {
      speedX = 0.0;
      speedZ = 0.0;
    } else {
      if (forward != 0.0) {
        if (strafe > 0.0) {
          yaw += ((forward > 0.0) ? -45 : 45);
        }
        else if (strafe < 0.0) {
          yaw += ((forward > 0.0) ? 45 : -45);
        }
        strafe = 0.0;
        forward = ((forward > 0.0) ? 1.0 : -1.0);
      }
      speedX = forward * speed * Math.cos(Math.toRadians(yaw + 90.0f)) + strafe * speed * Math.sin(Math.toRadians(yaw + 90.0f));
      speedZ = forward * speed * Math.sin(Math.toRadians(yaw + 90.0f)) - strafe * speed * Math.cos(Math.toRadians(yaw + 90.0f));
    }

    if (SetBack.get())
      MC.player.setPosition(lastReportedX, lastReportedY, lastReportedZ);

    for (int factor = 1, i = 0; i < Factor.getAsInteger(); i = ++factor)
      updatePosition(speedX * factor, speedY * factor, speedZ * factor);
  }

  private void updatePosition(double motionX, double motionY, double motionZ) {
    final double newX = MC.player.posX + motionX;
    final double newY = MC.player.posY + motionY;
    final double newZ = MC.player.posZ + motionZ;

    if (Rotations.get() && (MC.player.rotationYaw != serverYaw || MC.player.rotationPitch != serverPitch)) {
      MC.player.connection.sendPacket(new CPacketPlayer.PositionRotation(newX, newY, newZ, MC.player.rotationYaw, MC.player.rotationPitch, MC.player.onGround));
    } else {
      MC.player.connection.sendPacket(new CPacketPlayer.Position(newX, newY, newZ, MC.player.onGround));
    }

      MC.player.connection.sendPacket(new CPacketPlayer.Position(newX, newY + 2000.0, newZ, MC.player.onGround));


    if (!SetBack.get() && latestTeleportId != -1) {
      MC.player.connection.sendPacket(new CPacketConfirmTeleport(++latestTeleportId));
      positions.put(latestTeleportId, new double[] {newX, newY, newZ});
      positionPacketsReceived++;
      MC.player.setVelocity(motionX, motionY, motionZ);
    } else if (SetBack.get() && latestTeleportId != -1) {
      MC.player.connection.sendPacket(new CPacketConfirmTeleport(latestTeleportId));
      latestTeleportId = -1;
    }
  }

  private boolean packetCheck(int packetsReceived)
  {
    if (positionPacketsReceived >= packetsReceived) {
      positionPacketsReceived = 0;
      return true;
    }

    return false;
  }
}
