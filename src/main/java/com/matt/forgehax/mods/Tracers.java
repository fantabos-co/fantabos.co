package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.mods.managers.FriendManager;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.mobtypes.MobTypeEnum;
import com.matt.forgehax.util.math.AngleHelper;
import com.matt.forgehax.util.math.Plane;
import com.matt.forgehax.util.math.VectorUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Created on 8/6/2017 by fr1kin
 */
@RegisterMod
public class Tracers extends ToggleMod implements Colors {

  enum Mode {
    ARROWS,
    LINES,
    BOTH
  }

  public final Setting<Mode> mode =
      getCommandStub()
          .builders()
          .<Mode>newSettingEnumBuilder()
          .name("mode")
          .description("Tracer drawing mode.")
          .defaultTo(Mode.ARROWS)
          .build();

  enum ColorMode {
    STATIC,
    DYNAMIC
  }

  private final Setting<Color> color_player =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-player")
      .description("Color for players")
      .defaultTo(Color.of(191, 97, 106, 255))
      .build();
  private final Setting<Color> color_ally =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-ally")
      .description("Color for friends")
      .defaultTo(Color.of(0, 170, 170))
      .build();
  private final Setting<Color> color_hostile =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-hostile")
      .description("Color for hostile entities")
      .defaultTo(Color.of(255, 85, 85, 255))
      .build();
  private final Setting<Color> color_neutral =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-neutral")
      .description("Color for neutral entities")
      .defaultTo(Color.of(85, 85, 255, 255))
      .build();
  private final Setting<Color> color_friendly =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-friendly")
      .description("Color for friendly entities")
      .defaultTo(Color.of(0, 255, 0, 255))
      .build();
  private final Setting<Color> color_markers =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color-markers")
      .description("Color for water source blocks")
      .defaultTo(Color.of(128, 128, 128, 75))
      .build();

  public final Setting<ColorMode> colorMode =
      getCommandStub()
          .builders()
          .<ColorMode>newSettingEnumBuilder()
          .name("color-mode")
          .description("Color mode for tracers.")
          .defaultTo(ColorMode.DYNAMIC)
          .build();

  public final Setting<Integer> alpha =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("alpha")
          .description("Alpha value of the tracer color.")
          .defaultTo(191)
          .min(0)
          .max(255)
          .build();

  public final Setting<Boolean> antialias =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("antialias")
          .description("Antialiasing for the tracer, may hurt performance.")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> friend_highlight =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("friends")
          .description("Color friends differently")
          .defaultTo(true)
          .build();

  private final Setting<Float> width =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("width")
          .description("The width value for the tracers.")
          .min(0f)
          .max(10f)
          .defaultTo(1.5f)
          .build();

  private final Setting<Float> depth =
    getCommandStub()
        .builders()
        .<Float>newSettingBuilder()
        .name("depth")
        .description("The GUI depth at which to draw tracers")
        .min(-100f)
        .max(100f)
        .defaultTo(-3f)
        .build();

  public final Setting<Boolean> players =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("players")
          .description("Trace players.")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> hostile =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("hostile")
          .description("Trace hostile mobs.")
          .defaultTo(false)
          .build();

  public final Setting<Boolean> neutral =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("neutral")
          .description("Trace neutral mobs.")
          .defaultTo(false)
          .build();

  public final Setting<Boolean> friendly =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("friendly")
          .description("Trace friendly mobs.")
          .defaultTo(false)
          .build();
    
  private final Setting<Boolean> friend_filter =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("no-friends")
          .description("Do not display tracers for friends")
          .defaultTo(false)
          .build();

  private final Setting<Boolean> show_extra =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("extra")
          .description("Display extra tracers provided from markers")
          .defaultTo(false)
          .build();

  private final Setting<Long> timeout =
      getCommandStub()
          .builders()
          .<Long>newSettingBuilder()
          .name("timeout")
          .description("Time in ms after which extra tracers are cleared, set 0 to disable")
          .min(0L)
          .max(100000L)
          .defaultTo(0L)
          .build();

  private final Setting<Integer> distance =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("distance")
          .description("Delete extra tracers when more distant than this, set to 0 to disable")
          .min(0)
          .max(1024)
          .defaultTo(64)
          .build();

  public Tracers() {
    super(Category.RENDER, "Tracers", false, "See where other entities are.");
    Tracers.INSTANCE = this;
  }

