package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getModManager;

import com.google.common.util.concurrent.AtomicDouble;
import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.mods.services.FriendService;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.ColorClamp;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceBuilder;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.draw.font.Fonts;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.math.Plane;
import com.matt.forgehax.util.math.VectorUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class ESP extends ToggleMod implements Fonts {
  
  private static final int HEALTHBAR_WIDTH = 50;
  private static final int HEALTHBAR_HEIGHT = 3;
  
  public enum DrawOptions {
    NONE,
    NAME,
    BAR,
    FULL,
  }
  
  public enum ArmorOptions {
    DISABLED,
    SIMPLE,
    ENCHANTMENTS
  }

  /* I got this off Dominika too because I did not want to convert the table below */
  public static class EnchantEntry {
    private Enchantment enchant;
    private String name;

    public EnchantEntry(Enchantment enchant, String name) {
      this.enchant = enchant;
      this.name = name;
    }

    public Enchantment getEnchant() {
      return this.enchant;
    }

    public String getName() {
      return this.name;
    }
  }

  /* Thanks dominika for this table already done! */
  public static EnchantEntry[] enchants = {
    new EnchantEntry(Enchantments.PROTECTION, "pro"),
    new EnchantEntry(Enchantments.THORNS, "thr"),
    new EnchantEntry(Enchantments.SHARPNESS, "sha"),
    new EnchantEntry(Enchantments.FIRE_ASPECT, "fia"),
    new EnchantEntry(Enchantments.KNOCKBACK, "knb"),
    new EnchantEntry(Enchantments.UNBREAKING, "unb"),
    new EnchantEntry(Enchantments.POWER, "pow"),
    new EnchantEntry(Enchantments.FIRE_PROTECTION, "fpr"),
    new EnchantEntry(Enchantments.FEATHER_FALLING, "fea"),
    new EnchantEntry(Enchantments.BLAST_PROTECTION, "bla"),
    new EnchantEntry(Enchantments.PROJECTILE_PROTECTION, "ppr"),
    new EnchantEntry(Enchantments.RESPIRATION, "res"),
    new EnchantEntry(Enchantments.AQUA_AFFINITY, "aqu"),
    new EnchantEntry(Enchantments.DEPTH_STRIDER, "dep"),
    new EnchantEntry(Enchantments.FROST_WALKER, "fro"),
    new EnchantEntry(Enchantments.BINDING_CURSE, "bin"),
    new EnchantEntry(Enchantments.SMITE, "smi"),
    new EnchantEntry(Enchantments.BANE_OF_ARTHROPODS, "ban"),
    new EnchantEntry(Enchantments.LOOTING, "loo"),
    new EnchantEntry(Enchantments.SWEEPING, "swe"),
    new EnchantEntry(Enchantments.EFFICIENCY, "eff"),
    new EnchantEntry(Enchantments.SILK_TOUCH, "sil"),
    new EnchantEntry(Enchantments.FORTUNE, "for"),
    new EnchantEntry(Enchantments.FLAME, "fla"),
    new EnchantEntry(Enchantments.LUCK_OF_THE_SEA, "luc"),
    new EnchantEntry(Enchantments.LURE, "lur"),
    new EnchantEntry(Enchantments.MENDING, "men"),
    new EnchantEntry(Enchantments.VANISHING_CURSE, "van"),
    new EnchantEntry(Enchantments.PUNCH, "pun")
  };
  
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

  public final Setting<Boolean> ench_display =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("enchants")
      .description("Draw enchants over armor")
      .defaultTo(false)
      .build();

  private final Setting<Boolean> percentage =
   getCommandStub()
       .builders()
       .<Boolean>newSettingBuilder()
       .name("percentage")
       .description("Show pleb % damage instead of chad absolute damage")
       .defaultTo(false)
       .build();
  
  public ESP() {
    super(Category.RENDER, "ESP", false, "Shows entity locations and info");
  }
  
  @SubscribeEvent
  public void onRenderPlayerNameTag(RenderLivingEvent.Specials.Pre event) {
    if (EntityUtils.isPlayer(event.getEntity())) {
      event.setCanceled(true);
    }
  }
  
  @SubscribeEvent(priority = EventPriority.LOW)
  public void onRender2D(final Render2DEvent event) {
    if (getWorld() == null) return;

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
                    height,
                    ench_display.get(),
                    percentage.get()));
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
      double height,
      boolean details,
      boolean percentage);
    
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
        double height,
        boolean details,
        boolean percentage) {
        List<ItemStack> items =
          StreamSupport.stream(living.getEquipmentAndArmor().spliterator(), false)
            .filter(Objects::nonNull)
            .filter(stack -> !stack.isEmpty())
            .collect(Collectors.toList());
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
              .itemTextOverlay(stack, xx, y, percentage)
              .task(SurfaceBuilder::disableItemRendering)
              .pop();
            if (details) {
              double ty = topY - itemSize;
              for (int i = 0; i < enchants.length; i++) {
                int level = EnchantmentHelper.getEnchantmentLevel(enchants[i].getEnchant(), stack); 
                String text = "";
                if (level > 0) {
                  ty -= 5.D;
                  if (level > 32000) text = String.format("%s 32k", enchants[i].getName());
                  else text = String.format("%s %d", enchants[i].getName(), level);
                  builder
                    .reset()
                    .push()
                    .task(SurfaceBuilder::enableBlend)
                    .task(SurfaceBuilder::enableFontRendering)
                    // .fontRenderer(ARIAL)
                    .color(Colors.WHITE.toBuffer())
                    .scale(0.5D)
                    .text(text, xx * 2.D, ty * 2.D)
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
        double height,
        boolean details,
        boolean percentage) {
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
        double height,
        boolean details,
        boolean percentage) {
        String text = living.getDisplayName().getUnformattedText();
        String f_text;

        double x = topX - ((double) builder.getFontWidth(text) / 2.D);
        double y = topY - (double) builder.getFontHeight() - 1.D;

        FriendService mod = getModManager().get(FriendService.class).get();
        if (mod != null && mod.isFriend(text))
          f_text = ColorClamp.getClampedColor(mod.getFriendColor(text)) + text;
        else f_text = text;

        if (living.isPotionActive(MobEffects.STRENGTH)) {
          f_text += " [!]";
        }
        
        builder
          .reset()
          .push()
          .task(SurfaceBuilder::enableBlend)
          .task(SurfaceBuilder::enableFontRendering)
          // .task(SurfaceBuilder::enableTexture2D) // enable texture
          // .fontRenderer(ARIAL)
          .color(Colors.BLACK.toBuffer())
          .text(text, x + 1, y + 1)
          .color(Colors.WHITE.toBuffer())
          .text(f_text, x, y)
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
}
