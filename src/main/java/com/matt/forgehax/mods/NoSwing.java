package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class NoSwing extends ToggleMod {
  
  public NoSwing() {
    super(Category.PLAYER, "NoSwing", false, "Cancel swinging packets");
  }
  
  @SubscribeEvent
  public void onOutgoingPacket(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof CPacketAnimation)
      event.setCanceled(true); 
  }
}
