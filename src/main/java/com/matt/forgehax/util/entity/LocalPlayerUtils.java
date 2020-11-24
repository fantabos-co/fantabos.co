package com.matt.forgehax.util.entity;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getPlayerController;
import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getNetworkManager;

import com.google.common.base.Predicates;
import com.matt.forgehax.Globals;
import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.mods.services.SneakService;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.Switch;
import com.matt.forgehax.util.math.Angle;
import com.matt.forgehax.util.math.AngleHelper;

import java.util.Optional;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;

/**
 * Class for dealing with the local player only
 */
public class LocalPlayerUtils implements Globals {
  
  /**
   * Gets the players current view angles
   */
  public static Angle getViewAngles() {
    return PositionRotationManager.getState().getRenderClientViewAngles();
  }
  
  public static Angle getServerViewAngles() {
    return PositionRotationManager.getState().getRenderServerViewAngles();
  }
  
  public static Vec3d getVelocity() {
    return new Vec3d(getLocalPlayer().motionX, getLocalPlayer().motionY, getLocalPlayer().motionZ);
  }
  
  public static boolean isSneaking() {
    return getLocalPlayer().isSneaking();
  }
  
  public static boolean setSneaking(boolean sneak) {
    boolean old = isSneaking();
    getLocalPlayer().setSneaking(sneak);
    if (getLocalPlayer().movementInput != null) {
      getLocalPlayer().movementInput.sneak = sneak;
    }
    return old;
  }
  
  public static void setSneakingSuppression(boolean suppress) {
    SneakService.getInstance().setSuppressing(suppress);
  }
  
  public static Vec3d getEyePos() {
    return EntityUtils.getEyePos(getLocalPlayer());
  }
  
  public static Vec3d getDirectionVector() {
    return getViewAngles().getDirectionVector().normalize();
  }
  
  public static Vec3d getServerDirectionVector() {
    return getServerViewAngles().getDirectionVector().normalize();
  }
  
  public static RayTraceResult getViewTrace() {
    return MC.objectMouseOver;
  }
  
  public static RayTraceResult getMouseOverBlockTrace() {
    return Optional.ofNullable(MC.objectMouseOver)
        .filter(tr -> tr.getBlockPos() != null) // no its not intelliJ
        .filter(
            tr ->
                Type.BLOCK.equals(tr.typeOfHit)
                    || !Material.AIR.equals(
                    getWorld().getBlockState(tr.getBlockPos()).getMaterial()))
        .orElse(null);
  }
  
  public static RayTraceResult getViewTrace(
      Entity entity, Vec3d direction, float partialTicks, double reach, double reachAttack) {
    if (entity == null) {
      return null;
    }
    
    Vec3d eyes = entity.getPositionEyes(partialTicks);
    RayTraceResult trace = entity.rayTrace(reach, partialTicks);
    
    Vec3d dir = direction.scale(reach);
    Vec3d lookDir = eyes.add(dir);
    
    double hitDistance = trace == null ? reachAttack : trace.hitVec.distanceTo(eyes);
    Entity hitEntity = null;
    Vec3d hitEntityVec = null;
    
    for (Entity ent :
        getWorld()
            .getEntitiesInAABBexcluding(
                entity,
                entity.getEntityBoundingBox().expand(dir.x, dir.y, dir.y).grow(1.D),
                Predicates.and(
                    EntitySelectors.NOT_SPECTATING,
                    ent -> ent != null && ent.canBeCollidedWith()))) {
      AxisAlignedBB bb = ent.getEntityBoundingBox().grow(ent.getCollisionBorderSize());
      RayTraceResult tr = bb.calculateIntercept(eyes, lookDir);
      if (bb.contains(eyes)) {
        if (hitDistance > 0.D) {
          hitEntity = ent;
          hitEntityVec = tr == null ? eyes : tr.hitVec;
          hitDistance = 0.D;
        }
      } else if (tr != null) {
        double dist = eyes.distanceTo(tr.hitVec);
        if (dist < hitDistance || hitDistance == 0.D) {
          if (entity.getLowestRidingEntity() == ent.getLowestRidingEntity()
              && !ent.canRiderInteract()) {
            if (hitDistance == 0.D) {
              hitEntity = ent;
              hitEntityVec = tr.hitVec;
            }
          } else {
            hitEntity = ent;
            hitEntityVec = tr.hitVec;
            hitDistance = dist;
          }
        }
      }
    }
    
    if (hitEntity != null && reach > 3.D && eyes.distanceTo(hitEntityVec) > 3.D) {
      return new RayTraceResult(Type.MISS, hitEntityVec, EnumFacing.UP, new BlockPos(hitEntityVec));
    } else if (hitEntity != null && trace == null && hitDistance < reachAttack) {
      return new RayTraceResult(hitEntity, hitEntityVec);
    } else {
      return trace;
    }
  }
  
