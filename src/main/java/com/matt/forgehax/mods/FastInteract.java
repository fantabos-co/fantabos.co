package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created on 9/4/2016 by fr1kin (FastUseMod)
 * Updated in 2020
 */
@RegisterMod
public class FastInteract extends ToggleMod {

  private final Setting<Boolean> all =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("all")
      .description("just FastUse everything")
      .defaultTo(true)
      .build();
  private final Setting<Boolean> xp =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("xp")
      .description("FastUse when holding xp bottles")
      .defaultTo(true)
      .build();
  private final Setting<Boolean> place =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("place")
      .description("FastUse when holding a block")
      .defaultTo(false)
      .build();
  
  public FastInteract() {
    super(Category.PLAYER, "FastInteract", false, "Set player action delay to 0");
  }
  
  @SubscribeEvent
  public void onUpdate(LocalPlayerUpdateEvent event) {
    if (all.get() ||
        (place.get() && getLocalPlayer().getHeldItemMainhand().getItem() instanceof ItemBlock) ||
        (xp.get() && getLocalPlayer().getHeldItemMainhand().getItem().equals(Items.EXPERIENCE_BOTTLE))
    )
    FastReflection.Fields.Minecraft_rightClickDelayTimer.set(MC, 0);
  }
}
