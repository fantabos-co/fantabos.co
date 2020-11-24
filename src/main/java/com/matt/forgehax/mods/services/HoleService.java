package com.matt.forgehax.mods.services;

import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getLocalPlayer;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;


@RegisterMod
public class HoleService extends ServiceMod {

  public final Setting<Boolean> enabled =
  getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("enabled")
      .description("Actively look for holes around player")
      .defaultTo(true)
      .build();

  public final Setting<Double> radius =
  getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("radius")
      .description("How far to look for holes")
      .min(0D)
      .max(1000D)
      .defaultTo(20D)
      .build();

  public final Setting<Boolean> save =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("save")
      .description("Prevent player from falling in void holes")
      .defaultTo(true)
      .build();

  public final Setting<Double> save_threshold =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("threshold")
      .description("How much before Y 0 to trigger void save")
      .min(0D)
      .max(5D)
      .defaultTo(0.2D)
      .build();
  
  public HoleService() {
    super("HoleService", "Identify and classify holes around player, needed for other mods");
  }

  public enum HoleQuality {
    NOTHOLE,
    TEMPORARY,
    SAFE
  }

  public static Queue<BlockPos> safe_holes = new ConcurrentLinkedQueue<BlockPos>();
  public static Queue<BlockPos> temp_holes = new ConcurrentLinkedQueue<BlockPos>();
  public static Queue<BlockPos> voids = new ConcurrentLinkedQueue<BlockPos>();

  public static List<BlockPos> getAllHoles() {
    return Stream.concat(safe_holes.stream(), temp_holes.stream())
                             .collect(Collectors.toList());
  }

  @SubscribeEvent(priority = EventPriority.LOW)
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null || getWorld() == null) return;
    safe_holes.clear();
    temp_holes.clear();
    voids.clear();

    if (!enabled.get()) return;

    int maxX = (int) Math.round(getLocalPlayer().posX + (radius.get()/2));
    int maxY = (int) Math.round(getLocalPlayer().posY + (radius.get()/2));
    int maxZ = (int) Math.round(getLocalPlayer().posZ + (radius.get()/2));

    for (int x = (int) Math.round(getLocalPlayer().posX - (radius.get()/2)); x < maxX; x++) {
      for (int y = (int) Math.round(getLocalPlayer().posY - (radius.get()/2)); y < maxY; y++) {
        for (int z = (int) Math.round(getLocalPlayer().posZ - (radius.get()/2)); z < maxZ; z++) {
          BlockPos pos = new BlockPos(x, y, z);
          if (isVoid(pos)) {
            voids.add(pos);
          }
          switch (isHole(pos)) {
            case SAFE:
              safe_holes.add(pos);
              break;
            case TEMPORARY:
              temp_holes.add(pos);
              break;
            default: // do nothing
          }

        }
      }
    }
  }

  @SubscribeEvent
  public void onPlayerMovement(LocalPlayerUpdateEvent event) {
    if (getLocalPlayer() == null || !save.get()) return;
    if (getLocalPlayer().isElytraFlying()) return; // Player will get out of this pickle by himself
    for (BlockPos pos : voids) {
      if (isAboveVoid(pos, getLocalPlayer())) {
        getLocalPlayer().setVelocity(0, 0, 0); // server will snap us to last safe spot eventually
        break;
      }
    }
  }

  public static boolean isVoid(BlockPos pos) {
    if (pos.getY() == 0 && getWorld().getBlockState(pos).getBlock().equals(Blocks.AIR))
      return true;
    return false;
  }

  public boolean isAboveVoid(BlockPos hole, Entity entity) {
    if (entity.posY < save_threshold.get()) {
      if (entity.posX > hole.getX() && entity.posX < hole.getX() + 1)
        if (entity.posZ > hole.getZ() && entity.posZ < hole.getZ() + 1)
          return true;
    }
    return false;
  }

  // Returns hole quality, 0 is not hole, 1 is breakable hole, 2 is perfect hole
  public static HoleQuality isHole(BlockPos pos) {
    if (!getWorld().getBlockState(pos).getBlock().equals(Blocks.AIR) ||
        !getWorld().getBlockState(pos.offset(EnumFacing.UP)).getBlock().equals(Blocks.AIR) || 
        !getWorld().getBlockState(pos.offset(EnumFacing.UP).offset(EnumFacing.UP)).getBlock().equals(Blocks.AIR))
            return HoleQuality.NOTHOLE;
    int obi = 0;
    for (EnumFacing off : Offsets.HOLE) {
      if (getWorld().getBlockState(pos.offset(off)).getBlock().equals(Blocks.BEDROCK)) {
        // great, continue
      } else if (getWorld().getBlockState(pos.offset(off)).getBlock().equals(Blocks.OBSIDIAN)) {
        obi++;
      } else {
        return HoleQuality.NOTHOLE;
      }
    }
    if (obi > 0) return HoleQuality.TEMPORARY;
    else return HoleQuality.SAFE;
  }

  public static boolean isAboveHole(BlockPos hole, Entity entity) {
    return isAboveHole(hole, entity, 0.2D);
  }

  public static boolean isAboveHole(BlockPos hole, Entity entity, double distance) {
    return isAboveHole(hole, entity.getPositionVector(), distance);
  }

  public static boolean isAboveHole(BlockPos hole, Vec3d player, double distance) {
    double xp = player.x;
    double zp = player.z;
    double xh = hole.getX() + 0.5;
    double zh = hole.getZ() + 0.5;
    if (Math.abs(xp - xh) < distance && Math.abs(zp - zh) < distance)
      return true;
    return false;
  }

  public static boolean isInHole(Entity e) {
    for (BlockPos h : safe_holes) {
      if (e.getPosition().equals(h))
        return true;
    }
    for (BlockPos h : temp_holes) {
      if (e.getPosition().equals(h))
        return true;
    }
    return false;
  }

  public static class Offsets {
    private static final EnumFacing[] HOLE = {
      EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST
    };
  }
}
