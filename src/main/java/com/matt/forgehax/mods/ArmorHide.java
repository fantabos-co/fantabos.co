package com.matt.forgehax.mods;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
@RegisterMod
public class ArmorHide extends ToggleMod {

  public ArmorHide() {
    super(Category.RENDER, "ArmorHide", false, "Hide equipped armor pieces");
  }

  @Override
  public void onEnabled() {
    ForgeHaxHooks.preventArmorRendering = true;
  }
  
  @Override
  public void onDisabled() {
    ForgeHaxHooks.preventArmorRendering = false;
  }
}
