package com.matt.forgehax.mods;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import com.matt.forgehax.asm.ForgeHaxHooks;

@RegisterMod
public class NoGhostBlock extends ToggleMod {

  public final Setting<Boolean> placing =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("place")
      .description("Prevent ghost places")
      .defaultTo(true)
      .build();

  public final Setting<Boolean> breaking =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("break")
      .description("Prevent ghost breaks")
      .defaultTo(true)
      .build();
  
  public NoGhostBlock() {
    super(Category.WORLD, "NoGhostBlock", false, "Prevent clientside blocks");
  }

  @SubscribeEvent
  public void onUpdate(TickEvent.ClientTickEvent event) {
    ForgeHaxHooks.doPreventGhostBlocksPlace = placing.get();
    ForgeHaxHooks.doPreventGhostBlocksBreak = breaking.get();
  }

  @Override
  protected void onDisabled() {
    ForgeHaxHooks.doPreventGhostBlocksPlace = false;
    ForgeHaxHooks.doPreventGhostBlocksBreak = false;
  }

} 
