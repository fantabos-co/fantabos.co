package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.Helper.getLocalPlayer;

import java.util.LinkedList;
import java.util.Queue;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.network.play.client.CPacketClientStatus;
import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class Blink extends ToggleMod {

  private final Setting<Integer> limit =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("limit")
      .description("Max number of packets to hold, 0 to disable")
      .min(0)
      .max(1000)
      .defaultTo(0)
      .build();
  private final Setting<Boolean> reenable =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("walk")
      .description("After blinking, start new blink")
      .defaultTo(false)
      .build();
  
  public Blink() {
    super(Category.MOVEMENT, "Blink", false, "Holds all movement packets until turned off");
  }

  @Override
  public String getDisplayText() {
    return (getModName() + " [" + TextFormatting.RED + packetBuf.size() + TextFormatting.WHITE + "]");
  }

  private Queue<CPacketPlayer> packetBuf = new LinkedList<CPacketPlayer>();
  private double x, y, z;

  @Override
  protected void onDisabled() {
    try {
      while (packetBuf.size() > 0) {
        getNetworkManager().sendPacket(packetBuf.poll());
      }
    } catch (Exception e) {
      packetBuf.clear();
    }
  }

  @Override
  protected void onEnabled() {
    x = getLocalPlayer().posX;
    y = getLocalPlayer().posY;
    z = getLocalPlayer().posZ;
  }

  @SubscribeEvent
  public void onWorldUnload(WorldEvent.Unload event) {
    this.disable(false);
  }
  
  @SubscribeEvent()
  public void onOutgoingPacket(PacketEvent.Outgoing.Pre event) {
    if (!(event.getPacket() instanceof CPacketKeepAlive ||
          event.getPacket() instanceof CPacketTabComplete ||
          event.getPacket() instanceof CPacketChatMessage ||
          event.getPacket() instanceof CPacketClientStatus)) {
      if (limit.get() > 0 && packetBuf.size() >= limit.get()) {
        this.disable(false);
        if (reenable.get()) {
          this.enable(false);
        }
      } else {
        packetBuf.add(event.getPacket());
        event.setCanceled(true);
      }
    }
  }
}