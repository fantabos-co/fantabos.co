package com.matt.forgehax.mods;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.asm.events.DrawBlockBoundingBoxEvent;
import com.matt.forgehax.mods.services.RainbowService;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class BlockHighlightMod extends ToggleMod {

  private final Setting<Color> color =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color")
      .description("Color for trail")
      .defaultTo(Color.of(191, 97, 106, 255))
      .build();

  private final Setting<Boolean> rainbow =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("rainbow")
        .description("Use rainbow color instead")
        .defaultTo(false)
        .build();
  
  private final Setting<Float> width =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("width")
      .description("line width")
      .min(0.f)
      .max(10f)
      .defaultTo(5.f)
      .build();
  
  public BlockHighlightMod() {
    super(
        Category.RENDER, "BlockHighlight", false, "Make selected block bounding box more visible");
  }

  @Override
  protected void onEnabled() {
    ForgeHaxHooks.drawBlockHighlightInWater = true;
  }

  @Override
  protected void onDisabled() {
    ForgeHaxHooks.drawBlockHighlightInWater = false;
  }
  
  private float toFloat(int colorVal) {
    return colorVal / 255.f;
  }

  @SubscribeEvent
  public void onRenderBoxPre(DrawBlockBoundingBoxEvent.Pre event) {
    Color c;
    if (rainbow.get()) c = RainbowService.getRainbowColorClass();
    else c = color.get();
    GlStateManager.disableDepth();
    GlStateManager.glLineWidth(width.get());
    event.alpha = toFloat(c.getAlpha());
    event.red = toFloat(c.getRed());
    event.green = toFloat(c.getGreen());
    event.blue = toFloat(c.getBlue());
  }
  
  @SubscribeEvent
  public void onRenderBoxPost(DrawBlockBoundingBoxEvent.Post event) {
    GlStateManager.enableDepth();
  }
}
