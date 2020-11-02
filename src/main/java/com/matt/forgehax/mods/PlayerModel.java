package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.util.math.AlignHelper.Align;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class PlayerModel extends HudMod {
  
  public PlayerModel() {
    super(Category.GUI, "PlayerModel", false, "Render a player model on screen");
  }

  @Override
  protected Align getDefaultAlignment() {
    return Align.TOPRIGHT;
  }

  @Override
  protected int getDefaultOffsetX() {
    return 50;
  }

  @Override
  protected int getDefaultOffsetY() {
    return 10;
  }

  @Override
  protected double getDefaultScale() {
    return 20d;
  }
  
  @SubscribeEvent
  public void onRender(Render2DEvent event) {
    if (getLocalPlayer() == null) return;
    int scale_int = (int) Math.round(scale.get());
    renderPlayer(getPosX(0), getPosY(0), scale_int,
                    (float)Math.atan((double)(getLocalPlayer().rotationYaw / 40.0F)) * 40.0F,
                    (float)Math.atan((double)(getLocalPlayer().rotationPitch / 40.0F)) * 40.0F,
                    getLocalPlayer());
  }

  // Overridden to not do silly player reorienting
  // This comes from GuiInventory.drawEntityOnScreen
  private static void renderPlayer(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent)
  {
      GlStateManager.enableColorMaterial();
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)posX, (float)posY, 50.0F);
      GlStateManager.scale((float)(-scale), (float)scale, (float)scale);
      GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
      RenderHelper.enableStandardItemLighting();
      GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(-((float)Math.atan((double)(mouseY / 40.0F))) * 20.0F, 1.0F, 0.0F, 0.0F);
      GlStateManager.translate(0.0F, 0.0F, 0.0F);
      RenderManager rendermanager = MC.getRenderManager();
      rendermanager.setPlayerViewY(180.0F);
      rendermanager.setRenderShadow(false);
      rendermanager.renderEntity(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
      rendermanager.setRenderShadow(true);
      GlStateManager.popMatrix();
      RenderHelper.disableStandardItemLighting();
      GlStateManager.disableRescaleNormal();
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.disableTexture2D();
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
  }

}
