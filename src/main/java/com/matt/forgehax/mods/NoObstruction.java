package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.init.Items;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Created by Tonio on 30/07/2020
 * Upgrated by Fleyr on 11/08/2020 <3
 */
@RegisterMod
public class NoObstruction extends ToggleMod {

  public final Setting<Boolean> all =
  getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("all")
      .description("ignore collisions everytime")
      .defaultTo(true)
      .build();
  public final Setting<Boolean> pick =
  getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("pick")
      .description("ignore collisions if holding a (diamond) pickaxe")
      .defaultTo(true)
      .build();
  public final Setting<Boolean> crystal =
  getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("crystal")
        .description("ignore collisions if holding a crystal")
        .defaultTo(false)
        .build();
  
  public NoObstruction() {
    super(Category.EXPLOIT, "NoObstruction", false, "Allows you to break and place through entities");
  }
  
  //Made by Fleyr 11th August 2020
  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event){
    if (getLocalPlayer() == null) return;

    if(all.get())
      ForgeHaxHooks.allowPlaceThroughEntities = true;
    else if(pick.get() && getLocalPlayer().getHeldItemMainhand().getItem().equals(Items.DIAMOND_PICKAXE))
      ForgeHaxHooks.allowPlaceThroughEntities = true;
    else if(crystal.get() && getLocalPlayer().getHeldItemMainhand().getItem().equals(Items.END_CRYSTAL))
      ForgeHaxHooks.allowPlaceThroughEntities = true;
    else
      ForgeHaxHooks.allowPlaceThroughEntities = false;
  }

  @Override
  public void onDisabled() {
    ForgeHaxHooks.allowPlaceThroughEntities = false;
  }
}
