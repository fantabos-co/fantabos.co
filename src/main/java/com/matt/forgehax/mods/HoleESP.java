package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.mods.services.HoleService;
import com.matt.forgehax.util.color.Color;
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

  private final Setting<Color> color_safe =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-safe")
      .description("Color for all bedrock holes")
      .defaultTo(Color.of(191, 97, 106, 128))
      .build();
  private final Setting<Color> color_temp =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-temp")
      .description("Color for obsidian and mixed holes")
      .defaultTo(Color.of(191, 97, 106, 64))
      .build();
  private final Setting<Color> color_void =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-void")
      .description("Color for void holes")
      .defaultTo(Color.of(0, 0, 0, 200))
      .build();

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

  public final Setting<Double> height_safe =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("s-height")
      .description("Height of the highlighted safe hole")
      .defaultTo(0.5D)
      .min(0.01D)
      .max(5D)
      .build();

  public final Setting<Double> height_temp =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("t-height")
      .description("Height of the highlighted temporary hole")
      .defaultTo(0.2D)
      .min(0.01D)
      .max(5D)
      .build();

  public final Setting<Double> height_void =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("v-height")
      .description("Height of the highlighted void block")
      .defaultTo(0.01D)
      .min(0.01D)
      .max(256D)
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
          color_safe.get().toBuffer(), height_safe.get());
  
        event.getTessellator().draw();
      }
  
      if (fill.get()) {
        event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(
          event.getBuffer(), pos, GeometryMasks.Quad.ALL,
          color_safe.get().toBuffer(), height_safe.get());
  
        event.getTessellator().draw();
      }
    }

    for (BlockPos pos : HoleService.temp_holes) {
      if (outline.get()) {
        GlStateManager.glLineWidth(width.get());
        event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(
          event.getBuffer(), pos, GeometryMasks.Line.ALL,
          color_temp.get().toBuffer(), height_temp.get());
  
        event.getTessellator().draw();
      }
  
      if (fill.get()) {
        event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(
          event.getBuffer(), pos, GeometryMasks.Quad.ALL,
          color_temp.get().toBuffer(), height_temp.get());
  
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
            color_void.get().toBuffer(), height_void.get());
          event.getTessellator().draw();
        }
        
        if (fill.get()) {
          event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(
            event.getBuffer(), pos, GeometryMasks.Quad.ALL, 
            color_void.get().toBuffer(), height_void.get());
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