  private static Tracers INSTANCE;

  public static void addExtraTracer(Vec3d in) {
    INSTANCE.extra_tracers.put(in, System.currentTimeMillis());
  }

  private Map<Vec3d, Long> extra_tracers = new ConcurrentHashMap<Vec3d, Long>();

  @SubscribeEvent
  public void onDrawScreen(Render2DEvent event) {
    if (MC.world == null) {
      return;
    }

    GlStateManager.enableBlend();
    GlStateManager.tryBlendFuncSeparate(
        GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
        GlStateManager.SourceFactor.ONE,
        GlStateManager.DestFactor.ZERO);
    GlStateManager.disableTexture2D();

    if (antialias.get()) {
      GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }

    final Mode dm = mode.get();

    final float width = this.width.get();

    final double screenW = event.getScreenWidth();
    final double screenH = event.getScreenHeight();

    MC.world
        .loadedEntityList
        .stream()
        .filter(entity -> !Objects.equals(entity, MC.getRenderViewEntity()))
        .filter(entity -> entity instanceof EntityLivingBase)
        .map(EntityRelations::new)
        .filter(er -> !er.getRelationship().equals(MobTypeEnum.INVALID))
        .filter(EntityRelations::isOptionEnabled)
        .forEach(
            er -> {
              Entity entity = er.getEntity();
              MobTypeEnum relationship = er.getRelationship();

              Vec3d entityPos =
                  EntityUtils.getInterpolatedEyePos(entity, MC.getRenderPartialTicks());

              Color color;

              if (colorMode.get() == ColorMode.STATIC) {
                color = er.getColor().setAlpha(alpha.get());
              } else {
                double entityPosX = entityPos.x;
                double entityPosY = entityPos.y;
                double entityPosZ = entityPos.z;

                color = er.getDynamicColor(entityPosX, entityPosY, entityPosZ).setAlpha(alpha.get());
              }

              int size = (entity instanceof EntityPlayer ? 8 : 5);

              drawTracer(entityPos, color, dm, size, depth.get() + er.getDepth(), width, screenW, screenH);
            });

    if (show_extra.get()) {
      final Color color = color_markers.get();
      final long time_cap = (timeout.get() == 0 ? 0 : System.currentTimeMillis() - timeout.get());
      final int distance_cap = distance.get();
      for (Vec3d vec : extra_tracers.keySet()) {
        drawTracer(vec, color, dm, 2, depth.get() - 4f, width, screenW, screenH);
        if (extra_tracers.get(vec) < time_cap) {
          extra_tracers.remove(vec);
        }
        if (distance_cap > 0 && getLocalPlayer().getDistance(vec.x, vec.y, vec.z) > distance_cap) {
          extra_tracers.remove(vec);
        }
      }
    }

    GlStateManager.enableTexture2D();
    GlStateManager.disableBlend();
    GlStateManager.glLineWidth(1.0f);
    GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    GL11.glDisable(GL11.GL_LINE_SMOOTH);

    GlStateManager.color(1.f, 1.f, 1.f, 1.f);
  }

