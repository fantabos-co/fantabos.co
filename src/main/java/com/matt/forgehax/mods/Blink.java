package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getNetworkManager;

import java.util.LinkedList;
import java.util.Queue;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.text.TextFormatting;
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
      .defaultTo(0)
      .build();
  
  public Blink() {
    super(Category.MOVEMENT, "Blink", false, "Holds all movement packets until turned off");
  }

  @Override
  public String getDisplayText() {
    return (getModName() + " [" + TextFormatting.RED + packetBuf.size() + TextFormatting.WHITE + "]");
  }

  private Queue<CPacketPlayer> packetBuf = new LinkedList<CPacketPlayer>();

  @Override
  protected void onDisabled() {
    while (packetBuf.size() > 0) {
      getNetworkManager().sendPacket(packetBuf.poll());
    }
  }
  
  @SubscribeEvent()
  public void onOutgoingPacket(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof CPacketPlayer) {
      if (limit.get() > 0 && packetBuf.size() >= limit.get()) {
        this.disable(false);
      } else {
        packetBuf.add(event.getPacket());
        event.setCanceled(true);
      }
    }
  }
}