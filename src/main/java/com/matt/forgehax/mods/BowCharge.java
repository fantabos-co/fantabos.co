package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterMod
public class BowCharge extends ToggleMod {

  private final Setting<Boolean> spam =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("spam")
        .description("Release the bow immediately")
        .defaultTo(false)
        .build();

  public BowCharge() {
    super(Category.COMBAT, "BowCharge", false, "Charge perfectly your bow");
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null) return;
      if (getLocalPlayer().getHeldItemMainhand().getItem() instanceof net.minecraft.item.ItemBow &&
        getLocalPlayer().isHandActive() && getLocalPlayer().getItemInUseMaxCount() >= 3 ) { // DUnno why this, thanks SalHack
          if (spam.get() || getLocalPlayer().getHeldItemMainhand().getMaxItemUseDuration()
                                                - getLocalPlayer().getItemInUseCount() >= 20) {
            getNetworkManager().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, getLocalPlayer().getHorizontalFacing()));
            getNetworkManager().sendPacket(new CPacketPlayerTryUseItem(getLocalPlayer().getActiveHand()));
            getLocalPlayer().stopActiveHand();
          }

      }
  }
}