  private static void drawTracer(Vec3d target, Color color, Mode dm, int size,
                      float depth, float width, double screenWidth, double screenHeight) {
    Plane screenPos = VectorUtils.toScreen(target);

    final double cx = screenWidth / 2.f;
    final double cy = screenHeight / 2.f;

    GlStateManager.color(
        color.getRedAsFloat(),
        color.getGreenAsFloat(),
        color.getBlueAsFloat(),
        color.getAlphaAsFloat());

    GlStateManager.translate(0, 0, depth);

    if (dm.equals(Mode.BOTH) || dm.equals(Mode.ARROWS)) {
      if (!screenPos.isVisible()) {
        // get position on ellipse

        // dimensions of the ellipse
        final double dx = cx - 2;
        final double dy = cy - 20;

        // ellipse = x^2/a^2 + y^2/b^2 = 1
        // e = (pos - C) / d
        //  C = center vector
        //  d = dimensions
        double ex = (screenPos.getX() - cx) / dx;
        double ey = (screenPos.getY() - cy) / dy;

        // normalize
        // n = u/|u|
        double m = Math.abs(Math.sqrt(ex * ex + ey * ey));
        double nx = ex / m;
        double ny = ey / m;

        // scale
        // p = C + dot(n,d)
        double x = cx + nx * dx;
        double y = cy + ny * dy;

        // --------------------
        // now rotate triangle

        // point - center
        // w = <px - cx, py - cy>
        double wx = x - cx;
        double wy = y - cy;

        // u = <w, 0>
        double ux = screenWidth;
        double uy = 0.D;

        // |u|
        double mu = Math.sqrt(ux * ux + uy * uy);
        // |w|
        double mw = Math.sqrt(wx * wx + wy * wy);

        // theta = dot(u,w)/(|u|*|w|)
        double ang = Math.toDegrees(Math.acos((ux * wx + uy * wy) / (mu * mw)));

        // don't allow NaN angles
        if (ang == Float.NaN) {
          ang = 0;
        }

        // invert
        if (y < cy) {
          ang *= -1;
        }

        // normalize
        ang = (float) AngleHelper.normalizeInDegrees(ang);

        // --------------------

        // int size = relationship.equals(MobTypeEnum.PLAYER) ? 8 : 5;

        GlStateManager.pushMatrix();

        GlStateManager.translate(x, y, 0);
        GlStateManager.rotate((float) ang, 0.f, 0.f, size / 2.f);

        GlStateManager.color(
            color.getRedAsFloat(),
            color.getGreenAsFloat(),
            color.getBlueAsFloat(),
            color.getAlphaAsFloat());

        GlStateManager.glBegin(GL11.GL_TRIANGLES);
        {
          GL11.glVertex2d(0, 0);
          GL11.glVertex2d(-size, -size);
          GL11.glVertex2d(-size, size);
        }
        GlStateManager.glEnd();

        GlStateManager.popMatrix();
      }
    }

    if (dm.equals(Mode.BOTH) || dm.equals(Mode.LINES)) {
      GlStateManager.glLineWidth(width);
      GlStateManager.glBegin(GL11.GL_LINES);
      {
        GL11.glVertex2d(cx, cy);
        GL11.glVertex2d(screenPos.getX(), screenPos.getY());
      }
      GlStateManager.glEnd();
    }

    GlStateManager.translate(0, 0, -depth);
  }

  private class EntityRelations implements Comparable<EntityRelations> {

    private final Entity entity;
    private final MobTypeEnum relationship;
    private boolean isfriend = false;

    public EntityRelations(Entity entity) {
      Objects.requireNonNull(entity);
      this.entity = entity;
      this.relationship = EntityUtils.getRelationship(entity);
      if (entity instanceof EntityPlayer) {
        if (FriendManager.isFriendly(entity.getName()))
          isfriend = true;
      }
    }

    public Entity getEntity() {
      return entity;
    }

    public MobTypeEnum getRelationship() {
      return relationship;
    }

    public Color getColor() {
      switch (relationship) {
        case PLAYER:
          if (this.isfriend) return color_ally.get();
          return color_player.get();
        case HOSTILE:
          return color_hostile.get();
        case NEUTRAL:
          return color_neutral.get();
        default:
          return color_friendly.get();
      }
    }

    public Color getDynamicColor(double entityX, double entityY, double entityZ) {
      Color tracerColor;
      Entity player = MC.player;
      double playerX = player.posX;
      double playerY = player.posY;
      double playerZ = player.posZ;

      double entityDistance = VectorUtils.distance(playerX, playerY, playerZ, entityX, entityY, entityZ);

      if (entityDistance < 50) {
        int green = (int) ((entityDistance / 50) * 255);
        int red = 255 - green;
        tracerColor = Color.of(red, green, 0, alpha.get());
      } else {
        tracerColor = GREEN;
      }

      return tracerColor;
    }

    public float getDepth() {
      switch (relationship) {
        case PLAYER:
          return 3.f;
        case HOSTILE:
          return 2.f;
        case NEUTRAL:
          return 1.f;
        default:
          return 0.f;
      }
    }

    public boolean isOptionEnabled() {
      switch (relationship) {
        case PLAYER:
          if (this.isfriend && friend_filter.get())
            return false;
          return players.get();
        case HOSTILE:
          return hostile.get();
        case NEUTRAL:
          return neutral.get();
        default:
          return friendly.get();
      }
    }

    @Override
    public int compareTo(EntityRelations o) {
      return getRelationship().compareTo(o.getRelationship());
    }
  }
}
