package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

// This is nice because it doesn't change the original list but shows it in reverse
import com.google.common.collect.Lists;

import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.math.AlignHelper.Align;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

/**
 * Took inspiration from 086's code on KAMI
 */


@RegisterMod
public class ArmorHUD extends HudMod {
  
  private final Setting<Boolean> damage =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("damage")
          .description("Show item damage")
          .defaultTo(true)
          .build();
    
  private final Setting<Boolean> water_offset =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("water-offset")
          .description("Shift HUD up when in water")
          .defaultTo(true)
          .build();

  public ArmorHUD() {
    super(Category.GUI, "ArmorHUD", true, "Shows equipped armor above hotbar");
  }

  @Override
  protected Align getDefaultAlignment() { return Align.BOTTOM; }

  @Override
  protected int getDefaultOffsetX() { return 10; }

  @Override
  protected int getDefaultOffsetY() { return 56; }

  @Override
  protected double getDefaultScale() { return 1d; }

  private boolean inWater = false;

  @SubscribeEvent
  public void onRenderOverlay(RenderGameOverlayEvent event) {
    if (event.getType() == RenderGameOverlayEvent.ElementType.AIR)
      inWater = true;
  }

  @SubscribeEvent
  public void onRenderScreen(RenderGameOverlayEvent.Text event) {
    GlStateManager.enableTexture2D();
    int slot = 0;
    int bubble_offset;

    if (!getLocalPlayer().isInWater()) inWater = false;   
    if (water_offset.get() && inWater) bubble_offset = 12;
    else bubble_offset = 0;

    for (ItemStack i : Lists.reverse(getLocalPlayer().inventory.armorInventory)) {
      if (!i.equals(ItemStack.EMPTY) && i.getItem() != null) {
        // int x = i - 90 + (9 - iteration) * 20 + 2;
        GlStateManager.enableDepth();

        SurfaceHelper.drawItem(i, getPosX(slot*20), getPosY(bubble_offset));

        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        if (i.getCount() > 1)
          SurfaceHelper.drawText(String.format("%d", i.getCount()),
                  getPosX(slot*20 +5), getPosY(bubble_offset - 10), Colors.WHITE.toBuffer());

        if (damage.get() && i.getItemDamage() != 0) {
          int color = i.getItem().getRGBDurabilityForDisplay(i);
          int dmg = i.getMaxDamage() - i.getItemDamage();
          SurfaceHelper.drawText(String.format("%d", dmg),
                                  getPosX((slot*20)+6), getPosY(bubble_offset-13),
                                  color, (scale.get()/2F), true);
        }
      }
      slot++;
    }

    GlStateManager.enableDepth();
    GlStateManager.disableLighting();
  }
}