  public static boolean isInReach(Vec3d start, Vec3d end) {
    return start.squareDistanceTo(end)
        < getPlayerController().getBlockReachDistance()
        * getPlayerController().getBlockReachDistance();
  }
  
  private static final Switch FLY_SWITCH = new Switch("PlayerFlying") {
    @Override
    protected void onEnabled() {
      MC.addScheduledTask(() -> {
        if (getLocalPlayer() == null || getLocalPlayer().capabilities == null) {
          return;
        }
        
        getLocalPlayer().capabilities.allowFlying = true;
        getLocalPlayer().capabilities.isFlying = true;
      });
    }
    
    @Override
    protected void onDisabled() {
      MC.addScheduledTask(() -> {
        EntityPlayer player = getLocalPlayer();
        
        if (player == null || player.capabilities == null) {
          return;
        }
        
        PlayerCapabilities gmCaps = new PlayerCapabilities();
        MC.playerController.getCurrentGameType().configurePlayerCapabilities(gmCaps);
        
        PlayerCapabilities capabilities = player.capabilities;
        capabilities.allowFlying = gmCaps.allowFlying;
        capabilities.isFlying = gmCaps.allowFlying && capabilities.isFlying;
        capabilities.setFlySpeed(gmCaps.getFlySpeed());
      });
    }
  };
  
  public static Switch getFlySwitch() {
    return FLY_SWITCH;
  }


  /**
   * This comes from IronException's Freecam, all props to him for idea and implementation
   */

  public static void lookAt(final Entity entity, final Vec3d goal) {
    lookAt(entity, goal, false);
  }

  public static void lookAt(final Entity entity, final Vec3d goal, final boolean packet) {
      lookAt(entity, goal, true, true, packet);
  }

  public static void lookAt(final Entity entity, final Vec3d goal, final boolean yawIn, final boolean pitchIn, final boolean packet) {
      final Vec3d position = entity.getPositionEyes(1f);
      float yaw = entity.rotationYaw;
      float pitch = entity.rotationPitch;
      if (yawIn) {
          yaw = calculateYaw(position, goal);
      }
      if (pitchIn) {
          pitch = calculatePitch(position, goal);
      }
      lookAt(entity, yaw, pitch, packet);
  }

  public static void lookAt(final Entity entity, final float yaw, final float pitch, final boolean packet) {
      // TODO maybe a setting or whatever works best to set this?
      entity.prevRotationYaw = entity.rotationYaw;
      entity.prevRotationPitch = entity.rotationPitch;
      entity.rotationYaw = yaw;
      entity.rotationPitch = pitch;
      entity.setRotationYawHead(entity.rotationYaw); // so the head also looks that way
      if (packet) {
        PacketHelper.ignoreAndSend(new CPacketPlayer.Rotation(yaw, pitch, true));
      }
  }

  public static float calculateYaw(final Vec3d from, final Vec3d goal) {
      return AngleHelper.normalizeInDegrees(
              AngleHelper.getAngleFacingInDegrees(
                      goal.subtract(from)).getYaw());
  }

  public static float calculatePitch(final Vec3d from, final Vec3d goal) {
      return AngleHelper.normalizeInDegrees(
              AngleHelper.getAngleFacingInDegrees(
                      goal.subtract(from)).getPitch());
  }
}
