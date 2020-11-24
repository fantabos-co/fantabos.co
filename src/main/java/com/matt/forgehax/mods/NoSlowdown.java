package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getNetworkManager;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.asm.events.DoBlockCollisionsEvent;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.block.BlockWeb;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemShield;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerDigging.Action;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class NoSlowdown extends ToggleMod {

  private final Setting<Boolean> ncp_strict =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("ncp-strict")
      .description("Try to avoid being stopped by NCP")
      .defaultTo(false)
      .build();
  
  public NoSlowdown() {
    super(Category.PLAYER, "NoSlowDown", false, "Disables block slowdown");
  }
  
  @Override
  public void onEnabled() {
    ForgeHaxHooks.isNoSlowDownActivated = true;
    try {
      ForgeHaxHooks.LIST_BLOCK_FILTER.add(BlockSoulSand.class);
      ForgeHaxHooks.LIST_BLOCK_FILTER.add(BlockWeb.class);
    } catch (Exception e) {
    }
  }
  
  @Override
  public void onDisabled() {
    ForgeHaxHooks.isNoSlowDownActivated = false;
    try {
      ForgeHaxHooks.LIST_BLOCK_FILTER.remove(BlockSoulSand.class);
      ForgeHaxHooks.LIST_BLOCK_FILTER.remove(BlockWeb.class);
    } catch (Exception e) {
    }
  }

  private boolean isUsingSlowingItem(Item item) {
    if (getLocalPlayer().isHandActive() &&
        (item instanceof ItemFood ||
         item instanceof ItemBow ||
         item instanceof ItemShield ||
         item instanceof ItemPotion))
          return true;
      return false;
  }

  @SubscribeEvent
  public void onPacketSend(PacketEvent.Outgoing.Post event) {
    if (event.getPacket() instanceof CPacketPlayer && ncp_strict.get() && !getLocalPlayer().isRiding() &&
        isUsingSlowingItem(getLocalPlayer().getActiveItemStack().getItem())) {
      getNetworkManager().sendPacket(
        new CPacketPlayerDigging(Action.ABORT_DESTROY_BLOCK, getLocalPlayer().getPosition(), EnumFacing.DOWN));
    }
  }
  
  @SubscribeEvent
  public void onDoApplyBlockMovement(DoBlockCollisionsEvent event) {
    if (event.getEntity().equals(getLocalPlayer())) {
      if (Block.getIdFromBlock(event.getState().getBlock()) == 88) { // soul sand
        event.setCanceled(true);
      }
    }
  }
}
