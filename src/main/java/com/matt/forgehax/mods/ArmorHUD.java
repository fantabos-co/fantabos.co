package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

// This is nice because it doesn't change the original list but shows it in reverse
import com.google.common.collect.Lists;

import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.math.AlignHelper.Align;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
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

  private final Setting<Boolean> percentage =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("percentage")
          .description("Show pleb % damage instead of chad absolute damage")
          .defaultTo(false)
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

        RenderHelper.enableGUIStandardItemLighting();

        MC.getRenderItem().renderItemAndEffectIntoGUI(i, getPosX(slot*20), getPosY(bubble_offset));
        
        RenderHelper.disableStandardItemLighting();

        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        if (i.getCount() > 1)
          SurfaceHelper.drawText(String.format("%d", i.getCount()),
                  getPosX(slot*20 +5), getPosY(bubble_offset - 10), Colors.WHITE.toBuffer());

        if (damage.get() && i.getItemDamage() != 0) {
          int color = i.getItem().getRGBDurabilityForDisplay(i);
          String dmg;
          if (percentage.get()) dmg = String.format("%.0f%%", 100f * (1f - ((float) i.getItemDamage() / (float) i.getMaxDamage())));
          else dmg = String.format("%d", i.getMaxDamage() - i.getItemDamage());
          SurfaceHelper.drawText(dmg, getPosX((slot*20)+6), getPosY(bubble_offset-13),
                                  color, (scale.get()/2F), true);
        }
      }
      slot++;
    }

    GlStateManager.enableDepth();
    GlStateManager.disableLighting();
  }
}
