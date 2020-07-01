package com.matt.forgehax.mods;

import java.lang.reflect.Field;

import com.matt.forgehax.Helper;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * Author fsck
 * 2019-10-20.
 */
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

public final Setting<Boolean> main_also =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("lower-main")
      .description("Also lower mainhand")
      .defaultTo(false)
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
    super(Category.RENDER, "LowerHeld", false, "Lowers or hides your held items");
  }

  private Field off;
  private Field main;
  private static boolean disabled = false; // In case Reflection fails to find the field

  @Override
  public void onLoad() {
    try {
      off = ObfuscationReflectionHelper.findField(MC.entityRenderer.itemRenderer.getClass(),"field_187471_h");
      main = ObfuscationReflectionHelper.findField(MC.entityRenderer.itemRenderer.getClass(), "field_187469_f");
    } catch (Exception e) { // What does it throw?
      LOGGER.warn("Could not find fields to lower held item progress : {}", e.getMessage());
      disabled = true;
    }
  }

  @Override
  protected void onEnabled() {
    if (disabled) {
      Helper.printError("Reflection could not find necessary fields, mod cannot work");
      this.disable(false);
      return;
    }
  }

  @SubscribeEvent
  public void changeOffhandProgress(LocalPlayerUpdateEvent event) {
    if (MC.player == null) return;
    try {
      if (off.getFloat(MC.entityRenderer.itemRenderer) > offhand.get()) {
        off.set(MC.entityRenderer.itemRenderer, offhand.get());
      }
      if (main_also.get() && main.getFloat(MC.entityRenderer.itemRenderer) > mainhand.get()) {
        main.set(MC.entityRenderer.itemRenderer, mainhand.get());
      }
    } catch (IllegalAccessException e) {
      LOGGER.warn("Failed to access field with Reflection for LowerHeld");
    }
  }
}