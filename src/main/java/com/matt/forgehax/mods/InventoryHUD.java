package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import org.lwjgl.opengl.GL11;

import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.math.AlignHelper.Align;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

@RegisterMod
public class InventoryHUD extends HudMod {

  private final Setting<Color> color =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color")
      .description("Bckground color")
      .defaultTo(Color.of(191, 97, 106, 120))
      .build();

  public InventoryHUD() {
    super(Category.GUI, "InventoryHUD", false, "Shows your inventory");
  }

  @Override
  protected Align getDefaultAlignment() { return Align.TOPLEFT; }

  @Override
  protected int getDefaultOffsetX() { return 100; }

  @Override
  protected int getDefaultOffsetY() { return 50; }

  @Override
  protected double getDefaultScale() { return 1d; }

  @SubscribeEvent
  public void onRenderScreen(RenderGameOverlayEvent.Text event) {

	GL11.glPushMatrix();

    SurfaceHelper.drawRect(getPosX(-2), getPosY(-2), (20*9)+2, (20*3)+2,
                        color.get().toBuffer());
    
    RenderHelper.enableGUIStandardItemLighting();

    
    for (int i = 0; i < 27; i++) {
        ItemStack itemStack = getLocalPlayer().inventory.mainInventory.get(i + 9);
        int offsetX = getPosX(2 + (i % 9) * 20);
        int offsetY = getPosY((i / 9) * 20);
        MC.getRenderItem().renderItemAndEffectIntoGUI(itemStack, offsetX, offsetY);//Just use vanilla methods -TheAlphaEpsilon
        MC.getRenderItem().renderItemOverlayIntoGUI(MC.fontRenderer, itemStack, offsetX, offsetY, null);
    }
    
   
    RenderHelper.disableStandardItemLighting();

    GL11.glPopMatrix();

  }
}
