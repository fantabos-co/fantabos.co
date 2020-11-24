package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.Helper.getPlayerController;
import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.asm.events.PlayerDamageBlockEvent;
import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class FastBreak extends ToggleMod {

  private final Setting<Boolean> packet_spam =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("packet-spam")
      .description("Abuse packet spam to make breaking faster")
      .defaultTo(false)
      .build();
  
  public FastBreak() {
    super(Category.PLAYER, "FastBreak", false, "Fast break retard");
  }
  
  @SubscribeEvent
  public void onUpdate(LocalPlayerUpdateEvent event) {
    if (MC.playerController != null) {
      FastReflection.Fields.PlayerControllerMP_blockHitDelay.set(MC.playerController, 0);
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onBlockBreak(PlayerDamageBlockEvent event) {
    if (!event.isCanceled() ) {
      if (packet_spam.get()) {
        getLocalPlayer().swingArm(EnumHand.MAIN_HAND);
        getNetworkManager().sendPacket(
          new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, event.getPos(), event.getSide()));
        getNetworkManager().sendPacket(
          new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, event.getPos(), event.getSide()));
        event.setCanceled(true);
      }
    }
  }
}
