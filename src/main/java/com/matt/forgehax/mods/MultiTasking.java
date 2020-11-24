package com.matt.forgehax.mods;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;


@RegisterMod
public class MultiTasking extends ToggleMod {

  public MultiTasking() {
    super(Category.EXPLOIT, "MultiTasking", false, "Allows you to do multiple actions together");
  }
  
  @Override
  public void onEnabled() {
    ForgeHaxHooks.makeHandAlwaysInactive = true;
    ForgeHaxHooks.makeIsHittingBlockAlwaysFalse = true;
  }
  
  @Override
  public void onDisabled() {
    ForgeHaxHooks.makeHandAlwaysInactive = false;
    ForgeHaxHooks.makeIsHittingBlockAlwaysFalse = false;
  }
}
