package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.color.Color;
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


  @SubscribeEvent
  public void onRenderScreen(RenderGameOverlayEvent.Text event) {
    GlStateManager.enableTexture2D();
    int slot = 0;
    int bubble_offset;
    
    if (water_offset.get() && getLocalPlayer().isInWater()) bubble_offset = 12;
    else bubble_offset = 0;
    for (ItemStack i : MC.player.inventory.armorInventory) {
      if (!i.equals(ItemStack.EMPTY) && i.getItem() != null) {
        // int x = i - 90 + (9 - iteration) * 20 + 2;
        GlStateManager.enableDepth();

        SurfaceHelper.drawItem(i, getPosX(slot*20), getPosY(bubble_offset));

        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        if (damage.get()) {
          int color;
          int dmg = i.getMaxDamage() - i.getItemDamage();
          if (i.getMaxDamage() == 0) {
            color = Color.of(0, 255, 0, 255).toBuffer(); // Zero max damage, always repaired?
          } else {
            int green = (int) (((float) dmg / (float) i.getMaxDamage()) * 255F); // This is smart! Thanks 086!
            int red = 255 - green;
	          color = Color.of(red, green, 0, 255).toBuffer();
          }
          SurfaceHelper.drawText(String.format("%d", dmg), getPosX((slot*20)+6), getPosY(bubble_offset-13), color, (scale.get()/2F), false);
        }
      }
      slot++;
    }

    GlStateManager.enableDepth();
    GlStateManager.disableLighting();
  }
}
