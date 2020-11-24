package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.util.draw.SurfaceHelper.drawOutlinedRect;

import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.mods.managers.FriendManager;
import com.matt.forgehax.mods.services.RainbowService;
import com.matt.forgehax.asm.events.RenderEntityModelEvent;
import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.mobtypes.MobTypeEnum;
import com.matt.forgehax.util.math.Plane;
import com.matt.forgehax.util.math.VectorUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.draw.RenderUtils;
import java.util.Objects;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class EntityESP extends ToggleMod {

  public enum ESPMode {
    BOX,
    SQUARE,
    OUTLINE,
    WIREFRAME
  }

  public final Setting<ESPMode> mode =
    getCommandStub()
      .builders()
      .<ESPMode>newSettingEnumBuilder()
      .name("mode")
      .description("Rendering mode [box/square/outline/wireframe]")
      .defaultTo(ESPMode.BOX)
      .build();

  public final Setting<Boolean> players =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("players")
      .description("Enables players")
      .defaultTo(true)
      .build();

  public final Setting<Boolean> mobs_hostile =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("hostile")
      .description("Enables hostile mobs")
      .defaultTo(true)
      .build();

  public final Setting<Boolean> mobs_friendly =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("friendly")
      .description("Enables friendly mobs")
      .defaultTo(true)
      .build();
  
  public final Setting<Boolean> ontop =
		  getCommandStub()
		  	.builders()
		  	.<Boolean>newSettingBuilder()
		  	.name("ontop")
		  	.description("Renders the esp over actual entity (only for wireframe)")
		  	.defaultTo(true)
		  	.build();

  public final Setting<Float> linewidth =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("width")
      .description("Line width")
      .defaultTo(2.0F)
      .min(0f)
      .max(10f)
      .build();

  private final Setting<Color> color_player =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-player")
      .description("Color for players")
      .defaultTo(Color.of(191, 97, 106, 230))
      .build();
  private final Setting<Color> color_friend =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-friend")
      .description("Color for friended players")
      .defaultTo(Color.of(0, 170, 170, 230))
      .build();
  private final Setting<Color> color_neutral =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-neutral")
      .description("Color for neutral mobs")
      .defaultTo(Color.of(128, 128, 128, 150))
      .build();
  private final Setting<Color> color_hostile =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-hostile")
      .description("Color for hostile mobs")
      .defaultTo(Color.of(200, 200, 200, 200))
      .build();

//omg I'm not just a comment, I'm a whole commit
  private final Setting<Boolean> rainbow =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("rainbow")
      .description("epilepsy for tdjj")
      .defaultTo(false)
      .build();

  public EntityESP() {
    super(Category.RENDER, "EntityESP", false, "Highlight entities in various modes");
  }

  /*TheAlphaEpsilon*/
  @SubscribeEvent
  public void onRenderModel(RenderEntityModelEvent event) {
      if (event.entity == null || (mode.get() != ESPMode.OUTLINE && mode.get() != ESPMode.WIREFRAME)) return;
      if (!players.get().booleanValue()) {
          if (event.entity instanceof EntityPlayer) return;
      }
      if (!mobs_friendly.get().booleanValue()) {
          if (EntityUtils.getRelationship(event.entity) == MobTypeEnum.FRIENDLY ||
        		  EntityUtils.getRelationship(event.entity) == MobTypeEnum.NEUTRAL) {
        	  return;
          }
      }
      if (!this.mobs_hostile.get().booleanValue()) {
    	  if(EntityUtils.getRelationship(event.entity) == MobTypeEnum.HOSTILE) {
    	  	return;
      	}
      }
      
      //Store and set mc values
      Color color;
      if (rainbow.get()) color = RainbowService.getRainbowColorClass();
      else {
        switch (EntityUtils.getRelationship(event.entity)) {
        case PLAYER:
            if (FriendManager.isFriendly(event.entity.getName()))
              color = color_friend.get();
            else color = color_player.get();
          break;
        case HOSTILE:
            color = color_hostile.get();
          break;
        default:
            color = color_neutral.get();
          break;
        }
      }
      boolean fancyGraphics = MC.gameSettings.fancyGraphics;
      MC.gameSettings.fancyGraphics = false;
      float gamma = MC.gameSettings.gammaSetting;
      MC.gameSettings.gammaSetting = 10000.0f; //For extreme lines
      
      //Renders the actual entity first
      if (this.ontop.getAsBoolean()) {
          event.modelBase.render(event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale);
      }
      
      if (this.mode.get() == ESPMode.OUTLINE) {
    	  
          RenderUtils.renderOne(linewidth.getAsFloat());
          event.modelBase.render(event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale);
          GlStateManager.glLineWidth(linewidth.getAsFloat());
          
          RenderUtils.renderTwo();
          event.modelBase.render(event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale);
          GlStateManager.glLineWidth(linewidth.getAsFloat());
          
          RenderUtils.renderThree();
          
          RenderUtils.renderFour(color);
          event.modelBase.render(event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale);
          GlStateManager.glLineWidth(linewidth.getAsFloat());
         
          RenderUtils.renderFive();
          
      } else {
          GL11.glPushMatrix();
          GL11.glPushAttrib(1048575);
          if (mode.get() == ESPMode.WIREFRAME) {
              GL11.glPolygonMode(1032, 6913);
          } else {
              GL11.glPolygonMode(1028, 6913);
          }
          GL11.glDisable(3553);
          GL11.glDisable(2896);
          GL11.glDisable(2929);
          GL11.glEnable(2848);
          GL11.glEnable(3042);
          GlStateManager.blendFunc(770, 771);
          GlStateManager.color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
          GlStateManager.glLineWidth(linewidth.getAsFloat());
          event.modelBase.render(event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale);
          GL11.glPopAttrib();
          GL11.glPopMatrix();
      }
      
      //Renders the actual entity last
      if (!this.ontop.getAsBoolean()) {
          event.modelBase.render(event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale);
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
  
  /*************************************/
  
  @SubscribeEvent(priority = EventPriority.LOW)
  public void onRender2D(final Render2DEvent event) {
    if (mode.get() != ESPMode.SQUARE || getWorld() == null) return;

    getWorld()
      .loadedEntityList
      .stream()
      .filter(EntityUtils::isLiving)
      .filter(entity -> !Objects.equals(getLocalPlayer(), entity))
      .filter(EntityUtils::isAlive)
      .filter(EntityUtils::isValidEntity)
      .map(entity -> (EntityLivingBase) entity)
      .forEach(
        living -> {
          int color;
          if (rainbow.get()) color = RainbowService.getRainbowColor();
          else {
            switch (EntityUtils.getRelationship(living)) {
              case PLAYER:
                  if (!players.get()) return;
                  if (FriendManager.isFriendly(living.getName()))
                    color = color_friend.get().toBuffer();
                  else color = color_player.get().toBuffer();
                break;
              case HOSTILE:
                  if (!mobs_hostile.get()) return;
                  color = color_hostile.get().toBuffer();
                break;
              case NEUTRAL:
              case FRIENDLY:
                  if (!mobs_friendly.get()) return;
                  color = color_neutral.get().toBuffer();
                break;
              default:
                color = Colors.BLACK.toBuffer();
            }
          }

          Vec3d bottomPos = EntityUtils.getInterpolatedPos(living, event.getPartialTicks());
          Vec3d topPos =
            bottomPos.addVector(0.D, living.getRenderBoundingBox().maxY - living.posY, 0.D);

          Plane top = VectorUtils.toScreen(topPos);
          Plane bot = VectorUtils.toScreen(bottomPos);

          double topX = top.getX();
          double topY = top.getY() + 1.D;
          double botX = bot.getX();
          double botY = bot.getY() + 1.D;
          double height = (bot.getY() - top.getY());
          double width = height;

          drawOutlinedRect((int) (topX - (width/2)), (int) topY, (int) width, (int) height, color, linewidth.get());
        });
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onRender(final RenderEvent event) {
    if (mode.get() != ESPMode.BOX || getWorld() == null) return;

    getWorld()
      .loadedEntityList
      .stream()
      .filter(EntityUtils::isLiving)
      .filter(entity -> !Objects.equals(getLocalPlayer(), entity))
      .filter(EntityUtils::isAlive)
      .filter(EntityUtils::isValidEntity)
      .map(entity -> (EntityLivingBase) entity)
      .forEach(
        living -> {
          int color;
          if (rainbow.get()) color = RainbowService.getRainbowColor();
          else {
            switch (EntityUtils.getRelationship(living)) {
              case PLAYER:
                  if (!players.get()) return;
                  if (FriendManager.isFriendly(living.getName()))
                    color = color_friend.get().toBuffer();
                  else color = color_player.get().toBuffer();
                break;
              case HOSTILE:
                  if (!mobs_hostile.get()) return;
                  color = color_hostile.get().toBuffer();
                break;
              case NEUTRAL:
              case FRIENDLY:
                  if (!mobs_friendly.get()) return;
                  color = color_neutral.get().toBuffer();
                break;
              default:
                  color = Colors.BLACK.toBuffer();
            }
          }
          //TODO: This is not interpolated!!! that is why it is so jittery!
          AxisAlignedBB bb = living.getEntityBoundingBox();
          Vec3d minVec = new Vec3d(bb.minX, bb.minY, bb.minZ);
          Vec3d maxVec = new Vec3d(bb.maxX, bb.maxY, bb.maxZ);

          RenderUtils.drawBox(minVec, maxVec, color, linewidth.get(), true);
        });
  }
}
