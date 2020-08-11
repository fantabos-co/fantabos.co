package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.asm.events.BlockPlaceSoundEvent;

@RegisterMod
public class NoGhostBlock extends ToggleMod {

  // public final Setting<Boolean> placing =
  //   getCommandStub()
  //     .builders()
  //     .<Boolean>newSettingBuilder()
  //     .name("place")
  //     .description("Prevent ghost places")
  //     .defaultTo(true)
  //     .build();
  // 
  // public final Setting<Boolean> breaking =
  //   getCommandStub()
  //     .builders()
  //     .<Boolean>newSettingBuilder()
  //     .name("break")
  //     .description("Prevent ghost breaks")
  //     .defaultTo(true)
  //     .build();
  
  public NoGhostBlock() {
    super(Category.WORLD, "NoGhostBlock", false, "Prevent clientside blocks");
  }

  // @SubscribeEvent
  // public void onUpdate(TickEvent.ClientTickEvent event) {
  //   ForgeHaxHooks.doPreventGhostBlocksPlace = placing.get();
  //   ForgeHaxHooks.doPreventGhostBlocksBreak = breaking.get();
  // }

  @Override
  protected void onEnabled() {
    ForgeHaxHooks.doPreventGhostBlocksPlace = true;
    // ForgeHaxHooks.doPreventGhostBlocksBreak = false;
  }

  @Override
  protected void onDisabled() {
    ForgeHaxHooks.doPreventGhostBlocksPlace = false;
    // ForgeHaxHooks.doPreventGhostBlocksBreak = false;
  }

  @SubscribeEvent
  public void onPlaceSound(BlockPlaceSoundEvent event) {
    if (/*!placing.get() || */getLocalPlayer() == null ||
        !(getLocalPlayer().getHeldItemMainhand().getItem() instanceof ItemBlock))
      return;
    ItemBlock block = (ItemBlock) getLocalPlayer().getHeldItemMainhand().getItem();
    event.setSound(block.getBlock().getSoundType());
  }
} 
