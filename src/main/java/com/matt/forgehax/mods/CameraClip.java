package com.matt.forgehax.mods;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

/**
 * Created by tonio on 26/10/2020.
 *      eww doing this as a mod is wasteful
 *          but could I put it as a setting???
 */
@RegisterMod
public class CameraClip extends ToggleMod {

  public CameraClip() {
    super(Category.PLAYER, "CameraClip", true, "Allow 3rd person camera to clip walls");
  }
  
  @Override
  public void onEnabled() {
    ForgeHaxHooks.allowCameraClip = true;
  }
  
  @Override
  public void onDisabled() {
    ForgeHaxHooks.allowCameraClip = false;
  }
}
