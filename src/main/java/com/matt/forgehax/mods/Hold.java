package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.asm.events.LocalPlayerUpdateMovementEvent;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class Hold extends ToggleMod {

  private final Setting<Boolean> aggressive =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("cancel-packets")
      .description("Cancel almost all packets while holding")
      .defaultTo(false)
      .build();
  private final Setting<Float> threshold =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("threshold")
      .description("Min fall height to trigger, set to 0 for manual use")
      .min(0.F)
      .max(256.F)
      .defaultTo(0.F)
      .build();
      
  public Hold() {
    super(Category.MOVEMENT, "Hold", false, "Cancel movement and keeps you floating");
  }
  
  @SubscribeEvent
  public void onOutgoingPacket(PacketEvent.Outgoing.Pre event) {
    if (!aggressive.get()
        || getLocalPlayer() == null
        || getLocalPlayer().fallDistance < threshold.get()
        || event.getPacket() instanceof CPacketKeepAlive
        || event.getPacket() instanceof CPacketChatMessage
        || event.getPacket() instanceof CPacketTabComplete) return;
    event.setCanceled(true);
  }

  @SubscribeEvent
  public void onMovement(LocalPlayerUpdateMovementEvent event) {
    if (getLocalPlayer() == null || getLocalPlayer().fallDistance < threshold.get()) return;
    getLocalPlayer().setVelocity(0, 0, 0);
  }
}