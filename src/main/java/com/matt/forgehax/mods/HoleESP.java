package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.mods.services.HoleService;
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
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.opengl.GL11;

@RegisterMod
public class HoleESP extends ToggleMod {

  public HoleESP() {
    super(Category.RENDER, "HoleESP", false, "Highlights holes");
  }

  public final Setting<Boolean> outline =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("outline")
      .description("Renders an outline around the block")
      .defaultTo(false)
      .build();

  public final Setting<Boolean> fill =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("fill")
      .description("Renders a box on the block")
      .defaultTo(true)
      .build();

  public final Setting<Integer> alpha_safe =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("s-alpha")
      .description("Alpha value for safe hole in fill mode")
      .min(0)
      .max(255)
      .defaultTo(128)
      .build();
  private final Setting<Integer> red_safe =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("s-red")
      .description("Red amount for safe hole, 0-255")
      .min(0)
      .max(255)
      .defaultTo(191)
      .build();
  private final Setting<Integer> green_safe =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("s-green")
      .description("Green amount for safe hole, 0-255")
      .min(0)
      .max(255)
      .defaultTo(97)
      .build();
  private final Setting<Integer> blue_safe =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("s-blue")
      .description("Blue amount for safe hole, 0-255")
      .min(0)
      .max(255)
      .defaultTo(106)
      .build();

  public final Setting<Double> height_safe =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("s-height")
      .description("Height of the highlighted safe hole")
      .defaultTo(0.5D)
      .min(0.01D)
      .max(1D)
      .build();

  public final Setting<Integer> alpha_temp =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("t-alpha")
      .description("Alpha value for temporary holes in fill mode")
      .min(0)
      .max(255)
      .defaultTo(64)
      .build();
  private final Setting<Integer> red_temp =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("t-red")
      .description("Red amount for temporary hole, 0-255")
      .min(0)
      .max(255)
      .defaultTo(191)
      .build();
  private final Setting<Integer> green_temp =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("t-green")
      .description("Green amount for temporary hole, 0-255")
      .min(0)
      .max(255)
      .defaultTo(97)
      .build();
  private final Setting<Integer> blue_temp =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("t-blue")
      .description("Blue amount for temporary hole, 0-255")
      .min(0)
      .max(255)
      .defaultTo(106)
      .build();

  public final Setting<Double> height_temp =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("t-height")
      .description("Height of the highlighted temporary hole")
      .defaultTo(0.2D)
      .min(0.01D)
      .max(1D)
      .build();

  public final Setting<Integer> alpha_void =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("v-alpha")
      .description("Alpha value for void in fill mode")
      .min(0)
      .max(255)
      .defaultTo(255)
      .build();
  private final Setting<Integer> red_void =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("v-red")
      .description("Red amount for void, 0-255")
      .min(0)
      .max(255)
      .defaultTo(0)
      .build();
  private final Setting<Integer> green_void =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("v-green")
      .description("Green amount for void, 0-255")
      .min(0)
      .max(255)
      .defaultTo(0)
      .build();
  private final Setting<Integer> blue_void =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("v-blue")
      .description("Blue amount for void, 0-255")
      .min(0)
      .max(255)
      .defaultTo(0)
      .build();

  public final Setting<Double> height_void =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("v-height")
      .description("Height of the highlighted void block")
      .defaultTo(0.01D)
      .min(0.01D)
      .max(1D)
      .build();

  private final Setting<Float> width =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("width")
      .description("The width value for the outline")
      .min(0.5f)
      .defaultTo(1.0f)
      .build();

  public final Setting<Boolean> antialias =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("antialias")
      .description("Makes lines and triangles more smooth, may hurt performance")
      .defaultTo(false)
      .build();

  public final Setting<Boolean> void_esp =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("voids")
      .description("Also highlight void holes")
      .defaultTo(true)
      .build();

  @SubscribeEvent
  public void onRender(RenderEvent event) {
    if(MC.gameSettings.hideGUI || getWorld() == null) {
      return;
    }

    if (antialias.get()) {
      GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }
    
    for (BlockPos pos : HoleService.safe_holes) {
      if (outline.get()) {
        GlStateManager.glLineWidth(width.get());
        event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(
          event.getBuffer(), pos, GeometryMasks.Line.ALL,
          Color.of(red_safe.get(), green_safe.get(), blue_safe.get(), alpha_safe.get()).toBuffer(), height_safe.get());
  
        event.getTessellator().draw();
      }
  
      if (fill.get()) {
        event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(
          event.getBuffer(), pos, GeometryMasks.Quad.ALL,
          Color.of(red_safe.get(), green_safe.get(), blue_safe.get(), alpha_safe.get()).toBuffer(), height_safe.get());
  
        event.getTessellator().draw();
      }
    }

    for (BlockPos pos : HoleService.temp_holes) {
      if (outline.get()) {
        GlStateManager.glLineWidth(width.get());
        event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(
          event.getBuffer(), pos, GeometryMasks.Line.ALL,
          Color.of(red_temp.get(), green_temp.get(), blue_temp.get(), alpha_temp.get()).toBuffer(), height_temp.get());
  
        event.getTessellator().draw();
      }
  
      if (fill.get()) {
        event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(
          event.getBuffer(), pos, GeometryMasks.Quad.ALL,
          Color.of(red_temp.get(), green_temp.get(), blue_temp.get(), alpha_temp.get()).toBuffer(), height_temp.get());
  
        event.getTessellator().draw();
      }
    }

    if (void_esp.get()) {
      for (BlockPos pos : HoleService.voids) {
        if (outline.get()) {
          GlStateManager.glLineWidth(width.get());
          event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(
            event.getBuffer(), pos, GeometryMasks.Line.ALL,
            Color.of(red_void.get(), green_void.get(), blue_void.get(), alpha_void.get()).toBuffer(), height_void.get());
          event.getTessellator().draw();
        }
        
        if (fill.get()) {
          event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(
            event.getBuffer(), pos, GeometryMasks.Quad.ALL, 
            Color.of(red_void.get(), green_void.get(), blue_void.get(), alpha_void.get()).toBuffer(), height_void.get());
          event.getTessellator().draw();
        }
      }
    }

    if (antialias.get()) {
      GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    if (outline.get()) GlStateManager.glLineWidth(1.0f);
  }
}
