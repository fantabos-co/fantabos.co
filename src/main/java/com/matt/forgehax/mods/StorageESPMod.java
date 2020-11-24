package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryMasks;
import com.matt.forgehax.util.tesselation.GeometryTessellator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.item.EntityMinecartHopper;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.lwjgl.opengl.GL11;

/**
 * Created on 9/4/2016 by fr1kin
 * Updated by OverFloyd, may 2020
 */
@RegisterMod
public class StorageESPMod extends ToggleMod {

  public final Setting<Boolean> outline =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("outline")
          .description("Renders an outline around the block")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> fill =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("fill")
          .description("Renders a box on the block")
          .defaultTo(true)
          .build();

  public final Setting<Integer> alpha =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("alpha")
          .description("Alpha value for fill mode")
          .defaultTo(64)
          .min(0)
          .max(255)
          .build();

  private final Setting<Float> width =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("width")
          .description("The width value for the outline")
          .min(0f)
          .max(10f)
          .defaultTo(1.0f)
          .build();

  public final Setting<Boolean> antialias =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("antialias")
          .description("Makes lines and triangles more smooth, may hurt performance")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> chests =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("chests")
          .description("Show chests")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> dispensers =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("dispensers")
          .description("Show dispensers and droppers (parent)")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> shulkers =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("shulkers")
          .description("Show shulker boxes")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> eChests =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("ender")
          .description("Show ender chests")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> furnaces =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("furnaces")
          .description("Show furnaces")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> hoppers =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("hoppers")
          .description("Show hoppers")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> itemFrames =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("item-frames")
          .description("Show item frames")
          .defaultTo(true)
          .build();

  public final Setting<Integer> stash_warn =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("stash-warn")
          .description("Number of chests needed for warning, set 0 for always on, a very large number for always off")
          .min(0)
          .max(1000)
          .defaultTo(20)
          .build();

  private final Setting<Color> color_chest =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-chest")
      .description("Color for chests")
      .defaultTo(Color.of(255, 128, 0, 64))
      .build();
  private final Setting<Color> color_dispenser =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-dispenser")
      .description("Color for dispensers")
      .defaultTo(Color.of(0, 255, 0, 64))
      .build();
  private final Setting<Color> color_dropper =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-dropper")
      .description("Color for droppers")
      .defaultTo(Color.of(0, 170, 0, 64))
      .build();
  private final Setting<Color> color_shulker =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-shulker")
      .description("Color for shulker boxes")
      .defaultTo(Color.of(0, 170, 170, 64))
      .build();
  private final Setting<Color> color_echest =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-echest")
      .description("Color for ender chests")
      .defaultTo(Color.of(163, 73, 163, 64))
      .build();
  private final Setting<Color> color_furnace =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-furnace")
      .description("Color for furnaces")
      .defaultTo(Color.of(128, 128, 128, 64))
      .build();
  private final Setting<Color> color_hopper =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-hopper")
      .description("Color for hoppers")
      .defaultTo(Color.of(255, 255, 85, 64))
      .build();
  private final Setting<Color> color_frame =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-frame")
      .description("Color for item frames")
      .defaultTo(Color.of(153, 102, 51, 64))
      .build();

  public StorageESPMod() {
    super(Category.RENDER, "StorageESP", false, "Shows storage blocks/entities");
  }

  private int count = 0;
  private int count_chests = 0;

  @Override
  public String getDisplayText() {
    return (getModName() + " [" + TextFormatting.GOLD + count + TextFormatting.RESET + "]");
  }

  @Override
  public String getInfoDisplayText() {
    if (count_chests >= stash_warn.get())
      return TextFormatting.GOLD + "Chests: " + count_chests + TextFormatting.WHITE;
    return super.getInfoDisplayText();
  }

  private int getTileEntityOutlineColor(TileEntity tileEntity) {
    return Color.of(getTileEntityFillColor(tileEntity)).setAlpha(255).toBuffer();
  }

