package com.matt.forgehax.mods;

import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class LowerHeld extends ToggleMod {

  public final Setting<Float> offhand =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("offhand")
      .description("Offhand progress")
      .min(0F)
      .max(1.0F)
      .defaultTo(0.5F)
      .build();

  public final Setting<Float> mainhand =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("main")
      .description("Main Hand progress")
      .min(0F)
      .max(1.0F)
      .defaultTo(1.0F)
      .build();

  public LowerHeld() {
    super(Category.PLAYER, "LowerHeld", false, "Lowers or hides your held items");
  }

  @SubscribeEvent
  public void changeOffhandProgress(LocalPlayerUpdateEvent event) {
    if (MC.player == null) return;
    if (FastReflection.Fields.ItemRenderer_equippedProgressOffHand.get(MC.entityRenderer.itemRenderer) > offhand.get())
      FastReflection.Fields.ItemRenderer_equippedProgressOffHand.set(MC.entityRenderer.itemRenderer, offhand.get());
    if (FastReflection.Fields.ItemRenderer_equippedProgressMainHand.get(MC.entityRenderer.itemRenderer) > mainhand.get())
      FastReflection.Fields.ItemRenderer_equippedProgressMainHand.set(MC.entityRenderer.itemRenderer, mainhand.get());
  }
}