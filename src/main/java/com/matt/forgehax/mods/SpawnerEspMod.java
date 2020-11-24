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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Created on 9/29/2016 by fr1kin
 */
@RegisterMod
public class SpawnerEspMod extends ToggleMod {

  public SpawnerEspMod() {
    super(Category.RENDER, "SpawnerESP", false, "Shows spawners");
  }

  private final Setting<Color> color =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color")
      .description("Color for highlighted blocks")
      .defaultTo(Color.of(255, 85, 85, 64))
      .build();

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

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onRender(RenderEvent event) {
    if(MC.gameSettings.hideGUI || getWorld() == null) {
      return;
    }

    if (antialias.get()) {
      GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }

    for (TileEntity tileEntity : getWorld().loadedTileEntityList) {
      if (tileEntity instanceof TileEntityMobSpawner) {
        BlockPos pos = tileEntity.getPos();

        if (outline.get()) {
          GlStateManager.glLineWidth(width.get());
          event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(
            event.getBuffer(), pos, GeometryMasks.Line.ALL,
            Color.of(color.get().toBuffer()).setAlpha(255).toBuffer());
            
          event.getTessellator().draw();
        }

        if (fill.get()) {
          event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(
            event.getBuffer(), pos, GeometryMasks.Quad.ALL,
            color.get().toBuffer());

          event.getTessellator().draw();
        }
      }
    }

    GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    GL11.glDisable(GL11.GL_LINE_SMOOTH);
    GlStateManager.glLineWidth(1.0f);
  }
}
