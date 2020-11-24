package com.matt.forgehax.mods;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.asm.events.PacketEvent;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.network.play.client.CPacketConfirmTeleport;

@RegisterMod
public class PortalGodMode extends ToggleMod {

  public PortalGodMode() {
    super(Category.EXPLOIT, "PortalGodMode", false, "Cancels all TeleportConfirm packet");
  }

  public static Setting<Boolean> auto;

  {
    auto = getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("auto-enable")
      .description("Auto enable the mod when world is unloaded")
      .defaultTo(false)
      .build();
  }

  @SubscribeEvent
  public void onOutgoingPacketSent(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof CPacketConfirmTeleport) {
      event.setCanceled(true);
    }
  }
} 
