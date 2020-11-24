package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.asm.events.PacketEvent;
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
import net.minecraft.network.play.server.SPacketBlockBreakAnim;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

@RegisterMod
public class BreakingESP extends ToggleMod {

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
          .min(0.5f)
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

  public final Setting<Long> timeout =
      getCommandStub()
          .builders()
          .<Long>newSettingBuilder()
          .name("timeout")
          .description("Milliseconds after which to clear")
          .defaultTo(1000L)
          .min(1L)
          .max(10000L)
          .build();

  private final Setting<Color> color =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color")
      .description("Color for other directions")
      .defaultTo(Color.of(150, 150, 150, 200))
      .build();
  public final Setting<Boolean> progressive_alpha =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("progressive-alpha")
          .description("Makes alpha reltive to progress")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> server_stop =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("server-stop")
          .description("Accept server stop-breaking-animation and remove esp")
          .defaultTo(true)
          .build();

  public BreakingESP() {
    super(Category.RENDER, "BreakingESP", false, "Highlights blocks being broken");
  }

  private ConcurrentHashMap<BlockPos, Long> blocks_mined = new ConcurrentHashMap<BlockPos, Long>();
  private ConcurrentHashMap<BlockPos, Float> blocks_progress = new ConcurrentHashMap<BlockPos, Float>();

  @Override
  public String getDisplayText() {
    return (getModName() + " [" + TextFormatting.LIGHT_PURPLE + blocks_mined.size() + TextFormatting.RESET + "]");
  }

  @SubscribeEvent
  public void onPacketIncoming(PacketEvent.Incoming.Pre event) {
    if (event.getPacket() instanceof SPacketBlockBreakAnim) {
      SPacketBlockBreakAnim packet = event.getPacket();
      if (packet.getProgress() >= 0 && packet.getProgress() <= 9) {
        blocks_mined.put(packet.getPosition(), System.currentTimeMillis());
        blocks_progress.put(packet.getPosition(), (packet.getProgress() + 1) / 10f);
      } else if (server_stop.get()) {
        // Any other progress means that the animation should be reset
        blocks_mined.remove(packet.getPosition());
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

    long now = Instant.now().toEpochMilli();
    int alpha_p;
    int clr;
    Color c = color.get();

    for (BlockPos pos : blocks_mined.keySet()) {
      alpha_p = (progressive_alpha.get() ? (int) (c.getAlpha() * blocks_progress.get(pos)) : c.getAlpha());
      clr = Color.of(c.getRed(), c.getGreen(), c.getBlue(), alpha_p).toBuffer();
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

      if (blocks_mined.get(pos) != null && now > (blocks_mined.get(pos) + timeout.get())) {
        blocks_mined.remove(pos);
      }
    }

    if (antialias.get()) { 
      GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
      GlStateManager.glLineWidth(1.0f);
    }
  }
}
