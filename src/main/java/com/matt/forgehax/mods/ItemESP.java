package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.asm.events.RenderEntityItem2dEvent;
import com.matt.forgehax.asm.events.RenderEntityItem3dEvent;
import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.mods.services.RainbowService;
import com.matt.forgehax.util.draw.RenderUtils;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.math.Plane;
import com.matt.forgehax.util.math.VectorUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

@RegisterMod
public class ItemESP extends ToggleMod {

  public ItemESP() {
    super(Category.RENDER, "ItemESP", false, "ESP for items");
  }

  @Override
  public String getDisplayText() {
    long count = getWorld() // Jank but fr1kin loves streams :(
      .loadedEntityList
      .stream()
      .filter(EntityItem.class::isInstance)
      .map(EntityItem.class::cast)
      .filter(entity -> entity.ticksExisted > 1)
      .count();
    return (getModName() + " [" + TextFormatting.DARK_GREEN + count + TextFormatting.RESET + "]");
  }
  public final Setting<Double> scale =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("scale")
          .description("Scaling for text")
          .defaultTo(1.D)
          .min(0.D)
          .max(5D)
          .build();

  public final Setting<Boolean> age =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("age")
          .description("Show how long the item has existed (clientside)")
          .defaultTo(false)
          .build();

  private final Setting<Boolean> nametags =
		  getCommandStub()
		  	.builders()
		  	.<Boolean>newSettingBuilder()
		  	.name("nametag")
		  	.description("Renders a nametag above items")
		  	.defaultTo(true)
		  	.build();

  public final Setting<Double> boxOffsetY =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("box-offset-y")
          .description("Y offset for 3D box")
          .min(0D)
          .max(10D)
          .defaultTo(0D)
          .build();

  public final Setting<Float> width =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("width")
          .description("Outline width")
          .min(0f)
          .max(10f)
          .defaultTo(1.0F)
          .build();

  private final Setting<Color> color =
      getCommandStub()
          .builders()
          .newSettingColorBuilder()
          .name("color")
          .description("Color for drawn box")
          .defaultTo(Color.of(128, 128, 128, 200))
          .build();

	private final Setting<Boolean> rainbow =
			getCommandStub()
			  	.builders()
			  	.<Boolean>newSettingBuilder()
			  	.name("rainbow")
			  	.description("Use rainbow color instead")
			  	.defaultTo(false)
			  	.build();

  public final Setting<Boolean> antialias =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("antialias")
          .description("Makes lines and triangles more smooth, may hurt performance")
          .defaultTo(true)
          .build();
  private final Setting<TYPE> type =
		  getCommandStub()
		  	.builders()
		  	.<TYPE>newSettingEnumBuilder()
		  	.name("type")
		  	.description("ESP type; outline or wireframe)")
		  	.defaultTo(TYPE.OUTLINE)
		  	.build();
  
  private final Setting<Boolean> ontop =
		  getCommandStub()
		  	.builders()
		  	.<Boolean>newSettingBuilder()
		  	.name("ontop")
		  	.description("Renders the esp over actual entity (only for wireframe)")
		  	.defaultTo(true)
		  	.build();

  // private final int MAX_AGE = 6000; not needed! Awesome!
  private final int TICKS_SECOND = 20;

  @SubscribeEvent
  public void onRenderItem3d(RenderEntityItem3dEvent event) {
	  
    Color c;
    if (rainbow.get()) c = RainbowService.getRainbowColorClass();
    else c = color.get();

	  boolean fancyGraphics = MC.gameSettings.fancyGraphics; //Dont need fancy
	  MC.gameSettings.fancyGraphics = false;
	  float gamma = MC.gameSettings.gammaSetting;
	  MC.gameSettings.gammaSetting = 10000.0f; //For extreme lines
		
	  //Renders the actual entity first
	  if (this.ontop.getAsBoolean()) {
		  event.renderItem.renderItem(event.stack, event.transformedModel);
	  }
		
	  if(type.get() == TYPE.OUTLINE) {
		  
		  RenderUtils.renderOne(width.getAsFloat());
		  event.renderItem.renderItem(event.stack, event.transformedModel);
		  GlStateManager.glLineWidth(width.getAsFloat());
	      
	      RenderUtils.renderTwo();
		  event.renderItem.renderItem(event.stack, event.transformedModel);
	      GlStateManager.glLineWidth(width.getAsFloat());
	      
	      RenderUtils.renderThree();
	      RenderUtils.renderFour(c);
		  event.renderItem.renderItem(event.stack, event.transformedModel);
	      GlStateManager.glLineWidth(width.getAsFloat());
	     
	      RenderUtils.renderFive();
		  
	  } else {
		  
		  GL11.glPushMatrix();
          GL11.glPushAttrib(1048575);
          GL11.glPolygonMode(1032, 6913);
          
          GL11.glDisable(3553);
          GL11.glDisable(2896);
          GL11.glDisable(2929);
          GL11.glEnable(2848);
          GL11.glEnable(3042);
          GlStateManager.blendFunc(770, 771);
          GlStateManager.color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
          GlStateManager.glLineWidth(width.getAsFloat());
		  event.renderItem.renderItem(event.stack, event.transformedModel);
          GL11.glPopAttrib();
          GL11.glPopMatrix();
		  
	  }
	  
	  //Renders the actual entity last
	  if (!this.ontop.getAsBoolean()) {
		  event.renderItem.renderItem(event.stack, event.transformedModel);
      }
      try {
          MC.gameSettings.fancyGraphics = fancyGraphics;
          MC.gameSettings.gammaSetting = gamma;
      }
      catch (Exception exception) {
          // empty catch block
      }
      event.setCanceled(true);  }
  
