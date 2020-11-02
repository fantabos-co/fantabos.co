package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class SoundFilter extends ToggleMod {

  public SoundFilter() {
    super(Category.WORLD, "SoundFilter", false, "Cancel certain sounds");
  }

  public final Setting<Boolean> explosion =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("explosion")
      .description("Filter explosion sounds")
      .defaultTo(true)
      .build();

  public final Setting<Boolean> thunder =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("thunder")
      .description("Filter thunder storm sounds")
      .defaultTo(true)
      .build();

  // public final Setting<Boolean> totem =
  //   getCommandStub()
  //     .builders()
  //     .<Boolean>newSettingBuilder()
  //     .name("totem")
  //     .description("Filter totem pop sounds")
  //     .defaultTo(false)
  //     .build();

  public final Setting<Boolean> pigmen =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("pigmen")
      .description("Filter zombie pigmen ambient noise")
      .defaultTo(false)
      .build();
  public final Setting<Boolean> ghast =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("ghast")
      .description("Filter ghast ambient noise")
      .defaultTo(false)
      .build();

  @SubscribeEvent
  public void onPacketReceived(PacketEvent.Incoming.Pre event) {
    if (event.getPacket() instanceof SPacketSoundEffect) {
      SPacketSoundEffect packet = event.getPacket();
      if (explosion.get() && packet.getSound().equals(SoundEvents.ENTITY_GENERIC_EXPLODE))
        event.setCanceled(true);
      else if (thunder.get() && packet.getSound().equals(SoundEvents.ENTITY_LIGHTNING_THUNDER))
        event.setCanceled(true);
      // else if (totem.get() && packet.getSound().equals(SoundEvents.ITEM_TOTEM_USE))
      //   event.setCanceled(true);
      else if (pigmen.get() && packet.getSound().equals(SoundEvents.ENTITY_ZOMBIE_PIG_AMBIENT))
        event.setCanceled(true);
      else if (ghast.get() && (packet.getSound().equals(SoundEvents.ENTITY_GHAST_AMBIENT) ||
                               packet.getSound().equals(SoundEvents.ENTITY_GHAST_SCREAM)))
        event.setCanceled(true);
    }
  }
}
