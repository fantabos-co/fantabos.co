package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.projectile.Projectile;
import com.matt.forgehax.util.projectile.SimulationResult;
import com.matt.forgehax.util.tesselation.GeometryMasks;
import com.matt.forgehax.util.tesselation.GeometryTessellator;
import java.util.Iterator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

@RegisterMod
public class TrajectoryMod extends ToggleMod {

  private final Setting<Integer> alpha =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("alpha")
          .description("Transparency, 0-255")
          .min(0)
          .max(255)
          .defaultTo(255)
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
          .description("line width")
          .min(0.f)
          .max(10f)
          .defaultTo(5.f)
          .build();

  private final Setting<Boolean> target_box =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("target-box")
          .description("Draw a box at the end of the trajectory")
          .defaultTo(true)
          .build();

  private final Setting<Integer> skip =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("skip")
          .description("Points to skip in the trajectory drawing")
          .min(0)
          .max(20)
          .defaultTo(1)
          .build();
  
  public TrajectoryMod() {
    super(Category.COMBAT, "Trajectory", false, "Draws projectile trajectory");
  }

  @SubscribeEvent
  public void onRender(RenderEvent event) {
    Projectile projectile =
        Projectile.getProjectileByItemStack(getLocalPlayer().getHeldItemMainhand());
    if (!projectile.isNull()) {
      SimulationResult result =
          projectile.getSimulatedTrajectoryFromEntity(
              getLocalPlayer(),
              PositionRotationManager.getState().getRenderServerViewAngles(),
              projectile.getForce(
                  getLocalPlayer().getHeldItemMainhand().getMaxItemUseDuration()
                      - getLocalPlayer().getItemInUseCount()),
              0);
      if (result == null) {
        return;
      }
      
      if (result.getPathTraveled().size() > 1) {
        event.setTranslation(getLocalPlayer().getPositionVector());
        
        GlStateManager.enableDepth();
        GlStateManager.glLineWidth(width.get());
        
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        
        Iterator<Vec3d> it = result.getPathTraveled().iterator();
        Vec3d previous = it.next();
        int count = 0; // it is incremented before checking
        while (it.hasNext()) {
          Vec3d next = it.next();
          if (count >= skip.get()) {
            event
                .getBuffer()
                .pos(previous.x, previous.y, previous.z)
                .color(red.get(), green.get(), blue.get(), alpha.get())
                .endVertex();
            event.getBuffer().pos(next.x, next.y, next.z).color(red.get(), green.get(), blue.get(), alpha.get()).endVertex();
          }
          previous = next;
          count++;
        }
        
        event.getTessellator().draw();

        event.resetTranslation();

        if (target_box.get()) {
          int color = Color.of(red.get(), green.get(), blue.get(), alpha.get()).toBuffer();
          event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
          GeometryTessellator.drawCuboid(
              event.getBuffer(),
              new BlockPos(result.getHitPos()),
              GeometryMasks.Line.ALL, color);
          event.getTessellator().draw();
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        
        GlStateManager.glLineWidth(1.0f);
        GlStateManager.disableDepth();
      }
    }
  }
}