  @SubscribeEvent
  public void onRenderItem2d(RenderEntityItem2dEvent event) {
	  
    Color c;
    if (rainbow.get()) c = RainbowService.getRainbowColorClass();
    else c = color.get();

	  boolean fancyGraphics = MC.gameSettings.fancyGraphics; //Dont need fancy
	  MC.gameSettings.fancyGraphics = false;
	  float gamma = MC.gameSettings.gammaSetting;
	  MC.gameSettings.gammaSetting = 10000.0f; //For extreme lines
		
	  //Renders the actual entity first
	  if (this.ontop.getAsBoolean()) {
		  event.renderItem.renderItem(event.stack, event.transformedModel);
	  }
		
	  if(type.get() == TYPE.OUTLINE) {
		  
		  RenderUtils.renderOne(width.getAsFloat());
		  event.renderItem.renderItem(event.stack, event.transformedModel);
		  GlStateManager.glLineWidth(width.getAsFloat());
	      
	      RenderUtils.renderTwo();
		  event.renderItem.renderItem(event.stack, event.transformedModel);
	      GlStateManager.glLineWidth(width.getAsFloat());
	      
	      RenderUtils.renderThree();
	      RenderUtils.renderFour(c);
		  event.renderItem.renderItem(event.stack, event.transformedModel);
	      GlStateManager.glLineWidth(width.getAsFloat());
	     
	      RenderUtils.renderFive();
		  
	  } else {
		  
		  GL11.glPushMatrix();
          GL11.glPushAttrib(1048575);
          GL11.glPolygonMode(1032, 6913);
          
          GL11.glDisable(3553);
          GL11.glDisable(2896);
          GL11.glDisable(2929);
          GL11.glEnable(2848);
          GL11.glEnable(3042);
          GlStateManager.blendFunc(770, 771);
          GlStateManager.color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
          GlStateManager.glLineWidth(width.getAsFloat());
		  event.renderItem.renderItem(event.stack, event.transformedModel);
          GL11.glPopAttrib();
          GL11.glPopMatrix();
		  
	  }
	  
	  //Renders the actual entity last
	  if (!this.ontop.getAsBoolean()) {
		  event.renderItem.renderItem(event.stack, event.transformedModel);
      }
      try {
          MC.gameSettings.fancyGraphics = fancyGraphics;
          MC.gameSettings.gammaSetting = gamma;
      }
      catch (Exception exception) {
          // empty catch block
      }
      event.setCanceled(true); 
  }
  
  
  @SubscribeEvent
  public void onRender2D(final Render2DEvent event) {
	  
	  if(!nametags.getAsBoolean()) {
		  return;
	  }
	  
    GlStateManager.enableBlend();
    GlStateManager.tryBlendFuncSeparate(
        GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
        GlStateManager.SourceFactor.ONE,
        GlStateManager.DestFactor.ZERO);
    GlStateManager.enableTexture2D();
    GlStateManager.disableDepth();

    final double scale = this.scale.get() == 0 ? 1.D : this.scale.get();

    MC.world
        .loadedEntityList
        .stream()
        .filter(EntityItem.class::isInstance)
        .map(EntityItem.class::cast)
        .filter(entity -> entity.ticksExisted > 1)
        .forEach(
            entity -> {
              Vec3d bottomPos = EntityUtils.getInterpolatedPos(entity, event.getPartialTicks());
              Vec3d topPos =
                  bottomPos.addVector(0.D, entity.getRenderBoundingBox().maxY - entity.posY, 0.D);

              Plane top = VectorUtils.toScreen(topPos);
              Plane bot = VectorUtils.toScreen(bottomPos);

              if (!top.isVisible() && !bot.isVisible()) {
                return;
              }

              double offX = bot.getX() - top.getX();
              double offY = bot.getY() - top.getY();

              GlStateManager.pushMatrix();
              GlStateManager.translate(top.getX() - (offX / 2.D), bot.getY(), 0);

              ItemStack stack = entity.getItem();
              String text =
                  stack.getDisplayName() + (stack.isStackable() ? (" x" + stack.getCount()) : "");

              if (age.get()) {
                text += String.format(" [%d]", entity.getAge()/TICKS_SECOND);
              }

              SurfaceHelper.drawTextShadow(
                  text,
                  (int) (offX / 2.D - SurfaceHelper.getTextWidth(text, scale) / 2.D),
                  -(int) (offY - SurfaceHelper.getTextHeight(scale) / 2.D) - 1,
                  Colors.WHITE.toBuffer(),
                  scale);

              GlStateManager.popMatrix();
            });

    GlStateManager.enableDepth();
    GlStateManager.disableBlend();
  }
  
  enum TYPE {
	  OUTLINE, WIREFRAME
  }

}
