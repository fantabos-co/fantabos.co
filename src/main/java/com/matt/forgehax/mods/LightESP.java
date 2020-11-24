package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getLocalPlayer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryMasks;
import com.matt.forgehax.util.tesselation.GeometryTessellator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.lwjgl.opengl.GL11;

/**
 * Created on 9/4/2016 by fr1kin
 * Updated by OverFloyd, may 2020
 * Tonio made this for brightness
 */
@RegisterMod
public class LightESP extends ToggleMod {

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

  private final Setting<Color> color =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color")
      .description("Color for highlighted positions")
      .defaultTo(Color.of(0, 0, 0, 80))
      .build();

  private final Setting<Float> width =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("width")
          .description("The width value for the outline")
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

  public final Setting<Integer> ceiling =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("ceiling")
          .description("Check only blocks below this Y")
          .defaultTo(128)
          .min(0)
          .max(256)
          .build();

  public final Setting<Integer> limit_level =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("threshold")
          .description("Highlight all positions with a light level below this one")
          .defaultTo(10)
          .min(0)
          .max(20)
          .build();

  public final Setting<Boolean> negate =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("negate")
          .description("Show all places with more than threshold light instead")
          .defaultTo(false)
          .build();

  public final Setting<Boolean> ground_only =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("only-ground")
          .description("Show only dark places touching the ground")
          .defaultTo(false)
          .build();


  public LightESP() {
    super(Category.RENDER, "LightESP", false, "Highlight places depending on light level");
  }

  private Queue<BlockPos> positions = new ConcurrentLinkedQueue<BlockPos>();

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (getWorld() == null || getLocalPlayer() == null) return;

    positions.clear();
    int maxX = (int) Math.round(getLocalPlayer().posX + (radius.get()/2));
    int maxZ = (int) Math.round(getLocalPlayer().posZ + (radius.get()/2));
    for (int x = (int) Math.round(getLocalPlayer().posX - (radius.get()/2)); x < maxX; x++) {
      for (int y = 0; y < ceiling.get(); y++) {
        for (int z = (int) Math.round(getLocalPlayer().posZ - (radius.get()/2)); z < maxZ; z++) {
          BlockPos pos = new BlockPos(x, y, z);
          if (getWorld().getBlockState(pos).getBlock().equals(Blocks.AIR) &&
              (getWorld().getLight(pos) < limit_level.get() ^ negate.get()) &&
              (!ground_only.get() || !getWorld().getBlockState(pos.offset(EnumFacing.DOWN))
                                                    .getBlock().equals(Blocks.AIR))) {
            positions.add(pos);
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
    
    int clr = color.get().toBuffer();

    for (BlockPos pos : positions) {

      if (outline.get()) {
        GlStateManager.glLineWidth(width.get());
        event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Line.ALL, clr);
        event.getTessellator().draw();
      }

      if (fill.get()) {
        event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Quad.ALL, clr);
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

