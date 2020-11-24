package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getLocalPlayer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryMasks;
import com.matt.forgehax.util.tesselation.GeometryTessellator;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.lwjgl.opengl.GL11;

/**
 * Created on 9/4/2016 by fr1kin
 * Updated by OverFloyd, may 2020
 * Tonio made this for liquids
 */
@RegisterMod
public class LiquidESP extends ToggleMod {

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
          .defaultTo(false)
          .build();

  private final Setting<Integer> alpha =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("alpha")
          .description("Transparency, 0-255")
          .min(0)
          .max(255)
          .defaultTo(80)
          .build();

  private final Setting<Float> width =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("width")
          .description("The width value for the outline in source blocks")
          .min(0f)
          .max(10f)
          .defaultTo(0.5f)
          .build();

  public final Setting<Boolean> antialias =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("antialias")
          .description("Makes lines and triangles more smooth, may hurt performance")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> sources_only =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("sources-only")
          .description("Draw only liquid sources")
          .defaultTo(false)
          .build();

  public final Setting<Integer> radius =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("range")
          .description("How many blocks from you to extend in every direction")
          .defaultTo(8)
          .min(0)
          .max(1024)
          .build();

  private final Setting<Color> color_water_source =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-wsource")
      .description("Color for water source blocks")
      .defaultTo(Color.of(0, 0, 170, 170))
      .build();
  private final Setting<Color> color_water_stream =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-wstream")
      .description("Color for water stream blocks")
      .defaultTo(Color.of(85, 85, 255, 125))
      .build();
  private final Setting<Color> color_lava_source =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-lsource")
      .description("Color for lava source blocks")
      .defaultTo(Color.of(170, 0, 0, 170))
      .build();
  private final Setting<Color> color_lava_stream =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-lstream")
      .description("Color for lava stream blocks")
      .defaultTo(Color.of(255, 0, 0, 125))
      .build();

  public LiquidESP() {
    super(Category.RENDER, "LiquidESP", false, "Highlight liquids");
  }

  private Queue<BlockPos> water = new ConcurrentLinkedQueue<BlockPos>();
  private Queue<BlockPos> water_s = new ConcurrentLinkedQueue<BlockPos>();
  private Queue<BlockPos> lava = new ConcurrentLinkedQueue<BlockPos>();
  private Queue<BlockPos> lava_s = new ConcurrentLinkedQueue<BlockPos>();

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (getWorld() == null || getLocalPlayer() == null) return;

    water.clear();
    lava.clear();
    water_s.clear();
    lava_s.clear();

    int maxX = (int) Math.round(getLocalPlayer().posX + (radius.get()/2));
    int maxY = (int) Math.round(getLocalPlayer().posY + (radius.get()/2));
    int maxZ = (int) Math.round(getLocalPlayer().posZ + (radius.get()/2));
    for (int x = (int) Math.round(getLocalPlayer().posX - (radius.get()/2)); x < maxX; x++) {
      for (int y = (int) Math.round(getLocalPlayer().posY - (radius.get()/2)); y < maxY; y++) {
        for (int z = (int) Math.round(getLocalPlayer().posZ - (radius.get()/2)); z < maxZ; z++) {
          BlockPos pos = new BlockPos(x, y, z);
          IBlockState state = getWorld().getBlockState(pos);
          if (state.getBlock() instanceof BlockLiquid) {
            Integer l = (Integer) state.getValue(BlockLiquid.LEVEL);
            if (state.getBlock().equals(Blocks.LAVA)) {
              if (l.intValue() == 0)
                lava_s.add(pos);
              else
                lava.add(pos);
            } else if (state.getBlock().equals(Blocks.WATER)) {
              if (l.intValue() == 0)
                water_s.add(pos);
              else
                water.add(pos);
            }
          }

        }
      }
    }
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
    
    Color c;
    int color;
    int linecolor;

    if (!sources_only.get()) {
      c = color_water_stream.get();
      linecolor = Color.of(c.getRed(), c.getGreen(), c.getBlue(), 255).toBuffer();
      color = c.toBuffer();
      for (BlockPos pos : water) {
        if (outline.get()) {
          GlStateManager.glLineWidth(width.get());
          event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Line.ALL, linecolor);
          event.getTessellator().draw();
        }
        if (fill.get()) {
          event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Quad.ALL, color);
          event.getTessellator().draw();
        }
      }

      c = color_lava_stream.get();
      linecolor = Color.of(c.getRed(), c.getGreen(), c.getBlue(), 255).toBuffer();
      color = c.toBuffer();
      for (BlockPos pos : lava) {
        if (outline.get()) {
          GlStateManager.glLineWidth(width.get());
          event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Line.ALL, linecolor);
          event.getTessellator().draw();
        }
        if (fill.get()) {
          event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Quad.ALL, color);
          event.getTessellator().draw();
        }
      }
    }

    c = color_water_source.get();
    linecolor = Color.of(c.getRed(), c.getGreen(), c.getBlue(), 255).toBuffer();
    color = c.toBuffer();
    for (BlockPos pos : water_s) {
      if (outline.get()) {
        GlStateManager.glLineWidth(width.get());
        event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Line.ALL, linecolor);
        event.getTessellator().draw();
      }
      if (fill.get()) {
        event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Quad.ALL, color);
        event.getTessellator().draw();
      }
    }

    c = color_lava_source.get();
    linecolor = Color.of(c.getRed(), c.getGreen(), c.getBlue(), 255).toBuffer();
    color = c.toBuffer();
    for (BlockPos pos : lava_s) {
      if (outline.get()) {
        GlStateManager.glLineWidth(width.get());
        event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Line.ALL, linecolor);
        event.getTessellator().draw();
      }
      if (fill.get()) {
        event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Quad.ALL, color);
        event.getTessellator().draw();
      }
    }


    if (antialias.get()) {
      GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }
    GlStateManager.glLineWidth(1.0f);
  }
}