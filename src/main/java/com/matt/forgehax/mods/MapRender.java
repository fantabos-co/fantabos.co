package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.RenderItemAndEffectIntoGuiEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Optional;

/**
 * Created by IronException and TheAlphaEpsilon
 */

@RegisterMod
public class MapRender extends ToggleMod {

  private final Setting<Float> scale =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("scale")
      .description("The scale of how big the map should be rendered")
      .defaultTo(1.0f / 1.2f)
      .build();

  private final Setting<Boolean> background =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("background")
      .description("Whether the background image should also get drawn or not")
      .defaultTo(true)
      .build();

  private static final ResourceLocation RES_MAP_BACKGROUND = new ResourceLocation("textures/map/map_background.png");

  public MapRender() {
    super(Category.RENDER, "MapRender", false, "renders maps");
  }
  
  @SubscribeEvent
  public void overrideRendering(RenderItemAndEffectIntoGuiEvent event) {
	  
	  if(MC.world == null || MC.player == null) {
		  return;
	  }
	  
	  getMapData(event.getStack()).ifPresent(data -> {
		 event.setCanceled(true); 
		 drawMap(data, event.getX(), event.getY(), 1.0f / 8.0f, false);
	  });
	  
  }
  
  //To draw new gui in tooltips
  @SubscribeEvent
  public void drawNewGui(final RenderTooltipEvent.PostText event) {
    
    GlStateManager.disableDepth();
    GlStateManager.enableBlend();
    GlStateManager.disableLighting();
    GlStateManager.color(1, 1, 1);
    
    getMapData(event.getStack()).ifPresent(data -> drawMap(data, event.getX() - 4, event.getY() + event.getHeight() + 8, scale.get(), background.get()));

    GlStateManager.enableLighting();
    GlStateManager.disableBlend();
    GlStateManager.enableDepth();
  }

  private Optional<MapData> getMapData(final ItemStack stack) {
    if (!(stack.getItem() instanceof ItemMap)) {
      return Optional.empty();
    }
    return Optional.ofNullable(((ItemMap) stack.getItem()).getMapData(stack, MC.world));
  }

  // a little changed from quarks: https://github.com/Vazkii/Quark/blob/master/src/main/java/vazkii/quark/client/tooltip/MapTooltips.java
 
  /**
   * draws the map
   */
  private void drawMap(final MapData data, final int x, final int y, final float scale, final boolean background) {
    if (data == null) {
      return;
    }

    if (MC.entityRenderer.getMapItemRenderer().getMapInstanceIfExists(data.mapName) == null) {
      return;
    }


    // scale was 0.5 in quarks => 2 with what we do now


    GlStateManager.translate(x, y, 500);
    GlStateManager.scale(scale, scale, scale);
    GlStateManager.disableDepth();
    
    int pad = 0;
    double textureOffset = 7.0d / 142.0d;
    if (background) {
      pad = 7;
      textureOffset = 0;

    }
    final float size = 128 + pad;

    MC.getTextureManager().bindTexture(RES_MAP_BACKGROUND);
    final Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferbuilder = tessellator.getBuffer();
    bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
    bufferbuilder.pos(-pad, size, 0.0D).tex(textureOffset, 1.0D - textureOffset).endVertex();
    bufferbuilder.pos(size, size, 0.0D).tex(1.0D - textureOffset, 1.0D - textureOffset).endVertex();
    bufferbuilder.pos(size, -pad, 0.0D).tex(1.0D - textureOffset, textureOffset).endVertex();
    bufferbuilder.pos(-pad, -pad, 0.0D).tex(textureOffset, textureOffset).endVertex();
    tessellator.draw();

    MC.entityRenderer.getMapItemRenderer().renderMap(data, true);

    GlStateManager.scale(1.0 / scale, 1.0 / scale, 1.0 / scale);
    GlStateManager.translate(-x, -y, -500);

  }


}
