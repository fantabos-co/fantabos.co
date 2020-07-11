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
      .max(120)
      .build();
  private final Setting<Integer> red =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("red")
      .description("Red amount, 0-255")
      .min(0)
      .max(255)
      .defaultTo(191)
      .build();
  private final Setting<Integer> green =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("green")
      .description("Green amount, 0-255")
      .min(0)
      .max(255)
      .defaultTo(97)
      .build();
  private final Setting<Integer> blue =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("blue")
      .description("Blue amount, 0-255")
      .min(0)
      .max(255)
      .defaultTo(106)
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

  public final Setting<Double> height =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("height")
      .description("Height of the highlighted block")
      .defaultTo(0.5D)
      .min(0.1D)
      .max(1D)
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
    
    for (BlockPos pos : HoleService.holes) {
      if (outline.get()) {
        GlStateManager.glLineWidth(width.get());
        event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(
          event.getBuffer(), pos, GeometryMasks.Line.ALL,
          Color.of(red.get(), green.get(), blue.get(), alpha.get()).toBuffer(), height.get());
  
        event.getTessellator().draw();
      }
  
      if (fill.get()) {
        event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(
          event.getBuffer(), pos, GeometryMasks.Quad.ALL,
          Color.of(red.get(), green.get(), blue.get(), alpha.get()).toBuffer(), height.get());
  
        event.getTessellator().draw();
      }
    }

    GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    GL11.glDisable(GL11.GL_LINE_SMOOTH);
    GlStateManager.glLineWidth(1.0f);
  }
}
