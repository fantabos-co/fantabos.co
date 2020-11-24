package com.matt.forgehax.mods.services;

import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getModManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.matt.forgehax.Helper;
import com.matt.forgehax.events.EntityRemovedEvent;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.mods.Aimbot;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.RenderUtils;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryMasks;
import com.matt.forgehax.util.tesselation.GeometryTessellator;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


@RegisterMod
public class AIESP extends ServiceMod {

  private final Setting<Color> color_place =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-place")
      .description("Color for placed crystals")
      .defaultTo(Color.of(0, 170, 170, 100))
      .build();
  
  private final Setting<Float> width_place =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("p-width")
      .description("line width for placing")
      .min(0.f)
      .max(10f)
      .defaultTo(1.f)
      .build();

  private final Setting<Color> color_detonate =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-detonate")
      .description("Color for detonated crystals")
      .defaultTo(Color.of(191, 97, 106, 180))
      .build();
  
  private final Setting<Float> width_detonate =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("d-width")
      .description("line width for detonating")
      .min(0.f)
      .max(10f)
      .defaultTo(3.f)
      .build();

  private final Setting<Color> color_entity =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-entity")
      .description("Color for detonated crystals")
      .defaultTo(Color.of(255, 170, 0, 100))
      .build();
  
  private final Setting<Float> width_entity =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("e-width")
      .description("line width for entity")
      .min(0.f)
      .max(10f)
      .defaultTo(1.f)
      .build();
  private final Setting<Float> offset_entity =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("e-offset")
      .description("hitbox offset for entity")
      .min(-5.f)
      .max(5f)
      .defaultTo(0.f)
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
      .min(0L)
      .max(100000L)
      .build();

  public final Setting<Boolean> track_attempts =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("attempts")
      .description("Track attempts of blowing one crystal")
      .defaultTo(false)
      .build();
  
  public AIESP() {
    super("AIESP", "Render visual aids for CrystalAura and KillAura targeting");
  }

  private static Map<BlockPos, Long> crystal_place = new ConcurrentHashMap<BlockPos, Long>();
  private static Map<BlockPos, Long> crystal_detonate = new ConcurrentHashMap<BlockPos, Long>();
  private static Map<Vec3d, Integer> crystal_detonation_attempts = new ConcurrentHashMap<Vec3d, Integer>();

  public static void addCrystalPlacement(BlockPos pos, long time) {
    crystal_place.put(pos, time);
  }

  public static void addCrystalDetonation(EntityEnderCrystal ec, long time) {
    crystal_place.remove(ec.getPosition());
    crystal_detonate.put(ec.getPosition().offset(EnumFacing.DOWN), time);
    if (getModManager().get(AIESP.class).get().track_attempts.get()) { // EWWWWWWWW
      Integer att = crystal_detonation_attempts.get(ec.getPositionVector());
      if (att == null) {
        crystal_detonation_attempts.put(ec.getPositionVector(), 1);
      } else {
        crystal_detonation_attempts.put(ec.getPositionVector(), att + 1);
        Helper.printWarning("Attempted %d times to blow a crystal", att + 1);
      }
    }
  }

  @SubscribeEvent
  public void onExplosion(EntityRemovedEvent event) {
    if (event.getEntity() instanceof EntityEnderCrystal) {
      if (crystal_detonation_attempts.remove(((EntityEnderCrystal) event.getEntity()).getPositionVector()) != null && track_attempts.get()) {
        Helper.printInform("Succesfully detonated a crystal");
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onRenderLOW(final RenderEvent event) {
    if(MC.gameSettings.hideGUI || getWorld() == null) {
      return;
    }
    float off = offset_entity.get();
    int color = color_entity.get().toBuffer();
    Entity targeting = Aimbot.getTarget();
    if (targeting != null) {
      AxisAlignedBB bb = targeting.getEntityBoundingBox();
      Vec3d minVec = new Vec3d(bb.minX - off, bb.minY - off, bb.minZ - off);
      Vec3d maxVec = new Vec3d(bb.maxX + off, bb.maxY + off, bb.maxZ + off);
      RenderUtils.drawBox(minVec, maxVec, color, width_entity.get(), true);
    }
  }

  // These 2 need to be separate because drawBox interferes with the tessellator drawing

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onRenderHIGH(final RenderEvent event) {
    if(MC.gameSettings.hideGUI || getWorld() == null) {
      return;
    }

    long now = System.currentTimeMillis();
    int color;
    
    if (antialias.get()) {
      GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }
    
    color = color_place.get().toBuffer();
    for (BlockPos pos : crystal_place.keySet()) {
      GlStateManager.glLineWidth(width_place.get());
      event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
      GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Line.ALL, color);
      event.getTessellator().draw();
      event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Quad.ALL, color);
      event.getTessellator().draw();
      if (now > (crystal_place.get(pos) + timeout.get())) {
        crystal_place.remove(pos);
      }
    }

    color = color_detonate.get().toBuffer();
    for (BlockPos pos : crystal_detonate.keySet()) {
      GlStateManager.glLineWidth(width_detonate.get());
      event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
      GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Line.ALL, color);
      event.getTessellator().draw();
      event.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Quad.ALL, color);
      event.getTessellator().draw();
      if (now > (crystal_detonate.get(pos) + timeout.get())) {
        crystal_detonate.remove(pos);
      }
    }

    if (antialias.get()) { 
      GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
      GlStateManager.glLineWidth(1.0f);
    }
  }
}
