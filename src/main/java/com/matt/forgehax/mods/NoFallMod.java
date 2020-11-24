package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getNetworkManager;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class NoFallMod extends ToggleMod {

  public enum noFallMode {
    TELEPORT,
    ONGROUND
  }
  
  public final Setting<noFallMode> mode =
    getCommandStub()
      .builders()
      .<noFallMode>newSettingEnumBuilder()
      .name("mode")
      .description("Mode to use for noFall [teleport|onGround]")
      .defaultTo(noFallMode.ONGROUND)
      .build();

  public final Setting<Float> threshold =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("threshold")
      .description("Trigger after falling these many blocks")
      .min(0F)
      .max(100F)
      .defaultTo(4F)
      .build();
  
  public NoFallMod() {
    super(Category.MOVEMENT, "NoFall", false, "Prevents fall damage from being taken");
  }
  
  private float lastFallDistance = 0;
  
  @SubscribeEvent
  public void onPacketSend(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof CPacketPlayer
        && !(event.getPacket() instanceof CPacketPlayer.Rotation)
        && !PacketHelper.isIgnored(event.getPacket())) {
      CPacketPlayer packetPlayer = event.getPacket();
      switch (mode.get()) {
        case TELEPORT:
          if (FastReflection.Fields.CPacketPlayer_onGround.get(packetPlayer) && lastFallDistance >= threshold.get()) {
            CPacketPlayer packet =
                new CPacketPlayer.PositionRotation(
                    ((CPacketPlayer) event.getPacket()).getX(0),
                    1337 + ((CPacketPlayer) event.getPacket()).getY(0),
                    ((CPacketPlayer) event.getPacket()).getZ(0),
                    ((CPacketPlayer) event.getPacket()).getYaw(0),
                    ((CPacketPlayer) event.getPacket()).getPitch(0),
                    true);
            CPacketPlayer reposition =
                new CPacketPlayer.PositionRotation(
                    ((CPacketPlayer) event.getPacket()).getX(0),
                    ((CPacketPlayer) event.getPacket()).getY(0),
                    ((CPacketPlayer) event.getPacket()).getZ(0),
                    ((CPacketPlayer) event.getPacket()).getYaw(0),
                    ((CPacketPlayer) event.getPacket()).getPitch(0),
                    true);
            PacketHelper.ignore(packet);
            PacketHelper.ignore(reposition);
            getNetworkManager().sendPacket(packet);
            getNetworkManager().sendPacket(reposition);
            lastFallDistance = 0;
          } else {
            lastFallDistance = getLocalPlayer().fallDistance;
          }
          break;
        case ONGROUND:
          if (getLocalPlayer().fallDistance > threshold.get()) {
            FastReflection.Fields.CPacketPlayer_onGround.set(packetPlayer, true);
          }
      }
    }
  }
}