  private int getTileEntityFillColor(TileEntity tileEntity) {
    if (chests.getAsBoolean() && tileEntity instanceof TileEntityChest) {
      return color_chest.get().toBuffer();
    } else if (dispensers.getAsBoolean() && tileEntity instanceof TileEntityDispenser) {
      if (tileEntity instanceof TileEntityDropper) {
        return color_dropper.get().toBuffer();
      }
      return color_dispenser.get().toBuffer();
    } else if (shulkers.getAsBoolean() && tileEntity instanceof TileEntityShulkerBox) {
      return color_shulker.get().toBuffer();
    } else if (eChests.getAsBoolean() && tileEntity instanceof TileEntityEnderChest) {
      return color_echest.get().toBuffer();
    } else if (furnaces.getAsBoolean() && tileEntity instanceof TileEntityFurnace) {
      return color_furnace.get().toBuffer();
    } else if (hoppers.getAsBoolean() && tileEntity instanceof TileEntityHopper) {
      return color_hopper.get().toBuffer();
    } else return -1;
  }

  private int getEntityOutlineColor(Entity entity) {
    return Color.of(getEntityFillColor(entity)).setAlpha(255).toBuffer();
  }

  private int getEntityFillColor(Entity entity) {
    if (chests.getAsBoolean() && entity instanceof EntityMinecartChest)
      return color_chest.get().toBuffer();
    else if (hoppers.getAsBoolean() && entity instanceof EntityMinecartHopper)
      return color_hopper.get().toBuffer();
    else if (itemFrames.getAsBoolean() && entity instanceof EntityItemFrame
        && ((EntityItemFrame) entity).getDisplayedItem().getItem() instanceof ItemShulkerBox)
      return color_shulker.get().toBuffer();
    else if (itemFrames.getAsBoolean() && entity instanceof EntityItemFrame
        && !(((EntityItemFrame) entity).getDisplayedItem().getItem() instanceof ItemShulkerBox))
      return color_frame.get().toBuffer();
    else return -1;
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onRender(final RenderEvent event) {
    if(MC.gameSettings.hideGUI || getWorld() == null) {
      return;
    }

    if (antialias.get()) {
      GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }

    int chests = 0;
    int total = 0;
    for (TileEntity tileEntity : getWorld().loadedTileEntityList) {
      if (tileEntity instanceof TileEntityChest) chests++;
      total++;
      BlockPos pos = tileEntity.getPos();

      int outlineColor = getTileEntityOutlineColor(tileEntity);
      int fillColor = getTileEntityFillColor(tileEntity);
      if (outlineColor != -1 && fillColor != -1) {
        if (outline.get()) {
          GlStateManager.glLineWidth(width.get());
          event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Line.ALL, outlineColor);
          event.getTessellator().draw();
        }

        if (fill.get()) {
          event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Quad.ALL, fillColor);
          event.getTessellator().draw();
        }
      }
    }
    count = total;
    count_chests = chests;

    for (Entity entity : getWorld().loadedEntityList) {
      BlockPos pos = entity.getPosition();
      int outlineColor = getEntityOutlineColor(entity);
      int fillColor = getEntityFillColor(entity);

      if (outlineColor != -1 && fillColor != -1) {
        if (outline.get()) {
          GlStateManager.glLineWidth(width.get());
          event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(
              event.getBuffer(),
              entity instanceof EntityItemFrame ? pos.add(0, -1, 0) : pos,
              GeometryMasks.Line.ALL,
              outlineColor);
          event.getTessellator().draw();
        }

        if (fill.get()) {
          event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(
              event.getBuffer(),
              entity instanceof EntityItemFrame ? pos.add(0, -1, 0) : pos,
              GeometryMasks.Quad.ALL,
              fillColor);
          event.getTessellator().draw();
        }
      }
    }
    if (antialias.get()) {
      GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }
    GlStateManager.glLineWidth(1.0f);
  }
}
