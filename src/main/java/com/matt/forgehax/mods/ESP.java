package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

import com.google.common.util.concurrent.AtomicDouble;
import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.mods.managers.FriendManager;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.ColorClamp;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceBuilder;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.draw.font.Fonts;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.PlayerUtils;
import com.matt.forgehax.util.entity.EnchantmentUtils;
import com.matt.forgehax.util.entity.EnchantmentUtils.EntityEnchantment;
import com.matt.forgehax.util.math.Plane;
import com.matt.forgehax.util.math.VectorUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.init.MobEffects;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class ESP extends ToggleMod implements Fonts {
  
  private static final int HEALTHBAR_WIDTH = 50;
  private static final int HEALTHBAR_HEIGHT = 3;
  
  public enum DrawOptions {
    NONE,
    NAME,
    BAR,
    INFO,
    FULL,
  }
  
  public enum ArmorOptions {
    DISABLED,
    SIMPLE,
    ENCHANTMENTS
  }

  // static settings allow to be accessed inside the various item draw() calls
  private static Setting<Boolean> ench_display;
  private static Setting<Boolean> percentage;
  private static Setting<Boolean> ping_name;
  private static Setting<Boolean> health_name;
  private static Setting<Boolean> gamemode_name;
  private static Setting<Boolean> distance_name;

  {
    ench_display =
      getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("enchants")
        .description("Draw enchants over armor")
        .defaultTo(true)
        .build();

    percentage =
      getCommandStub()
         .builders()
         .<Boolean>newSettingBuilder()
         .name("percentage")
         .description("Show pleb % damage instead of chad absolute damage")
         .defaultTo(false)
         .build();

    ping_name =
      getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("ping")
        .description("Show ping in names")
        .defaultTo(false)
        .build();

    health_name =
      getCommandStub()
         .builders()
         .<Boolean>newSettingBuilder()
         .name("health")
         .description("Show health of player in name")
         .defaultTo(false)
         .build();

    gamemode_name =
      getCommandStub()
         .builders()
         .<Boolean>newSettingBuilder()
         .name("gamemode")
         .description("Show gamemode of player in name")
         .defaultTo(false)
         .build();

    distance_name =
      getCommandStub()
         .builders()
         .<Boolean>newSettingBuilder()
         .name("distance")
         .description("Show distance from player in name")
         .defaultTo(false)
         .build();
  }
  
  public final Setting<DrawOptions> players =
    getCommandStub()
      .builders()
      .<DrawOptions>newSettingEnumBuilder()
      .name("players")
      .description("Enables players")
      .defaultTo(DrawOptions.FULL)
      .build();
  
  public final Setting<DrawOptions> mobs_hostile =
    getCommandStub()
      .builders()
      .<DrawOptions>newSettingEnumBuilder()
      .name("hostile")
      .description("Enables hostile mobs")
      .defaultTo(DrawOptions.NONE)
      .build();
  
  public final Setting<DrawOptions> mobs_friendly =
    getCommandStub()
      .builders()
      .<DrawOptions>newSettingEnumBuilder()
      .name("friendly")
      .description("Enables friendly mobs")
      .defaultTo(DrawOptions.NONE)
      .build();
  
  public final Setting<DrawOptions> mob_tameable =
    getCommandStub()
      .builders()
      .<DrawOptions>newSettingEnumBuilder()
      .name("tamed")
      .description("Specific rule for Tameable mobs")
      .defaultTo(DrawOptions.NONE)
      .build();
  private final Setting<Boolean> australia =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("australia")
      .description("Make all mobs citizens of the other side of the world")
      .defaultTo(false)
      .changed(cb -> {
        if (!cb.getTo() && getWorld() != null) { // disabled australia
          getWorld().loadedEntityList.forEach(e -> {
            if (e.hasCustomName() && e.getCustomNameTag().equals("Dinnerbone")) {
              e.setCustomNameTag(""); // revert changes, this may delete manually assigned Dinnerbone nametags clientside
            }
          });
        }
      })
      .build();

  public ESP() {
    super(Category.RENDER, "ESP", false, "Actually just nametags");
  }
  
  @SubscribeEvent
  public void onRenderPlayerNameTag(RenderLivingEvent.Specials.Pre event) {
    if (EntityUtils.isPlayer(event.getEntity())) {
      event.setCanceled(true);
    }
  }
  
  @SubscribeEvent
  public void onRender2D(final Render2DEvent event) {
    if (getWorld() == null) return;

    getWorld()
      .loadedEntityList
      .stream()
      .filter(EntityUtils::isLiving)
      .filter(entity -> !Objects.equals(MC.getRenderViewEntity(), entity))
      .filter(EntityUtils::isAlive)
      .filter(EntityUtils::isValidEntity)
      .map(entity -> (EntityLivingBase) entity)
      .forEach(
        living -> {
          if (australia.get() && !(living instanceof AbstractClientPlayer) && !living.hasCustomName()) {
            living.setCustomNameTag("Dinnerbone");
          }

          final Setting<DrawOptions> setting;
          if ((living instanceof EntityTameable && ((EntityTameable) living).isTamed()) ||
               living instanceof EntityHorse && ((EntityHorse) living).isTame()) {
            setting = mob_tameable;
          } else {
            switch (EntityUtils.getRelationship(living)) {
              case PLAYER:
                setting = players;
                break;
              case HOSTILE:
                setting = mobs_hostile;
                break;
              case NEUTRAL:
              case FRIENDLY:
                setting = mobs_friendly;
                break;
              default:
                setting = null;
                break;
            }
          }

          if (setting == null || DrawOptions.NONE.equals(setting.get())) {
            return;
          }
          
          Vec3d bottomPos = EntityUtils.getInterpolatedPos(living, event.getPartialTicks());
          Vec3d topPos =
            bottomPos.addVector(0.D, living.getRenderBoundingBox().maxY - living.posY, 0.D);
          
          Plane top = VectorUtils.toScreen(topPos);
          Plane bot = VectorUtils.toScreen(bottomPos);
          
          // stop here if neither are visible
          if (!top.isVisible() && !bot.isVisible()) {
            return;
          }
          
          double topX = top.getX();
          double topY = top.getY() + 1.D;
          double botX = bot.getX();
          double botY = bot.getY() + 1.D;
          double height = (bot.getY() - top.getY());
          double width = height;
          
          AtomicDouble offset = new AtomicDouble();
          TopComponents.REVERSE_VALUES
            .stream()
            .filter(comp -> comp.valid(setting))
            .forEach(
              comp -> {
                double os = offset.get();
                offset.set(
                  os
                    + comp.draw(
                    event.getSurfaceBuilder(),
                    living,
                    topX,
                    topY - os,
                    botX,
                    botY,
                    width,
                    height));
              });
        });
  }
  
  private interface IComponent {
    
    /**
     * Draw component
     *
     * @param living entity handle
     * @param topX top x
     * @param topY top y
     * @param botX bot x
     * @param botY bot y
     * @param width width
     * @param height height
     * @return y offset
     */
    double draw(
      SurfaceBuilder builder,
      EntityLivingBase living,
      double topX,
      double topY,
      double botX,
      double botY,
      double width,
      double height);
    
    /**
     * Check if the draw component is valid for this setting
     */
    boolean valid(Setting<DrawOptions> setting);
  }
  
  private enum TopComponents implements IComponent {
    ITEMS {
      @Override
      public double draw(
        SurfaceBuilder builder,
        EntityLivingBase living,
        double topX,
        double topY,
        double botX,
        double botY,
        double width,
        double height) {
        List<ItemStack> items = new ArrayList<>();
        if (!living.getHeldItemOffhand().isEmpty())
          items.add(living.getHeldItemOffhand());
        for (ItemStack e : living.getArmorInventoryList())
          if (!e.isEmpty())
            items.add(e);
        if (!living.getHeldItemMainhand().isEmpty())
          items.add(living.getHeldItemMainhand());
        Collections.reverse(items);

        if (!items.isEmpty()) { // only continue if there are elements present
          final double itemSize = 16;
          double x = topX - ((itemSize * (double) items.size()) / 2.D);
          double y = topY - itemSize;
          for (int index = 0; index < items.size(); ++index) {
            ItemStack stack = items.get(index);
            double xx = x + (index * itemSize);
            builder
              .reset()
              .push()
              .task(SurfaceBuilder::clearColor)
              .task(SurfaceBuilder::enableItemRendering)
              .item(stack, xx, y)
              .itemTextOverlay(stack, xx, y, percentage.get())
              .task(SurfaceBuilder::disableItemRendering)
              .pop();
            if (ench_display.get()) {
              double ty = topY - 5.D;
              for (EntityEnchantment e : EnchantmentUtils.getEnchantmentsSorted(stack.getEnchantmentTagList())) {
                if (e.getLevel() > 0) {
                  ty -= 5.D;
                  builder
                    .reset()
                    .push()
                    .task(SurfaceBuilder::enableBlend)
                    .task(SurfaceBuilder::enableFontRendering)
                    .color(Colors.WHITE.toBuffer())
                    .scale(0.5D)
                    .text(e.getShortName(), xx * 2.D + 2, ty * 2.D)
                    .task(SurfaceBuilder::disableBlend)
                    .task(SurfaceBuilder::disableFontRendering)
                    .pop();
                }
              }
            }
          }
          return itemSize + 1.D;
        } else {
          return 0.D;
        }
      }
      
      @Override
      public boolean valid(Setting<DrawOptions> setting) {
        return DrawOptions.FULL.compareTo(setting.get())
          <= 0; // ADVANCED less than or equal to SETTING
      }
    },
    HEALTH {
      @Override
      public double draw(
        SurfaceBuilder builder,
        EntityLivingBase living,
        double topX,
        double topY,
        double botX,
        double botY,
        double width,
        double height) {
        float hp =
          MathHelper.clamp(living.getHealth(), 0, living.getMaxHealth()) / living.getMaxHealth();
        double x = topX - (HEALTHBAR_WIDTH / 2);
        double y = topY - HEALTHBAR_HEIGHT - 2;
        int color =
          (living.getHealth() + living.getAbsorptionAmount() > living.getMaxHealth())
            ? Colors.YELLOW.toBuffer()
            : Color.of(
              (int) ((255 - hp) * 255),
              (int) (255 * hp),
              0,
              255).toBuffer(); // if above 20 hp bar is yellow
        
        builder
          .reset() // clean up from previous uses
          .push()
          .task(SurfaceBuilder::enableBlend)
          .task(SurfaceBuilder::disableTexture2D)
          .beginQuads()
          .color(Colors.BLACK.toBuffer())
          .rectangle(x, y, HEALTHBAR_WIDTH, HEALTHBAR_HEIGHT)
          .end()
          .reset()
          .beginQuads()
          .color(color)
          .rectangle(
            x + 1.D, y + 1.D, ((double) HEALTHBAR_WIDTH - 2.D) * hp, HEALTHBAR_HEIGHT - 2.D)
          .end()
          .task(SurfaceBuilder::disableBlend)
          .task(SurfaceBuilder::enableTexture2D)
          .pop();
        
        return HEALTHBAR_HEIGHT + 3.D;
      }
      
      @Override
      public boolean valid(Setting<DrawOptions> setting) {
        return DrawOptions.BAR.compareTo(setting.get())
          <= 0; // SIMPLE less than or equal to SETTING
      }
    },
    NAME {
      @Override
      public double draw(
        SurfaceBuilder builder,
        EntityLivingBase living,
        double topX,
        double topY,
        double botX,
        double botY,
        double width,
        double height) {
        StringBuilder text = new StringBuilder();

        String name = living.getDisplayName().getUnformattedText();

        if (distance_name.get())
          text.append(PlayerUtils.getDistanceColor(living)).append(" ");

        if (health_name.get())
          text.append(PlayerUtils.getHPColor(living)).append(" ");

        if (FriendManager.isFriend(name))
          text.append(ColorClamp.getClampedColor(FriendManager.getFriendColor(name)) + name + TextFormatting.RESET + " ");
        else text.append(name + " ");

        if (living instanceof EntityPlayer) {
          EntityPlayer p = (EntityPlayer) living;
          if (gamemode_name.get())
            text.append(PlayerUtils.getGmode(p) + " ");
  
          if (ping_name.get())
            text.append(PlayerUtils.getColorPing(p));

          if (living.isPotionActive(MobEffects.STRENGTH))
            text.append(" [!]");
        }

        double w = (double) builder.getFontWidth(text.toString());
        double h = (double) builder.getFontHeight();
        double x = topX -  (w / 2.D);
        double y = topY - h - 1.D;

        String raw_text = TextFormatting.getTextWithoutFormattingCodes(text.toString());
        
        builder
          .reset()
          .push()
          .task(SurfaceBuilder::enableBlend)
          .task(SurfaceBuilder::disableTexture2D)
          .beginQuads()
          .color(Color.of(0, 0, 0, 100).toBuffer())
          .rectangle(x - 2, y - 2, w + 3, h + 3)
          .end()
          .beginQuads()
          .color(Color.of(50, 50, 50, 50).toBuffer())
          .rectangle(x - 1, y - 1, w + 1, h + 1)
          .end()
          .task(SurfaceBuilder::enableTexture2D)
          .task(SurfaceBuilder::enableFontRendering)
          .color(Colors.BLACK.toBuffer())
          .text(raw_text, x + 1, y + 1)
          .color(Colors.WHITE.toBuffer())
          .text(text.toString(), x, y)
          .task(SurfaceBuilder::disableBlend)
          .task(SurfaceBuilder::disableFontRendering)
          .pop();
        
        return SurfaceHelper.getTextHeight();
      }
      
      @Override
      public boolean valid(Setting<DrawOptions> setting) {
        return DrawOptions.NONE.compareTo(setting.get()) < 0; // DISABLED less than SETTING
      }
    };
    
    static final List<TopComponents> REVERSE_VALUES = Arrays.asList(TopComponents.values());
    
    static {
      Collections.reverse(REVERSE_VALUES);
    }
  }

  private static String makePlayerString(EntityPlayer in) {
    StringBuilder out = new StringBuilder();
    if (distance_name.get()) out.append(PlayerUtils.getDistanceColor(in)).append(" ");
    if (gamemode_name.get()) out.append(PlayerUtils.getGmode(in)).append(" ");
    if (FriendManager.isFriend(in.getName()))
      out.append(ColorClamp.getClampedColor(FriendManager.getFriendColor(in.getName())) + in.getName() + TextFormatting.RESET + " ");
    else out.append(in.getName() + " ");
    if (health_name.get()) out.append(" [").append(PlayerUtils.getHPColor(in)).append("]");
    if (ping_name.get()) out.append(" ").append(PlayerUtils.getColorPing(in));
    return out.toString();
  }
}
